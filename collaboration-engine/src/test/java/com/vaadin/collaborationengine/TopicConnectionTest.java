/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Command;
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

    private final CollaborationEngine engine = new CollaborationEngine(
            (topicId, isActive) -> {
                if (isActive) {
                    activeTopics.add(topicId);
                } else {
                    activeTopics.remove(topicId);
                }
            });

    private CollaborationMap map;

    private Command deactivateCommand;
    private Registration connectionRegistration;

    @Before
    public void setup() {
        connectionRegistration = engine.openTopicConnection(context, "topic",
                SystemUserInfo.get(), connection -> {
                    map = connection.getNamedMap("map");

                    return () -> {
                        if (deactivateCommand != null) {
                            deactivateCommand.execute();
                        }
                    };
                });

    }

    @Test
    public void getUserInfo_receiveTheOriginUserInstance() {
        UserInfo randomUser = new UserInfo(UUID.randomUUID().toString());
        AtomicReference<TopicConnection> topicConnection = new AtomicReference<>();
        new CollaborationEngine().openTopicConnection(context, "foo",
                randomUser, tc -> {
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
}
