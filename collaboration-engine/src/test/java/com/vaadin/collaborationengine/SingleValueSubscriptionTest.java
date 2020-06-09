package com.vaadin.collaborationengine;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleValueSubscriptionTest {

    private CollaborationEngine collaborationEngine;
    private TopicConnection topicConnection;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        topicConnection = collaborationEngine.openTopicConnection("foo");
    }

    @Test
    public void subscribeTopic_subscriberReceivesCurrentValue() {
        String value = "bar";
        topicConnection.setValue(value);

        AtomicBoolean isCalled = new AtomicBoolean(false);
        topicConnection.subscribe(newValue -> {
            Assert.assertFalse("Expected subscriber to be notified only once",
                    isCalled.get());
            Assert.assertEquals("Expected subscriber to get the current value",
                    value, newValue);
            isCalled.set(true);
        });
        Assert.assertTrue("Expected subscriber to be notified", isCalled.get());
    }

    @Test
    public void subscribeTopic_setValue_allSubscribersReceiveCurrentValue() {
        TopicConnection otherConnection = collaborationEngine
                .openTopicConnection("foo");

        String value = "bar";

        Function<AtomicBoolean, SingleValueSubscriber> factory = isCalled -> newValue -> {
            if (newValue != null) {
                Assert.assertFalse(
                        "Expected subscriber to be notified only once",
                        isCalled.get());
                Assert.assertEquals(
                        "Expected subscriber to get the current value", value,
                        newValue);
                isCalled.set(true);
            }
        };

        AtomicBoolean isFirstCalled = new AtomicBoolean(false);
        AtomicBoolean isOtherCalled = new AtomicBoolean(false);

        topicConnection.subscribe(factory.apply(isFirstCalled));
        otherConnection.subscribe(factory.apply(isOtherCalled));

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
        AtomicBoolean isOtherCalled = new AtomicBoolean(false);

        TopicConnection otherConnection = collaborationEngine
                .openTopicConnection("foo");

        Function<AtomicBoolean, SingleValueSubscriber> factory = isCalled -> newValue -> {
            if (newValue != null) {
                Assert.assertFalse(
                        "Expected subscriber to be notified only once",
                        isCalled.get());
                Assert.assertEquals(
                        "Expected subscriber to get the current value", value,
                        newValue);
                isCalled.set(true);
            }
        };

        topicConnection.subscribe(factory.apply(isFirstCalled));

        topicConnection.setValue(value);

        isFirstCalled.set(false);

        otherConnection.subscribe(factory.apply(isOtherCalled));

        Assert.assertFalse(
                "Expected the existing subscriber to not be notified",
                isFirstCalled.get());
        Assert.assertTrue("Expected the new subscriber to be notified",
                isOtherCalled.get());
    }

    @Test
    public void subscribeWhileNotifyingSubscribers_doesNotThrow() {
        TopicConnection otherConnection = collaborationEngine
                .openTopicConnection("foo");
        topicConnection.subscribe(val -> {
            otherConnection.subscribe(val2 -> {
            });
        });
        otherConnection.setValue("bar");
    }

}
