/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.pro.licensechecker.LicenseChecker;

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

    static final Logger LOGGER = LoggerFactory
            .getLogger(CollaborationEngine.class);

    static final String COLLABORATION_ENGINE_NAME = "vaadin-collaboration-engine";
    static final String COLLABORATION_ENGINE_VERSION = "3.1";

    static final int USER_COLOR_COUNT = 7;

    private Map<String, Topic> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();
    private Map<String, Integer> activeTopicsCount = new ConcurrentHashMap<>();

    private LicenseHandler licenseHandler;

    private CollaborationEngineConfiguration configuration;

    private final TopicActivationHandler topicActivationHandler;

    private Clock clock = Clock.systemUTC();

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
                .getAttribute(CollaborationEngine.class, () -> {
                    // CollaborationEngineConfiguration has not been provided

                    if (vaadinService.getDeploymentConfiguration()
                            .isProductionMode()) {
                        throw new IllegalStateException(
                                "Vaadin is running in production mode, and "
                                        + "Collaboration Engine is missing a required configuration object. "
                                        + "The configuration should be "
                                        + "set by calling the static CollaborationEngine.configure() method "
                                        + "in a VaadinServiceInitListener or, if using Spring/CDI, provide "
                                        + "a bean of type CollaborationEngineConfiguration. "
                                        + "More info in Vaadin documentation.");
                    } else {
                        LOGGER.warn(
                                "Collaboration Engine is used in development/trial mode. "
                                        + "Note that in order to make a production build, "
                                        + "you need to obtain a license from Vaadin and configure the '"
                                        + FileHandler.DATA_DIR_PUBLIC_PROPERTY
                                        + "' property. You also need to provide a configuration object "
                                        + "by using the static CollaborationEngine.configure() method in "
                                        + "a VaadinServiceInitListener or, if using Spring/CDI, provide "
                                        + "a bean of type CollaborationEngineConfiguration. "
                                        + "More info in Vaadin documentation.");
                        return CollaborationEngine.configure(vaadinService,
                                new CollaborationEngineConfiguration(e -> {
                                    throw new IllegalStateException(
                                            "License event handler was called in dev mode. "
                                                    + "This should not happen.");
                                }));
                    }
                });
    }

    /**
     * Sets the configuration for the Collaboration Engine associated with the
     * given Vaadin service. This configuration is required when running in
     * production mode. It can be set only once.
     * <p>
     * You should register a {@link VaadinServiceInitListener} where you call
     * this method with the service returned by
     * {@link ServiceInitEvent#getSource()}.
     *
     * @param vaadinService
     *            the Vaadin service for which to configure the Collaboration
     *            Engine
     * @param configuration
     *            the configuration to provide for the Collaboration Engine
     * @return the configured Collaboration Engine instance
     */
    public static CollaborationEngine configure(VaadinService vaadinService,
            CollaborationEngineConfiguration configuration) {
        return configure(vaadinService, configuration,
                new CollaborationEngine());
    }

    static CollaborationEngine configure(VaadinService vaadinService,
            CollaborationEngineConfiguration configuration,
            CollaborationEngine ce) {
        Objects.requireNonNull(vaadinService, "VaadinService cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");
        if (vaadinService.getContext()
                .getAttribute(CollaborationEngine.class) != null) {
            throw new IllegalStateException(
                    "Collaboration Engine has been already configured for the provided VaadinService. "
                            + "The configuration can be provided only once.");
        }
        configuration.setVaadinService(vaadinService);
        ce.configuration = configuration;

        vaadinService.getContext().setAttribute(CollaborationEngine.class, ce);
        if (!vaadinService.getDeploymentConfiguration().isProductionMode()) {
            LicenseChecker.checkLicense(COLLABORATION_ENGINE_NAME,
                    COLLABORATION_ENGINE_VERSION);
        }
        return ce;
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

        if (configuration == null) {
            throw new IllegalStateException(
                    "Collaboration Engine is missing required configuration "
                            + "that should be provided by a VaadinServiceInitListener. "
                            + "Collaboration Engine is supported only in a Vaadin application, "
                            + "where VaadinService initialization is expected to happen before usage.");
        }

        ensureConfigAndLicenseHandlerInitialization();
        if (configuration.isLicenseCheckingEnabled()) {
            boolean hasSeat = licenseHandler.registerUser(localUser.getId());

            if (!hasSeat) {
                // User quota exceeded, don't open the connection.
                LOGGER.warn(
                        "Access for user '{}' was denied. The license may have "
                                + "expired or the user quota may have exceeded, check the "
                                + "license events handled by your LicenseEventHandler for "
                                + "more details.",
                        localUser.getId());
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
        final boolean hasAccess = configuration.isLicenseCheckingEnabled()
                ? licenseHandler.registerUser(user.getId())
                : true;

        AccessResponse response = new AccessResponse(hasAccess);
        context.dispatchAction(() -> requestCallback.accept(response));
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

    CollaborationEngineConfiguration getConfiguration() {
        return configuration;
    }

    Clock getClock() {
        return clock;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private synchronized void ensureConfigAndLicenseHandlerInitialization() {
        if (licenseHandler == null) {
            // Will throw if config is invalid
            licenseHandler = new LicenseHandler(this);
        }
    }
}
