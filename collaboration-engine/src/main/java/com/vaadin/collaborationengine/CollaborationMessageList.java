/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.Registration;

/**
 * Extension of the {@link MessageList} component which integrates with the
 * {@link CollaborationEngine}. It reads the messages from a topic and renders
 * them within the component. The list is automatically updated when new
 * messages are available. You can use the {@link CollaborationMessageInput}
 * component for submitting messages.
 *
 * @author Vaadin Ltd
 */
public class CollaborationMessageList extends Composite<MessageList>
        implements HasSize, HasStyle {

    static final String MAP_NAME = CollaborationMessageList.class.getName();
    static final String MAP_KEY = "messages";

    private final CollaborationEngine ce;
    private Registration topicRegistration;
    private CollaborationMap map;

    private final UserInfo localUser;

    static {
        UsageStatistics.markAsUsed(
                CollaborationEngine.COLLABORATION_ENGINE_NAME
                        + "/CollaborationMessageList",
                CollaborationEngine.COLLABORATION_ENGINE_VERSION);
    }

    /**
     * Creates a new collaboration message list component with the provided
     * topic id.
     * <p>
     * Whenever a new message is posted to the topic id, this component will
     * render it. Use {@link CollaborationMessageInput} to give controls to the
     * end user to post new messages to the topic.
     * <p>
     * If a {@code null} topic id is provided, the component won't display any
     * messages, until connecting to a non-null topic with
     * {@link #setTopic(String)}.
     *
     * @param localUser
     *            the information of the end user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, or <code>null</code> to not
     *            connect the component to any topic
     */
    public CollaborationMessageList(UserInfo localUser, String topicId) {
        this(localUser, topicId, CollaborationEngine.getInstance());
    }

    CollaborationMessageList(UserInfo localUser, String topicId,
            CollaborationEngine ce) {
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
        this.ce = ce;
        setTopic(topicId);
    }

    /**
     * Sets the topic to use with this component. The connection to the previous
     * topic (if any) and existing messages are removed. A connection to the new
     * topic is opened and the list of messages in the new topic are populated
     * to this component.
     * <p>
     * If the topic id is {@code null}, no messages will be displayed.
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     */
    public void setTopic(String topicId) {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
        }
        if (topicId != null) {
            topicRegistration = ce.openTopicConnection(getContent(), topicId,
                    localUser, this::onConnectionActivate);
        }
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        map = topicConnection.getNamedMap(MAP_NAME);
        map.subscribe(event -> refreshMessages());
        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        map = null;
        refreshMessages();
    }

    private void refreshMessages() {
        List<MessageListItem> messages = map != null
                ? map.get(MAP_KEY, JsonUtil.LIST_MESSAGE_TYPE_REF)
                : Collections.emptyList();
        getContent().setItems(messages);
    }
}
