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

import com.vaadin.flow.shared.Registration;

/**
 * API for sending and subscribing to updates between clients collaborating on
 * the same collaboration topic.
 *
 * @author Vaadin Ltd
 */
public class TopicConnection {

    private final Topic topic;
    private final ConnectionContext context;

    TopicConnection(ConnectionContext context, Topic topic) {
        this.topic = topic;
        this.context = context;
    }

    Topic getTopic() {
        return topic;
    }

    /**
     * Gets the current topic value.
     *
     * @return the topic value
     */
    public Object getValue() {
        return topic.getValue();
    }

    /**
     * Sets the value of the connected collaboration topic, notifying all
     * subscribers.
     *
     * @param value
     *            the new value to set for the topic
     */
    public void setValue(Object value) {
        topic.setValue(value);
    }

    /**
     * Atomically updates the value if the current value {@code equals} the
     * expected value. Subscribers are notified only if the value is updated.
     *
     * @param expected
     *            the expected value
     * @param update
     *            the value to set if the expected value is currently set
     * @return <code>true</code> if the value was updated, <code>false</code> if
     *         the previous value is retained
     */
    public boolean compareAndSet(Object expected, Object update) {
        return topic.compareAndSet(expected, update);
    }

    /**
     * Adds a subscriber which will be notified whenever someone changes the
     * value of the collaboration topic.
     *
     * @param subscriber
     *            the callback for handling topic value changes
     * @return a handle that can be used for removing the subscription, not
     *         <code>null</code>
     */
    public Registration subscribe(SingleValueSubscriber subscriber) {
        return topic.subscribe(newValue -> context
                .dispatchAction(() -> subscriber.onValueChange(newValue)));
    }

}
