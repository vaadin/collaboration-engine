/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
