/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Objects;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.Registration;

/**
 * Extension of the {@link MessageInput} component which integrates with the
 * {@link CollaborationMessageList}. The user can type a message and submit it.
 * The messages will be displayed in any {@link CollaborationMessageList} that
 * is connected to the same topic as the list passed as the argument of this
 * component constructor. The text area and button will be disabled while the
 * connection to the topic is not active or the topic is set to
 * <code>null</code> (see {@link CollaborationMessageList#setTopic(String)}).
 *
 * @author Vaadin Ltd
 */
public class CollaborationMessageInput extends Composite<MessageInput>
        implements HasSize, HasStyle {

    static {
        UsageStatistics.markAsUsed(
                CollaborationEngine.COLLABORATION_ENGINE_NAME
                        + "/CollaborationMessageInput",
                CollaborationEngine.COLLABORATION_ENGINE_VERSION);
    }

    /**
     * Creates a new collaboration message input component which submits
     * messages to the provided {@link CollaborationMessageList}.
     *
     * @param list
     *            the list which will display the submitted messages, not null
     */
    public CollaborationMessageInput(CollaborationMessageList list) {
        Objects.requireNonNull(list,
                "A list instance to connect this component to is required");
        getContent().setEnabled(false);
        list.setSubmitter(activationContext -> {
            getContent().setEnabled(true);
            Registration registration = getContent().addSubmitListener(
                    event -> activationContext.appendMessage(event.getValue()));
            return () -> {
                registration.remove();
                getContent().setEnabled(false);
            };
        });
    }
}
