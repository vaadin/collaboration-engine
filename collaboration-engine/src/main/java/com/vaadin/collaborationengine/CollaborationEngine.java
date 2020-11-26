/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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

    static class CollaborationEngineConfig {
        final boolean licenseCheckingEnabled;

        // TODO: Remove this flag when enabling the enforcements for real. At
        // that point, we need only the licenseCheckingEnabled flag.
        final boolean licenseTermsEnforced;
        final Path dataDirPath;

        CollaborationEngineConfig(boolean licenseCheckingEnabled,
                boolean licenseTermsEnforced, Path dataDirPath) {
            this.licenseCheckingEnabled = licenseCheckingEnabled;
            this.licenseTermsEnforced = licenseTermsEnforced;
            this.dataDirPath = dataDirPath;
        }
    }

    static final String COLLABORATION_ENGINE_NAME = "CollaborationEngine";
    static final String COLLABORATION_ENGINE_VERSION = "3.0";

    private static final CollaborationEngine collaborationEngine = new CollaborationEngine();

    static final int USER_COLOR_COUNT = 7;

    private Map<String, Topic> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();
    private Map<String, Integer> activeTopicsCount = new ConcurrentHashMap<>();

    private Supplier<CollaborationEngineConfig> configProvider;
    private CollaborationEngineConfig config;
    private LicenseHandler licenseHandler;

    private final TopicActivationHandler topicActivationHandler;

    static {
        UsageStatistics.markAsUsed(COLLABORATION_ENGINE_NAME,
                COLLABORATION_ENGINE_VERSION);
    }

    CollaborationEngine() {
        // package-protected to hide from users but to be usable in unit tests
        this((topicId, isActive) -> {
            // implement network sync to the topic
        });
    }

    CollaborationEngine(TopicActivationHandler topicActivationHandler) {
        this.topicActivationHandler = topicActivationHandler;
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

        if (configProvider == null) {
            throw new IllegalStateException(
                    "Collaboration Engine is missing required configuration "
                            + "that should be provided by a VaadinServiceInitListener. "
                            + "Collaboration Engine is supported only in a Vaadin application, "
                            + "where VaadinService initialization is expected to happen before usage.");
        }

        ensureConfigAndLicenseHandlerInitialization();
        if (config.licenseTermsEnforced) {
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
     * Requests access for a user to the Collaboration Engine, then the provided
     * callback will be invoked with a boolean which will be {@code true} if the
     * access is granted.
     * <p>
     * This method can be used to check if the user has access to the
     * Collaboration Engine, e.g. if the license is not expired and there is
     * quota for that user; depending on the response, it's then possible to
     * adapt the UI enabling or disabling collaboration features.
     *
     * @param ui
     *            the UI which will be accessed to execute the callback
     * @param user
     *            the user requesting access
     * @param accessCallback
     *            the callback to accept the response
     */
    public void requestAccess(UI ui, UserInfo user,
            Consumer<Boolean> accessCallback) {
        Objects.requireNonNull(ui, "The UI cannot be null");
        ComponentConnectionContext context = new ComponentConnectionContext(ui);
        requestAccess(context, user, accessCallback);
    }

    /**
     * Requests access for a user to the Collaboration Engine, then the provided
     * callback will be invoked with a boolean which will be {@code true} if the
     * access is granted.
     * <p>
     * This method can be used to check if the user has access to the
     * Collaboration Engine, e.g. if the license is not expired and there is
     * quota for that user; depending on the response, it's then possible to
     * adapt the UI enabling or disabling collaboration features.
     *
     * @param context
     *            context for the connection
     * @param user
     *            the user requesting access
     * @param accessCallback
     *            the callback to accept the response
     */
    public void requestAccess(ConnectionContext context, UserInfo user,
            Consumer<Boolean> accessCallback) {
        Objects.requireNonNull(context, "ConnectionContext cannot be null");
        Objects.requireNonNull(user, "UserInfo cannot be null");
        Objects.requireNonNull(accessCallback, "The callback cannot be null");

        // Will handle remote connection here

        AtomicBoolean hasAccess = new AtomicBoolean(true);
        ensureConfigAndLicenseHandlerInitialization();
        if (config.licenseTermsEnforced) {
            hasAccess.set(licenseHandler.registerUser(user.getId()));
        }

        context.dispatchAction(() -> accessCallback.accept(hasAccess.get()));
    }

    void setConfigProvider(Supplier<CollaborationEngineConfig> configProvider) {
        this.configProvider = configProvider;
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

    private synchronized void ensureConfigAndLicenseHandlerInitialization() {
        if (config == null) {
            config = configProvider.get();
        }
        if (licenseHandler == null) {
            // Will throw if config is invalid
            licenseHandler = new LicenseHandler(config);
        }
    }
}
