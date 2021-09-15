package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockConnectionContext.FailOnPurposeException;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

public class TopicConnectionTest {

    private final MockConnectionContext context = MockConnectionContext
            .createEager();

    private final Set<String> activeTopics = new HashSet<>();

    private final VaadinService service = new MockService();

    private final CollaborationEngine engine = new TestUtil.TestCollaborationEngine(
            (topicId, isActive) -> {
                if (isActive) {
                    activeTopics.add(topicId);
                } else {
                    activeTopics.remove(topicId);
                }
            });

    private TopicConnection connection;
    private CollaborationMap map;

    private Command deactivateCommand;
    private Registration connectionRegistration;

    @Before
    public void setup() {
        VaadinService.setCurrent(service);
        TestUtil.configureTestCollaborationEngine(service, engine);
        connectionRegistration = engine.openTopicConnection(context, "topic",
                SystemUserInfo.getInstance(), connection -> {
                    this.connection = connection;
                    map = connection.getNamedMap("map");

                    return () -> {
                        if (deactivateCommand != null) {
                            deactivateCommand.execute();
                        }
                    };
                });

    }

    @After
    public void cleanUp() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void getUserInfo_receiveTheOriginUserInstance() {
        UserInfo randomUser = new UserInfo(UUID.randomUUID().toString());
        AtomicReference<TopicConnection> topicConnection = new AtomicReference<>();
        engine.openTopicConnection(context, "foo", randomUser, tc -> {
            topicConnection.set(tc);
            return null;
        });
        Assert.assertEquals(randomUser, topicConnection.get().getUserInfo());
    }

    @Test
    public void throwingSubscriber_otherSubscribersNotified() {
        AtomicInteger count = new AtomicInteger();

        map.subscribe(event -> {
            Assert.assertEquals("Other subscriber should not have run yet", 0,
                    count.get());
            throw new FailOnPurposeException();
        });
        map.subscribe(event -> count.incrementAndGet());

        try {
            map.put("key", "value");
            /*
             * At some point, this will change to not let subscriber exceptions
             * propagate out to the method that initiated the change, and then
             * this test will also fail.
             */
            Assert.fail("Expected exception");
        } catch (FailOnPurposeException e) {
        }

        Assert.assertEquals("Other subscriber should have run", 1, count.get());
    }

    @Test
    public void throwingSubscriber_connectionIsClosed() {
        map.subscribe(event -> {
            throw new FailOnPurposeException();
        });

        try {
            map.put("key", "value");
            /*
             * At some point, this will change to not let subscriber exceptions
             * propagate out to the method that initiated the change, and then
             * this test will also fail.
             */
            Assert.fail("Expected exception");
        } catch (FailOnPurposeException e) {
        }

        Assert.assertTrue(context.isClosed());
    }

    @Test
    public void throwingDeactivateHandler_connectionIsProperlyClosed() {
        deactivateCommand = () -> {
            throw new FailOnPurposeException();
        };

        try {
            connectionRegistration.remove();
            /*
             * At some point, this may change to not let close handler
             * exceptions propagate out to the method that triggered closing,
             * and then this test will also fail.
             */
            Assert.fail("Expected exception");
        } catch (FailOnPurposeException e) {
        }

        Assert.assertTrue(context.isClosed());
        Assert.assertTrue(activeTopics.isEmpty());
    }

    @Test
    public void throwingCloseHandler_connectionIsProperlyClosed() {
        context.setThrowOnClose(true);

        try {
            connectionRegistration.remove();
            /*
             * At some point, this may change to not let close handler
             * exceptions propagate out to the method that triggered closing,
             * and then this test will also fail.
             */
            Assert.fail("Expected exception");
        } catch (FailOnPurposeException e) {
        }

        Assert.assertTrue(context.isClosed());
        Assert.assertTrue(activeTopics.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void getNamedMap_throwsIfInactive() {
        connectionRegistration.remove();
        connection.getNamedMap("foo");
    }

    @Test
    public void immediateDeactivate_listenersRemoved() {
        MockConnectionContext context = new MockConnectionContext();
        List<Runnable> actions = new ArrayList<>();
        context.setExecutor(actions::add);
        engine.openTopicConnection(context, "topic2",
                SystemUserInfo.getInstance(), connection -> null);

        context.activate();
        Assert.assertEquals("One action should have been dispatched", 1,
                actions.size());

        context.deactivate();
        Assert.assertEquals("One more action should have been dispatched", 2,
                actions.size());

        Topic topic = engine.getTopic("topic2");
        Assert.assertFalse(
                "There should be no subscribers before dispatching actions",
                topic.hasChangeListeners());

        actions.forEach(Runnable::run);

        Assert.assertFalse(
                "There should be no subscribers after dispatching actions",
                topic.hasChangeListeners());
    }
}
