/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
 * @since 3.1
 *
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
