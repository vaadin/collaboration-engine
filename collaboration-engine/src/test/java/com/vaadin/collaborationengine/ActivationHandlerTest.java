package com.vaadin.collaborationengine;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class ActivationHandlerTest {

    private CollaborationEngine collaborationEngine;
    private SpyConnectionContext context;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        context = new SpyConnectionContext();
    }

    @Test
    public void openTopicConnection_triggerCallbackOnlyWhenActivated() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    isCalled.set(true);
                    return null;
                });

        Assert.assertFalse("Expect the connection callback not to be called",
                isCalled.get());
        context.activate();
        Assert.assertTrue("Expect the connection callback to be called",
                isCalled.get());
    }

    @Test
    public void deactivateConnection_subscriberNoLongerReceiveCurrentValue() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map")
                            .subscribe(event -> isCalled.set(true));
                    return null;
                });
        context.activate();
        isCalled.set(false);
        context.deactivate();

        SpyConnectionContext otherContext = new SpyConnectionContext();
        collaborationEngine.openTopicConnection(otherContext, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map").put("bar", "baz");
                    return null;
                });
        otherContext.activate();

        Assert.assertFalse(
                "Expect the deactivated connection to not notify its subscriber",
                isCalled.get());
    }

    WeakReference<MapSubscriber> weakSubscriber;

    @Test
    public void deactivateConnection_garbageCollectedTheTopicSubscriber()
            throws InterruptedException {
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    MapSubscriber subscriber = new MapSubscriber() {
                        @Override
                        public void onMapChange(MapChangeEvent event) {
                            // nop
                        }
                    };
                    weakSubscriber = new WeakReference<>(subscriber);
                    topicConnection.getNamedMap("map").subscribe(subscriber);
                    subscriber = null;
                    return null;
                });
        context.activate();
        context.deactivate();
        Assert.assertTrue(
                "Expect subscriber to be garbage-collected when connection is deactivated",
                TestUtils.isGarbageCollected(this.weakSubscriber));
    }

    @Test
    public void deactivatedConnection_triggerConnectionCallback() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map").subscribe(event -> {
                    });
                    return () -> isCalled.set(true);
                });
        context.activate();
        context.deactivate();
        Assert.assertTrue(
                "Expect the returned Registration of connection callback to be called when deactivated",
                isCalled.get());
    }

    @Test
    public void reactivatedConnection_triggerConnectionCallbackAgain() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    isCalled.set(true);
                    topicConnection.getNamedMap("").subscribe(event -> {
                    });
                    return null;
                });
        context.activate();
        context.deactivate();
        isCalled.set(false);

        context.activate();
        Assert.assertTrue(
                "Expect the returned Registration of connection callback to be called when deactivated",
                isCalled.get());
    }

    class SpyConnectionContext implements ConnectionContext {

        private ActivationHandler activationHandler;

        public void activate() {
            activationHandler.setActive(true);
        }

        public void deactivate() {
            activationHandler.setActive(false);
        }

        @Override
        public Registration setActivationHandler(ActivationHandler handler) {
            activationHandler = handler;
            return null;
        }

        @Override
        public void dispatchAction(Command action) {
            action.execute();
        }

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            return new CompletableFuture<>();
        }
    }
}
