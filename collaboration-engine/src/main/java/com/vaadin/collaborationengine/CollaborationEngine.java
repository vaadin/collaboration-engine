/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.shared.Registration;

/**
 * CollaborationEngine is an API for creating collaborative experiences in
 * Vaadin applications. It's used by sending and subscribing to changes between
 * collaborators via {@link TopicConnection collaboration topics}.
 * <p>
 * Use {@link #getInstance()} to get a reference to the singleton object.
 *
 * @author Vaadin Ltd
 */
@JsModule("./field-highlighter/src/vaadin-field-highlighter.js")
public class CollaborationEngine {

    static final String COLLABORATION_ENGINE_NAME = "CollaborationEngine";
    static final String COLLABORATION_ENGINE_VERSION = "1.0";

    private static final CollaborationEngine collaborationEngine = new CollaborationEngine();

    static final int USER_COLOR_COUNT = 7;

    private Map<String, Topic> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();
    private Map<String, Integer> activeTopicsCount = new ConcurrentHashMap<>();

    private final boolean licenseCheckingEnabled;
    private final LicenseHandler licenseHandler;

    private final TopicActivationHandler topicActivationHandler;

    static {
        UsageStatistics.markAsUsed(COLLABORATION_ENGINE_NAME,
                COLLABORATION_ENGINE_VERSION);
    }

    CollaborationEngine() {
        // package-protected to hide from users but to be usable in unit tests
        this(false, (topicId, isActive) -> {
            // implement network sync to the topic
        });
    }

    CollaborationEngine(boolean licenseCheckingEnabled,
            TopicActivationHandler topicActivationHandler) {
        this.licenseCheckingEnabled = licenseCheckingEnabled;
        this.topicActivationHandler = topicActivationHandler;
        this.licenseHandler = licenseCheckingEnabled ? new LicenseHandler()
                : null;
    }

    private void updateTopicActivation(String topicId, Boolean isActive) {
        activeTopicsCount.putIfAbsent(topicId, 0);
        activeTopicsCount.computeIfPresent(topicId, (topic, count) -> {
            int newCount = isActive ? count + 1 : count - 1;
            if (newCount <= 0) {
                activeTopicsCount.remove(topicId);
                topicActivationHandler.setActive(topicId, false);
            } else if (isActive && newCount == 1) {
                topicActivationHandler.setActive(topicId, true);
            }
            return newCount;
        });
    }

    /**
     * Gets the {@link CollaborationEngine} singleton.
     *
     * @return the {@link CollaborationEngine} singleton
     */
    public static CollaborationEngine getInstance() {
        return collaborationEngine;
    }

    /**
     * Opens a connection to the collaboration topic with the provided id based
     * on a component instance. If the topic with the provided id does not exist
     * yet, it's created on demand.
     *
     * @param component
     *            the component which hold UI access, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     * @param localUser
     *            the user who is related to the topic connection, a
     *            {@link SystemUserInfo} can be used for non-interaction
     *            threads. Not {@code null}.
     * @param connectionActivationCallback
     *            the callback to be executed when a connection is activated,
     *            not {@code null}
     * @return the handle that can be used for closing the connection
     */
    public Registration openTopicConnection(Component component, String topicId,
            UserInfo localUser,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(component, "Connection context can't be null");
        ConnectionContext context = new ComponentConnectionContext(component);
        return openTopicConnection(context, topicId, localUser,
                connectionActivationCallback);
    }

    /**
     * Opens a connection to the collaboration topic with the provided id based
     * on a generic context definition. If the topic with the provided id does
     * not exist yet, it's created on demand.
     *
     * @param context
     *            context for the connection
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     * @param localUser
     *            the user who is related to the topic connection, a
     *            {@link SystemUserInfo} can be used for non-interaction
     *            threads. Not {@code null}.
     * @param connectionActivationCallback
     *            the callback to be executed when a connection is activated,
     *            not {@code null}
     * @return the handle that can be used for closing the connection
     */
    public Registration openTopicConnection(ConnectionContext context,
            String topicId, UserInfo localUser,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(context, "Connection context can't be null");
        Objects.requireNonNull(topicId, "Topic id can't be null");
        Objects.requireNonNull(localUser, "User can't be null");
        Objects.requireNonNull(connectionActivationCallback,
                "Callback for connection activation can't be null");

        if (licenseCheckingEnabled) {
            boolean hasSeat = licenseHandler.registerUser(localUser.getId());

            if (!hasSeat) {
                // User quota exceeded, don't open the connection.
                return () -> {
                    // Nothing to do for closing the connection.
                };
            }
        }

        Topic topic = topics.computeIfAbsent(topicId, id -> new Topic());

        TopicConnection connection = new TopicConnection(context, topic,
                localUser, isActive -> updateTopicActivation(topicId, isActive),
                connectionActivationCallback);
        return connection::deactivateAndClose;
    }

    /**
     * Gets the color index of a user based on the user id. If the color index
     * for a user id does not exist yet, it's created on demand.
     *
     * @param userId
     *            user id
     * @return the color index
     */
    int getUserColorIndex(String userId) {
        Integer colorIndex = userColors.computeIfAbsent(userId,
                id -> userColors.size() % USER_COLOR_COUNT);
        return colorIndex.intValue();
    }

    /**
     * Gets the internal license handler. Package protected for testing
     * purposes.
     *
     * @return the license handler
     */
    LicenseHandler getLicenseHandler() {
        return licenseHandler;
    }
}
