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
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;
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
        final Path dataDirPath;

        CollaborationEngineConfig(boolean licenseCheckingEnabled,
                Path dataDirPath) {
            this.licenseCheckingEnabled = licenseCheckingEnabled;
            this.dataDirPath = dataDirPath;
        }
    }

    static final String COLLABORATION_ENGINE_NAME = "CollaborationEngine";
    static final String COLLABORATION_ENGINE_VERSION = "3.0";

    static final int USER_COLOR_COUNT = 7;

    private Map<String, Topic> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();
    private Map<String, Integer> activeTopicsCount = new ConcurrentHashMap<>();

    private Supplier<CollaborationEngineConfig> configProvider;
    private CollaborationEngineConfig config;
    private LicenseHandler licenseHandler;
    private LicenseEventHandler licenseEventHandler;

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
     * Gets the {@link CollaborationEngine} instance from the current
     * {@link VaadinService}.
     *
     * @return the {@link CollaborationEngine} instance
     */
    public static CollaborationEngine getInstance() {
        return getInstance(VaadinService.getCurrent());
    }

    /**
     * Gets the {@link CollaborationEngine} instance from the provided
     * {@link VaadinService}.
     *
     * @return the {@link CollaborationEngine} instance
     */
    public static CollaborationEngine getInstance(VaadinService vaadinService) {
        Objects.requireNonNull(vaadinService, "VaadinService cannot be null");
        return vaadinService.getContext()
                .getAttribute(CollaborationEngine.class);
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
     * @return the handle that can be used for configuring or closing the
     *         connection
     */
    public TopicConnectionRegistration openTopicConnection(Component component,
            String topicId, UserInfo localUser,
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
     * @return the handle that can be used for configuring or closing the
     *         connection
     */
    public TopicConnectionRegistration openTopicConnection(
            ConnectionContext context, String topicId, UserInfo localUser,
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
        if (config.licenseCheckingEnabled) {
            boolean hasSeat = licenseHandler.registerUser(localUser.getId());

            if (!hasSeat) {
                // User quota exceeded, don't open the connection.
                return new TopicConnectionRegistration(null, context);
            }
        }

        Topic topic = topics.computeIfAbsent(topicId, id -> new Topic());

        TopicConnection connection = new TopicConnection(context, topic,
                localUser, isActive -> updateTopicActivation(topicId, isActive),
                connectionActivationCallback);
        return new TopicConnectionRegistration(connection, context);
    }

    /**
     * Sets the handler for license events. The handler will be invoked when
     * license events occur, e.g. when the license is expired or when the
     * end-user quota has entered the grace period. The handler can then be used
     * for example to forward these events via e-mail or to a monitoring
     * application to be alerted about the current status of the license.
     * <p>
     * In production mode, the handler must be set before using collaborative
     * features, such as {@link CollaborationBinder} or directly opening a
     * connection to Collaboration Engine.
     * <p>
     * See {@link LicenseEventType} for a list of license event types.
     *
     * @param handler
     *            the license event handler, not {@code null}
     */
    public void setLicenseEventHandler(LicenseEventHandler handler) {
        Objects.requireNonNull(handler, "The handler cannot be null");
        if (licenseEventHandler != null) {
            throw new IllegalStateException(
                    "The handler was already set and it can only be set once.");
        }
        licenseEventHandler = handler;
    }

    /**
     * Requests access for a user to Collaboration Engine. The provided callback
     * will be invoked with a response that tells whether the access is granted.
     * <p>
     * This method can be used to check if the user has access to the
     * Collaboration Engine, e.g. if the license is not expired and there is
     * quota for that user; depending on the response, it's then possible to
     * adapt the UI enabling or disabling collaboration features.
     * <p>
     * In the callback, you can check from the response whether the user has
     * access or not with the {@link AccessResponse#hasAccess()} method. It
     * returns {@code true} if access has been granted for the user.
     * <p>
     * The current {@link UI} is accessed to run the callback, which means that
     * UI updates in the callback are pushed to the client in real-time. Because
     * of depending on the current UI, the method can be called only in the
     * request processing thread, or it will throw.
     *
     * @param user
     *            the user requesting access
     * @param requestCallback
     *            the callback to accept the response
     */
    public void requestAccess(UserInfo user,
            Consumer<AccessResponse> requestCallback) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException("You are calling the requestAccess "
                    + "method without a UI instance being available. You can "
                    + "either move the call where you are sure a UI is defined "
                    + "or directly provide a ConnectionContext to the method. "
                    + "The current UI is automatically defined when processing "
                    + "requests to the server. In other cases, (e.g. from "
                    + "background threads), the current UI is not automatically "
                    + "defined.");
        }
        ComponentConnectionContext context = new ComponentConnectionContext(ui);
        requestAccess(context, user, requestCallback);
    }

    /**
     * Requests access for a user to Collaboration Engine. The provided callback
     * will be invoked with a response that tells whether the access is granted.
     * <p>
     * This method can be used to check if the user has access to the
     * Collaboration Engine, e.g. if the license is not expired and there is
     * quota for that user; depending on the response, it's then possible to
     * adapt the UI enabling or disabling collaboration features.
     * <p>
     * In the callback, you can check from the response whether the user has
     * access or not with the {@link AccessResponse#hasAccess()} method. It
     * returns {@code true} if access has been granted for the user.
     *
     * @param context
     *            context for the connection
     * @param user
     *            the user requesting access
     * @param requestCallback
     *            the callback to accept the response
     */
    public void requestAccess(ConnectionContext context, UserInfo user,
            Consumer<AccessResponse> requestCallback) {
        Objects.requireNonNull(context, "ConnectionContext cannot be null");
        Objects.requireNonNull(user, "UserInfo cannot be null");
        Objects.requireNonNull(requestCallback,
                "AccessResponse cannot be null");

        // Will handle remote connection here

        ensureConfigAndLicenseHandlerInitialization();
        final boolean hasAccess = config.licenseCheckingEnabled
                ? licenseHandler.registerUser(user.getId())
                : true;

        AccessResponse response = new AccessResponse(hasAccess);
        context.dispatchAction(() -> requestCallback.accept(response));
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

    LicenseEventHandler getLicenseEventHandler() {
        return licenseEventHandler;
    }

    synchronized CollaborationEngineConfig getConfig() {
        if (config == null) {
            config = configProvider.get();
        }
        return config;
    }

    private synchronized void ensureConfigAndLicenseHandlerInitialization() {
        if (licenseHandler == null) {
            // Will throw if config is invalid
            licenseHandler = new LicenseHandler(this);
        }
    }
}
