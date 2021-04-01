/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.time.Instant;

/**
 * Message of a {@link CollaborationMessageList}.
 *
 * @author Vaadin Ltd
 */
public class CollaborationMessage implements Serializable {
    private UserInfo user;
    private String text;
    private Instant time;

    /**
     * Creates a new message.
     */
    public CollaborationMessage() {
        // Needed for Jackson deserialization
    }

    /**
     * Creates a new message with the specified {@code user} as the message
     * author info, {@code text} as the message content and {@code time} as the
     * message timestamp.
     *
     * @param user
     *            the user-info of the message author
     * @param text
     *            the content of the message
     * @param time
     *            the timestamp of the message
     */
    public CollaborationMessage(UserInfo user, String text, Instant time) {
        this.user = user;
        this.text = text;
        this.time = time;
    }

    /**
     * Gets the message author user-info.
     *
     * @return the user-info
     */
    public UserInfo getUser() {
        return user;
    }

    /**
     * Sets the message author user-info.
     *
     * @param user
     *            the user-info
     */
    public void setUser(UserInfo user) {
        this.user = user;
    }

    /**
     * Gets the message content.
     *
     * @return the message content
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the message content.
     *
     * @param text
     *            the message content
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the message timestamp.
     *
     * @return the message timestamp
     */
    public Instant getTime() {
        return time;
    }

    /**
     * Sets the message timestamp.
     *
     * @param time
     *            the message timestamp
     */
    public void setTime(Instant time) {
        this.time = time;
    }
}
