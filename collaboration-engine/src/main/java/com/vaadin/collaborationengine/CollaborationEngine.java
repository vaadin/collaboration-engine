/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import javax.servlet.ServletContext;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.collaborationengine.Backend.EventLog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
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
 * Use {@link #getInstance()} to get a reference to the singleton object in
 * cases where Vaadin's thread locals are defined (such as in UI code invoked by
 * the framework). In other circumstances, an instance can be found as an
 * attribute in the runtime context (typically {@link ServletContext}) using the
 * fully qualified class name of this class as the attribute name. That instance
 * will only be available after explicitly calling
 * {@link #configure(VaadinService, CollaborationEngineConfiguration)} during
 * startup or calling {@link #getInstance()} at least once.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@NpmPackage(value = "@vaadin/field-highlighter", version = "23.2.0-alpha2")
@JsModule("@vaadin/field-highlighter/src/vaadin-field-highlighter.js")
public class CollaborationEngine {

    private static class TopicAndEventLog {
        private final Topic topic;
        private final EventLog eventLog;

        public TopicAndEventLog(Topic topic, EventLog eventLog) {
            this.topic = topic;
            this.eventLog = eventLog;
        }
    }

    static final Logger LOGGER = LoggerFactory
            .getLogger(CollaborationEngine.class);

    static final String COLLABORATION_ENGINE_NAME = "vaadin-collaboration-engine";
    static final String COLLABORATION_ENGINE_VERSION = "5.2";

    static final int USER_COLOR_COUNT = 7;

    private Map<String, TopicAndEventLog> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();
    private Map<String, Integer> activeTopicsCount = new ConcurrentHashMap<>();
    private final Set<TopicConnectionRegistration> registrations = ConcurrentHashMap
            .newKeySet();

    private LicenseHandler licenseHandler;

    private CollaborationEngineConfiguration configuration;

    private final TopicActivationHandler topicActivationHandler;

    private Clock clock = Clock.systemUTC();

    private ExecutorService executorService;

    private VaadinService vaadinService;

    private SystemConnectionContext systemContext;

    private final AtomicBoolean active = new AtomicBoolean(true);

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
        if (isActive) {
            activeTopicsCount.putIfAbsent(topicId, 0);
        }
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
     * <p>
     * Situations without a current {@code VaadinService} can also find the
     * corresponding instance by looking it up from the runtime context (such as
     * {@link ServletContext}) using {@code CollaborationEngine.class.getName()}
     * as the attribute name. That instance will only be available after
     * explicitly calling
     * {@link #configure(VaadinService, CollaborationEngineConfiguration)}
     * during startup or calling {@link #getInstance()} at least once.
     *
     * @return the {@link CollaborationEngine} instance
     *
     * @since 1.0
     */
    public static CollaborationEngine getInstance() {
        VaadinService service = VaadinService.getCurrent();
        if (service == null) {
            throw new IllegalStateException(
                    "Cannot get the current CollaborationEngine instance when there is no current VaadinService instance.");
        }
        return getInstance(service);
    }

    /**
     * Gets the {@link CollaborationEngine} instance from the provided
     * {@link VaadinService}.
     *
     * @return the {@link CollaborationEngine} instance
     *
     * @since 3.0
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
                                        + CollaborationEngineConfiguration.DATA_DIR_PUBLIC_PROPERTY
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
                                }), new CollaborationEngine(), false);
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
     *
     * @since 3.0
     */
    public static CollaborationEngine configure(VaadinService vaadinService,
            CollaborationEngineConfiguration configuration) {
        return configure(vaadinService, configuration,
                new CollaborationEngine(), true);
    }

    static CollaborationEngine configure(VaadinService vaadinService,
            CollaborationEngineConfiguration configuration,
            CollaborationEngine ce, boolean storeInService) {
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
        ce.vaadinService = vaadinService;
        ce.systemContext = new SystemConnectionContext(ce);

        configuration.getBackend().setCollaborationEngine(ce);

        ExecutorService executorService = ce.configuration.getExecutorService();
        final boolean useManagedExecutorService = executorService == null;
        if (useManagedExecutorService) {
            ce.executorService = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
        } else {
            ce.executorService = executorService;
        }

        vaadinService.addServiceDestroyListener(event -> {
            ce.active.set(false);
            ce.clearConnections();
            if (useManagedExecutorService) {
                LOGGER.info("Shutting down thread pool");
                ce.executorService.shutdown();
            }
        });
        if (storeInService) {
            // Avoid storing from inside computeIfAbsent
            vaadinService.getContext().setAttribute(CollaborationEngine.class,
                    ce);
        }
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
     *
     * @since 1.0
     */
    public TopicConnectionRegistration openTopicConnection(Component component,
            String topicId, UserInfo localUser,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(component, "Connection context can't be null");
        ConnectionContext context = new ComponentConnectionContext(component);
        return openTopicConnection(context, topicId, localUser,
                connectionActivationCallback);
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    private void clearConnections() {
        LOGGER.info("Deactivating connections before shutdown");
        List<CompletableFuture<Void>> futures = new ArrayList<>(
                registrations.size());
        LOGGER.debug("Closing {} connections", registrations.size());
        for (TopicConnectionRegistration registration : registrations) {
            registration.remove();
            registration.getPendingFuture().ifPresent(futures::add);
        }

        final int timeoutInSeconds = 1;
        final Instant end = Instant.now().plus(timeoutInSeconds,
                ChronoUnit.SECONDS);
        LOGGER.debug("Waiting for {} asynchronous tasks to complete",
                futures.size());
        for (CompletableFuture<Void> future : futures) {
            if (waitForFuture(future, end, timeoutInSeconds)) {
                break;
            }
        }
        LOGGER.debug("Finished waiting for asynchronous tasks");
        registrations.clear();
    }

    // Waits for the future and returns true if there is a timeout
    private static boolean waitForFuture(CompletableFuture<Void> future,
            Instant end, long timeoutInSeconds) {
        boolean timeout = false;
        final String timeoutMessage = "Timeout reached when waiting for "
                + "topic connections to be closed";
        try {
            LOGGER.trace("Waiting for future to complete");
            future.get(timeoutInSeconds, TimeUnit.SECONDS);
            LOGGER.trace("Future completed successfully");
            if (Instant.now().isAfter(end)) {
                LOGGER.warn(timeoutMessage);
                timeout = true;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.info("Exception caught when closing topic connections", e);
        } catch (TimeoutException e) {
            LOGGER.warn(timeoutMessage, e);
            timeout = true;
        }
        return timeout;
    }

    private void assertConfigured() {
        if (configuration == null) {
            throw new IllegalStateException(
                    "Collaboration Engine is missing required configuration "
                            + "that should be provided by a VaadinServiceInitListener. "
                            + "Collaboration Engine is supported only in a Vaadin application, "
                            + "where VaadinService initialization is expected to happen before usage.");
        }
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
     *
     * @since 1.0
     */
    public TopicConnectionRegistration openTopicConnection(
            ConnectionContext context, String topicId, UserInfo localUser,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(context, "Connection context can't be null");
        Objects.requireNonNull(topicId, "Topic id can't be null");
        Objects.requireNonNull(localUser, "User can't be null");
        Objects.requireNonNull(connectionActivationCallback,
                "Callback for connection activation can't be null");

        assertConfigured();

        ensureConfigAndLicenseHandlerInitialization();
        if (!active.get()) {
            LOGGER.info("Tried to open a connection to a closed collaboration"
                    + " engine instance");
            return createFailedTopicConnectionRegistration(context);
        }

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
                return createFailedTopicConnectionRegistration(context);
            }
        }

        TopicAndEventLog topicAndConnection = topics.computeIfAbsent(topicId,
                this::createTopicAndEventLog);
        BiConsumer<UUID, ObjectNode> distributor = (id,
                node) -> topicAndConnection.eventLog.submitEvent(id,
                        JsonUtil.toString(node));
        TopicConnection connection = new TopicConnection(this, context,
                topicAndConnection.topic, distributor, localUser,
                isActive -> updateTopicActivation(topicId, isActive),
                connectionActivationCallback);

        TopicConnectionRegistration registration = new TopicConnectionRegistration(
                connection, context, getExecutorService(),
                registrations::remove);
        registrations.add(registration);
        if (!active.get()) {
            registration.remove();
            LOGGER.info("Tried to open a connection to a closed collaboration"
                    + " engine instance");
            return createFailedTopicConnectionRegistration(context);
        }
        return registration;
    }

    private TopicConnectionRegistration createFailedTopicConnectionRegistration(
            ConnectionContext context) {
        return new TopicConnectionRegistration(null, context,
                getExecutorService(), r -> {
                    // No op
                });
    }

    private TopicAndEventLog createTopicAndEventLog(String id) {
        EventLog eventLog = configuration.getBackend().openEventLog(id);

        Topic topic = new Topic(id, this, eventLog);
        return new TopicAndEventLog(topic, eventLog);
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
     * To avoid calling this method multiple times per user, it is suggested to
     * cache the result during the login process (e.g. in the session).
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
     *
     * @since 3.0
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
     * To avoid calling this method multiple times per user, it is suggested to
     * cache the result during the login process (e.g. in the session).
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
     *
     * @since 3.0
     */
    public void requestAccess(ConnectionContext context, UserInfo user,
            Consumer<AccessResponse> requestCallback) {
        Objects.requireNonNull(context, "ConnectionContext cannot be null");
        Objects.requireNonNull(user, "UserInfo cannot be null");
        Objects.requireNonNull(requestCallback,
                "AccessResponse cannot be null");

        // Will handle remote connection here
        context.init(new SingleUseActivationHandler(actionDispatcher -> {
            ensureConfigAndLicenseHandlerInitialization();
            final boolean hasAccess = !configuration.isLicenseCheckingEnabled()
                    || licenseHandler.registerUser(user.getId());

            AccessResponse response = new AccessResponse(hasAccess);
            actionDispatcher
                    .dispatchAction(() -> requestCallback.accept(response));
        }), getExecutorService());
    }

    /**
     * Gets the color index of a user if different than -1, or let Collaboration
     * Engine provide one. If the color index for a user id does not exist yet,
     * it's created on demand based on the user id.
     *
     * @param userInfo
     *            user info
     * @return the color index
     *
     * @since 3.1
     */
    public int getUserColorIndex(UserInfo userInfo) {
        int currentColorIndex = userInfo.getColorIndex();
        if (currentColorIndex != -1) {
            return currentColorIndex;
        }
        String userId = userInfo.getId();
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

    synchronized void ensureConfigAndLicenseHandlerInitialization() {
        if (licenseHandler == null) {
            // Will throw if config is invalid
            licenseHandler = new LicenseHandler(this);
        }
    }

    // For testing
    Topic getTopic(String topicId) {
        return topics.get(topicId).topic;
    }

    VaadinService getVaadinService() {
        return vaadinService;
    }

    /**
     * Gets a system connection context for this collaboration engine instance.
     * The system connection context can be used when Collaboration Engine is
     * used in situations that aren't directly associated with a UI, such as
     * from a background thread or when integrating with external services.
     *
     * @return a system connection context instance, not <code>null</code>
     */
    public SystemConnectionContext getSystemContext() {
        assertConfigured();

        return systemContext;
    }
}
