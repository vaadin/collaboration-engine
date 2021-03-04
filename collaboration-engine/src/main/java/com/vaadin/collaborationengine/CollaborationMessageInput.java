/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.Registration;

/**
 * Extension of the {@link MessageInput} component which integrates with the
 * {@link CollaborationEngine}. The user can type a message and submit it. The
 * messages will be displayed in any {@link CollaborationMessageList} that is
 * connected to the same topic. The text area and button will be disabled if the
 * topic is set to <code>null</code>.
 *
 * @author Vaadin Ltd
 */
public class CollaborationMessageInput extends Composite<MessageInput>
        implements HasSize, HasStyle {

    static final String MAP_NAME = CollaborationMessageList.class.getName();
    static final String MAP_KEY = CollaborationMessageList.MAP_KEY;

    private final CollaborationEngine ce;
    private Registration topicRegistration;
    private CollaborationMap map;

    private final UserInfo localUser;

    static {
        UsageStatistics.markAsUsed(
                CollaborationEngine.COLLABORATION_ENGINE_NAME
                        + "/CollaborationMessageInput",
                CollaborationEngine.COLLABORATION_ENGINE_VERSION);
    }

    /**
     * Creates a new collaboration message input component with the provided
     * topic id.
     * <p>
     * The provided user information is used for identifying who sent the
     * message.
     * <p>
     * A {@link CollaborationMessageList}, connected to the same topic id, can
     * be used to render the messages sent by the users.
     * <p>
     * If a {@code null} topic id is provided, the text area and button will be
     * disabled, until connecting to a non-null topic with
     * {@link #setTopic(String)}.
     *
     * @param localUser
     *            the information of the end user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, or <code>null</code> to not
     *            connect the component to any topic
     */
    public CollaborationMessageInput(UserInfo localUser, String topicId) {
        this(localUser, topicId, CollaborationEngine.getInstance());
    }

    CollaborationMessageInput(UserInfo localUser, String topicId,
            CollaborationEngine ce) {
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
        this.ce = ce;
        setTopic(topicId);
        getContent().addSubmitListener(this::submitMessage);
    }

    /**
     * Sets the topic to use with this component. The connection to the previous
     * topic (if any) is closed and a connection to the new topic is opened.
     * Messages submitted via this component will be rendered in any
     * {@link CollaborationMessageList} that is connected to the same topic.
     * <p>
     * If the topic id is {@code null}, the component will be disabled.
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     */
    public void setTopic(String topicId) {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
        }
        getContent().setEnabled(false);
        if (topicId != null) {
            topicRegistration = ce.openTopicConnection(getContent(), topicId,
                    localUser, this::onConnectionActivate);
        }
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        map = topicConnection.getNamedMap(MAP_NAME);
        getContent().setEnabled(true);
        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        map = null;
        getContent().setEnabled(false);
    }

    void submitMessage(MessageInput.SubmitEvent event) {
        Objects.requireNonNull(map, "CollaborationMap cannot be null");
        List<MessageListItem> messages = map.get(MAP_KEY,
                JsonUtil.LIST_MESSAGE_TYPE_REF);
        List<MessageListItem> newMessages = messages != null
                ? new ArrayList<>(messages)
                : new ArrayList<>();
        MessageListItem submittedMessage = new MessageListItem(event.getValue(),
                Instant.now(), localUser.getName(), localUser.getImage());
        submittedMessage.setUserAbbreviation(localUser.getAbbreviation());
        submittedMessage.setUserColorIndex(ce.getUserColorIndex(localUser));
        newMessages.add(submittedMessage);
        map.replace(MAP_KEY, messages, newMessages).thenAccept(success -> {
            if (!success) {
                submitMessage(event);
            }
        });
    }
}
