package com.vaadin.collaborationengine;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class SingleValueSubscriptionTest {
    private static class SubscriberSpy implements SingleValueSubscriber {
        private Object lastValue = null;
        private boolean updated = false;

        @Override
        public void onValueChange(Object value) {
            Assert.assertFalse("Event was not expected at this time", updated);
            updated = true;
            lastValue = value;
        }

        public void assertNoUpdate(String message) {
            Assert.assertFalse(message, updated);
        }

        public void assertUpdate(String message, Object expectedValue) {
            Assert.assertTrue("No value has been set", updated);
            Assert.assertEquals(message, expectedValue, lastValue);
            reset();
        }

        public void reset() {
            updated = false;
        }

    }

    private CollaborationEngine collaborationEngine;
    private ConnectionContext context;
    private TopicConnection topicConnection;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        context = new ConnectionContext() {
            @Override
            public void setActivationHandler(ActivationHandler handler) {
                handler.setActive(true);
            }

            @Override
            public void dispatchAction(Command action) {
                action.execute();
            }
        };

        collaborationEngine.openTopicConnection(context, "foo", tc -> {
            topicConnection = tc;
            tc.subscribe(val -> {
            });
            return () -> {
            };
        });
    }

    @Test
    public void subscribeTopic_subscriberReceivesCurrentValue() {
        String value = "bar";
        AtomicBoolean isCalled = new AtomicBoolean(false);
        topicConnection.setValue(value);
        topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isCalled));
        Assert.assertEquals(
                "The set value should be available through the getter", value,
                topicConnection.getValue());
        Assert.assertTrue("Expected subscriber to be notified", isCalled.get());
    }

    @Test
    public void subscribeTopic_setValue_allSubscribersReceiveCurrentValue() {
        String value = "bar";
        AtomicBoolean isFirstCalled = new AtomicBoolean(false);
        topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isFirstCalled));

        AtomicBoolean isOtherCalled = new AtomicBoolean(false);
        topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isOtherCalled));

        topicConnection.setValue(value);

        Assert.assertTrue("Expected the first subscriber to be notified",
                isFirstCalled.get());
        Assert.assertTrue("Expected the second subscriber to be notified",
                isOtherCalled.get());
    }

    @Test
    public void subscribeTopic_onlyNewSubscriberReceivesCurrentValue() {
        String value = "bar";
        AtomicBoolean isFirstCalled = new AtomicBoolean(false);

        topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isFirstCalled));
        topicConnection.setValue(value);
        isFirstCalled.set(false);

        AtomicBoolean isOtherCalled = new AtomicBoolean(false);
        topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isOtherCalled));

        Assert.assertFalse(
                "Expected the existing subscriber to not be notified",
                isFirstCalled.get());
        Assert.assertTrue("Expected the new subscriber to be notified",
                isOtherCalled.get());
    }

    @Test
    public void subscribeWhileNotifyingSubscribers_doesNotThrow() {
        topicConnection.subscribe(val -> {
            topicConnection.subscribe(val2 -> {
            });
        });
        topicConnection.setValue("bar");
    }

    private SingleValueSubscriber getTestSingleValueSubscriber(
            String currentValue, AtomicBoolean isCalled) {
        return newValue -> {
            if (newValue != null) {
                Assert.assertFalse(
                        "Expected subscriber to be notified only once",
                        isCalled.get());
                Assert.assertEquals(
                        "Expected subscriber to get the current value",
                        currentValue, newValue);
                isCalled.set(true);
            }
        };
    }

    @Test
    public void unsubscribeTopic_subscriberNoLongerReceiveCurrentValue() {
        String value = "bar";

        AtomicBoolean isCalled = new AtomicBoolean(false);
        Registration registration = topicConnection
                .subscribe(getTestSingleValueSubscriber(value, isCalled));

        isCalled.set(false);
        registration.remove();
        topicConnection.setValue(value);

        Assert.assertFalse(isCalled.get());
    }

    @Test
    public void compareAndSet_currentValuePassed_newValueSet() {
        SubscriberSpy spy = new SubscriberSpy();

        topicConnection.setValue(LocalDate.of(2001, 1, 1));
        topicConnection.subscribe(spy);
        spy.reset();

        Object newValue = LocalDate.of(2002, 2, 2);
        boolean success = topicConnection
                .compareAndSet(LocalDate.of(2001, 1, 1), newValue);
        Assert.assertTrue("Operation should have succeeded", success);
        spy.assertUpdate("Value should be updated", newValue);
    }

    @Test
    public void compareAndSet_otherValuePassed_oldValueRemains() {
        Object value = LocalDate.of(2001, 1, 1);
        SubscriberSpy spy = new SubscriberSpy();

        topicConnection.setValue(value);
        topicConnection.subscribe(spy);
        spy.reset();

        Object newValue = new Object();

        boolean success = topicConnection
                .compareAndSet(LocalDate.of(2002, 2, 2), newValue);
        Assert.assertFalse("Operation should not have succeeded", success);
        spy.assertNoUpdate("Value should not have changed");
        Assert.assertSame("Same value should be in use", value,
                topicConnection.getValue());
    }

}
