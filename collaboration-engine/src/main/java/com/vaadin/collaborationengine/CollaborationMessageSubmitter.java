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

import com.vaadin.flow.shared.Registration;

/**
 * Submitter of messages. A submitter can be set on a
 * {@link CollaborationMessageList} to setup the message appending logic when
 * the connection to the associated topic is activated and also to return a
 * callback to handle connection deactivation (e.g. disabling an input field).
 *
 * @author Vaadin Ltd.
 * @see CollaborationMessageList#setSubmitter(CollaborationMessageSubmitter)
 */
@FunctionalInterface
public interface CollaborationMessageSubmitter extends Serializable {

    /**
     * The activation context of a {@link CollaborationMessageSubmitter}.
     */
    interface ActivationContext extends Serializable {

        /**
         * Appends a message.
         *
         * @param message
         *            the message content
         */
        void appendMessage(String message);
    }

    /**
     * Handles the activation of the submitter, for example setting up an input
     * component to append new messages using the
     * {@link ActivationContext#appendMessage(String)} method. The returned
     * {@link Registration} callback can be used to handle the deactivation of
     * the submitter, for example to disable an input component.
     *
     * @param activationContext
     *            the activation context
     * @return the registration which will be removed when the connection is
     *         deactivated, not null
     */
    Registration onActivation(ActivationContext activationContext);
}
