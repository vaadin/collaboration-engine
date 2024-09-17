/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
    public void immediateDeactivate_connectionNotActivated() {
        MockConnectionContext context = new MockConnectionContext();
        List<Runnable> actions = new ArrayList<>();
        context.setExecutor(actions::add);

        engine.openTopicConnection(context, "topic2",
                SystemUserInfo.getInstance(), connection -> {
                    Assert.fail("Activation should never run");
                    return null;
                });

        context.activate();
        context.deactivate();

        Topic topic = engine.getTopic("topic2");
        Assert.assertFalse(
                "There should be no subscribers before dispatching actions",
                topic.hasChangeListeners());

        Assert.assertFalse("There should be pending actions",
                actions.isEmpty());
        actions.forEach(Runnable::run);

        Assert.assertFalse(
                "There should be no subscribers after dispatching actions",
                topic.hasChangeListeners());
    }

    @Test
    public void immediateReactivate_deactivateNeverHappened() {
        MockConnectionContext context = new MockConnectionContext();
        List<Runnable> actions = new ArrayList<>();
        context.setExecutor(actions::add);

        AtomicInteger activationCount = new AtomicInteger();

        engine.openTopicConnection(context, "topic2",
                SystemUserInfo.getInstance(), connection -> {
                    activationCount.incrementAndGet();
                    return () -> Assert.fail("Deactivation should never run");
                });
        context.activate();
        actions.forEach(Runnable::run);
        actions.clear();

        Assert.assertEquals("Sanity check", 1, activationCount.get());

        context.deactivate();
        context.activate();

        actions.forEach(Runnable::run);
        Assert.assertEquals("Activation should have run only once", 1,
                activationCount.get());
    }
}
