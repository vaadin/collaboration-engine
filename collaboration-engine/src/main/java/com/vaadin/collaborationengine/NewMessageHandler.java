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

/**
 * Functional interface that defines how to handle a message when it is added to
 * a topic.
 *
 * @see MessageManager#setNewMessageHandler(NewMessageHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface NewMessageHandler extends Serializable {

    /**
     * The context of the new message.
     */
    interface MessageContext extends Serializable {

        /**
         * Gets the new message.
         *
         * @return the message, not {@code null}
         */
        CollaborationMessage getMessage();
    }

    /**
     * Handles a message when it is added to a topic.
     *
     * @param context
     *            the context of the message, not {@code null}
     */
    void handleNewMessage(MessageContext context);
}
