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

/**
 * API for sending and subscribing to updates between clients collaborating on
 * the same collaboration topic.
 *
 * @author Vaadin Ltd
 */
public class TopicConnection {

    private final Topic topic;

    TopicConnection(Topic topic) {
        this.topic = topic;
    }

    Topic getTopic() {
        return topic;
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
     * Adds a subscriber which will be notified whenever someone changes the
     * value of the collaboration topic.
     *
     * @param subscriber
     *            the callback for handling topic value changes
     */
    public void subscribe(SingleValueSubscriber subscriber) {
        topic.subscribe(subscriber);
    }

}
