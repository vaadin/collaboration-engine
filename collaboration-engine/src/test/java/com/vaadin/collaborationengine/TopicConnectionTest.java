package com.vaadin.collaborationengine;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

public class TopicConnectionTest {

    private static class SpyConnectionContext implements ConnectionContext {
        boolean closed = false;
        boolean throwOnClose = false;

        @Override
        public Registration setActivationHandler(ActivationHandler handler) {
            handler.setActive(true);
            return () -> {
                closed = true;
                if (throwOnClose) {
                    throw new FailOnPurposeException();
                }
            };
        }

        @Override
        public void dispatchAction(Command action) {
            action.execute();
        }

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            return null;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    private static class FailOnPurposeException extends RuntimeException {
    }

    private final SpyConnectionContext context = new SpyConnectionContext();

    private final Set<String> activeTopics = new HashSet<>();

    private final VaadinService service = new MockService();

    private final CollaborationEngine engine = new CollaborationEngine(
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
        context.throwOnClose = true;

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
}
