/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.collaborationengine.examplecomponent.ExampleComponent;
import com.vaadin.collaborationengine.examplecomponent.Message;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.Registration;

/**
 * Extension of the {@link ExampleComponent} component which integrates with the
 * {@link CollaborationEngine}. It displays a list of messages added to the
 * topic, enables users to add more messages to the topic, and relays any new
 * messages to other users instantly.
 * 
 * @author Vaadin Ltd
 */
public class CollaborationExampleComponent extends Composite<ExampleComponent> {

    private static final String MAP_NAME = CollaborationExampleComponent.class
            .getName();
    private static final String MAP_KEY = "activity";

    private final CollaborationEngine ce;
    private TopicConnectionRegistration topicRegistration;
    private CollaborationMap map;

    private final UserInfo localUser;

    static {
        UsageStatistics.markAsUsed(
                CollaborationEngine.COLLABORATION_ENGINE_NAME
                        + "/CollaborationExampleComponent",
                CollaborationEngine.COLLABORATION_ENGINE_VERSION);
    }

    /**
     * Creates a new collaboration example component with the provided local
     * user.
     * <p>
     * Collaboration example component enables users to discuss with each other
     * around a topic, providing a stream of messages as well as controls to add
     * new messages.
     * <p>
     * The provided user information is used to identify who has sent each
     * message.
     * <p>
     * If a {@code null} topic id is provided, the component won't display any
     * messages and the controls for sending new messages are disabled. The list
     * of messages will be repopulated and controls enabled when a non-null
     * topic is provided with {@link #setTopic(String)}.
     *
     * @param localUser
     *            the information of the local user
     * @param topicId
     *            the id of the topic to connect to, or <code>null</code> to not
     *            connect the component to any topic
     */
    public CollaborationExampleComponent(UserInfo localUser, String topicId) {
        this(localUser, topicId, CollaborationEngine.getInstance());
    }

    CollaborationExampleComponent(UserInfo localUser, String topicId,
            CollaborationEngine ce) {
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
        this.ce = ce;
        getContent().setSubmitListener(this::submitMessage);
        setTopic(topicId);
    }

    /**
     * Sets the topic to use with this component. The connection to the previous
     * topic (if any) and existing messages are removed. Connection to the new
     * topic is opened and the messages in the new topic are populated to this
     * component.
     * <p>
     * If the topic id is {@code null}, no messages will be displayed and the
     * controls for sending new messages will be disabled.
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     */
    public void setTopic(String topicId) {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
        }
        setFieldInputsEnabled(false);
        if (topicId != null) {
            topicRegistration = ce.openTopicConnection(getContent(), topicId,
                    localUser, this::onConnectionActivate);
        }
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        map = topicConnection.getNamedMap(MAP_NAME);
        map.subscribe(event -> refreshMessages());
        setFieldInputsEnabled(true);
        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        map = null;
        refreshMessages();
        setFieldInputsEnabled(false);
    }

    private void setFieldInputsEnabled(boolean enabled) {
        getContent().getCommentField().setEnabled(enabled);
        getContent().getSubmitMessageButton().setEnabled(enabled);
    }

    private void refreshMessages() {
        List<Message> messages = map != null
                ? map.get(MAP_KEY, JsonUtil.LIST_MESSAGE_TYPE_REF)
                : Collections.emptyList();
        getContent().setMessages(messages);
    }

    private void submitMessage(String content) {
        Objects.requireNonNull(map, "CollaborationMap cannot be null");
        List<Message> messages = map.get(MAP_KEY,
                JsonUtil.LIST_MESSAGE_TYPE_REF);
        List<Message> newMessages = messages != null ? new ArrayList<>(messages)
                : new ArrayList<>();
        Message message = new Message(content, localUser.getName(),
                localUser.getImage(), LocalDateTime.now());
        newMessages.add(message);
        map.replace(MAP_KEY, messages, newMessages).thenAccept(success -> {
            if (!success) {
                submitMessage(content);
            }
        });
    }
}
