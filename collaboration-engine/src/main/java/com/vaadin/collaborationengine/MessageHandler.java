/*
 * Copyright 2020-2022 Vaadin Ltd.
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
 * @see MessageManager#setMessageHandler(MessageHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface MessageHandler extends Serializable {

    /**
     * The context of the message.
     */
    interface MessageContext extends Serializable {

        /**
         * Gets the message.
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
    void handleMessage(MessageContext context);
}
