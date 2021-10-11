/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

/**
 * The common abstract superclass of Collaboration Managers.
 *
 * @author Vaadin Ltd
 */
public abstract class AbstractCollaborationManager {

    /**
     * The callback executed when the manager is activated, i.e. when the
     * connection to the topic is established.
     *
     * @see AbstractCollaborationManager#setActivationHandler(ActivationHandler)
     */
    @FunctionalInterface
    public interface ActivationHandler extends Serializable {

        /**
         * The method executed when the manager is activated. The method might
         * return a callback which will be invoked when the manager is
         * deactivated, i.e. the connection to the topic is closed. This
         * callback can be used to clean-up resources used during activation.
         *
         * @return a callback which will be executed when the manager is
         *         deactivated, or {@code null} if not needed
         */
        public Registration onActivation();
    }

    private final CollaborationEngine collaborationEngine;

    private final UserInfo localUser;

    private final String topicId;

    private Registration topicRegistration;

    private ActivationHandler activationHandler;

    private Registration deactivationHandler;

    private boolean active;

    /**
     * Constructs a new manager instance.
     *
     * @param localUser
     *            the local user, not {@code null}
     * @param topicId
     *            the topic id, not {@code null}
     * @param collaborationEngine
     *            the Collaboration Engine instance, not {@code null}
     */
    protected AbstractCollaborationManager(UserInfo localUser, String topicId,
            CollaborationEngine collaborationEngine) {
        this.localUser = Objects.requireNonNull(localUser);
        this.topicId = Objects.requireNonNull(topicId);
        this.collaborationEngine = Objects.requireNonNull(collaborationEngine);
    }

    /**
     * Opens a connection to the topic of this manager using the provided
     * context and activation callback.
     *
     * @param context
     *            the connection context, not {@code null}
     * @param connectionActivationCallback
     *            the callback to be executed when a connection is activated,
     *            not {@code null}
     */
    protected void openTopicConnection(ConnectionContext context,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        if (topicRegistration != null) {
            topicRegistration.remove();
        }
        topicRegistration = collaborationEngine.openTopicConnection(context,
                topicId, localUser, connection -> {
                    active = true;
                    if (activationHandler != null) {
                        deactivationHandler = activationHandler.onActivation();
                    }
                    Registration callbackRegistration = connectionActivationCallback
                            .apply(connection);
                    return callbackRegistration != null
                            ? Registration.combine(callbackRegistration,
                                    this::onTopicRegistrationRemove)
                            : this::onTopicRegistrationRemove;
                });
    }

    /**
     * Sets an handler that will be executed when the manager is activated, i.e.
     * the connection to the topic is established.
     *
     * @param handler
     *            the handler, {@code null} to remove an existing handler
     */
    public void setActivationHandler(ActivationHandler handler) {
        if (deactivationHandler != null) {
            deactivationHandler.remove();
            deactivationHandler = null;
        }
        activationHandler = handler;
        if (active && activationHandler != null) {
            deactivationHandler = activationHandler.onActivation();
        }
    }

    private void onTopicRegistrationRemove() {
        if (deactivationHandler != null) {
            deactivationHandler.remove();
        }
        active = false;
    }

    /**
     * Gets the {@link CollaborationEngine} of this manager.
     *
     * @return the {@link CollaborationEngine}, not {@code null}
     */
    protected CollaborationEngine getCollaborationEngine() {
        return collaborationEngine;
    }

    /**
     * Gets the local user of this manager.
     *
     * @return the local user, not {@code null}
     */
    public UserInfo getLocalUser() {
        return localUser;
    }

    /**
     * Gets the topic id of this manager.
     *
     * @return the topic id, not {@code null}
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     * Closes the manager connection to the topic. This is typically handled
     * automatically by the connection-context and in most cases is not needed
     * to be done explicitly, e.g. with {@link ComponentConnectionContext} when
     * the component is detached.
     */
    public void close() {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
        }
    }
}
