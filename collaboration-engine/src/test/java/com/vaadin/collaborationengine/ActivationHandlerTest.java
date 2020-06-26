package com.vaadin.collaborationengine;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Command;

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
                topicConnection -> {
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
                topicConnection -> {
                    topicConnection.subscribe(val -> isCalled.set(true));
                    return null;
                });
        context.activate();
        isCalled.set(false);
        context.deactivate();

        SpyConnectionContext otherContext = new SpyConnectionContext();
        collaborationEngine.openTopicConnection(otherContext, "foo",
                topicConnection -> {
                    topicConnection.setValue("baz");
                    return null;
                });
        otherContext.activate();

        Assert.assertFalse(
                "Expect the deactivated connection to not notify its subscriber",
                isCalled.get());
    }

    WeakReference<SingleValueSubscriber> weakSubscriber;

    @Test
    public void deactivateConnection_garbageCollectedTheTopicSubscriber()
            throws InterruptedException {
        collaborationEngine.openTopicConnection(context, "foo",
                topicConnection -> {
                    SingleValueSubscriber subscriber = new SingleValueSubscriber() {
                        @Override
                        public void onValueChange(Object value) {
                            // no impl
                        }
                    };
                    weakSubscriber = new WeakReference<>(subscriber);
                    topicConnection.subscribe(weakSubscriber.get());
                    subscriber = null;
                    return null;
                });
        context.activate();
        context.deactivate();
        Assert.assertTrue(
                "Expect subscriber to be garbage-collected when connection is deactivated",
                isGarbageCollected(this.weakSubscriber));
    }

    @Test
    public void deactivatedConnection_triggerConnectionCallback() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                topicConnection -> {
                    topicConnection.subscribe(val -> {
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
                topicConnection -> {
                    isCalled.set(true);
                    topicConnection.subscribe(val -> {
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

    private static boolean isGarbageCollected(WeakReference<?> ref)
            throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            if (ref.get() == null) {
                return true;
            }
        }
        return false;
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
        public void setActivationHandler(ActivationHandler handler) {
            activationHandler = handler;
        }

        @Override
        public void dispatchAction(Command action) {
            action.execute();
        }
    }
}
