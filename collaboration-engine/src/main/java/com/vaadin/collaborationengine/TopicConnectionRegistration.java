/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * A registration for configuring or removing a topic connection that is opened
 * with Collaboration Engine.
 *
 * @see CollaborationEngine#openTopicConnection(ConnectionContext, String,
 *      UserInfo, SerializableFunction)
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public class TopicConnectionRegistration implements Registration {

    /**
     * An action for handling a failed topic connection.
     *
     * @see TopicConnectionRegistration#onConnectionFailed(ConnectionFailedAction)
     *
     * @author Vaadin Ltd
     */
    @FunctionalInterface
    public interface ConnectionFailedAction {
        /**
         * Handles a failed topic connection.
         *
         * @param event
         *            the connection failed event
         */
        void onConnectionFailed(ConnectionFailedEvent event);
    }

    /**
     * An event that is fired when the topic connection fails.
     *
     * @see TopicConnectionRegistration#onConnectionFailed(ConnectionFailedAction)
     *
     * @author Vaadin Ltd
     */
    public static class ConnectionFailedEvent extends EventObject {
        ConnectionFailedEvent(TopicConnectionRegistration source) {
            super(source);
        }

        @Override
        public TopicConnectionRegistration getSource() {
            return (TopicConnectionRegistration) super.getSource();
        }
    }

    private final AtomicReference<TopicConnection> topicConnectionReference;
    private ConnectionContext connectionContext;
    private Executor executor;
    private CompletableFuture<Void> pendingFuture;
    private final Consumer<TopicConnectionRegistration> afterDisconnection;

    TopicConnectionRegistration(TopicConnection topicConnection,
            ConnectionContext connectionContext, Executor executor,
            Consumer<TopicConnectionRegistration> afterDisconnection) {
        this.topicConnectionReference = new AtomicReference<>(topicConnection);
        this.connectionContext = connectionContext;
        this.executor = executor;
        this.afterDisconnection = afterDisconnection;
    }

    /**
     * Closes the topic connection. NO-OP if the connection has failed.
     */
    @Override
    public void remove() {
        TopicConnection topicConnection = topicConnectionReference
                .getAndSet(null);
        if (topicConnection != null) {
            pendingFuture = topicConnection.deactivateAndClose();
            pendingFuture.thenRun(() -> {
                this.pendingFuture = null;
                this.afterDisconnection.accept(this);
            });
        }
        connectionContext = null;
        executor = null;
    }

    Optional<CompletableFuture<Void>> getPendingFuture() {
        return Optional.ofNullable(pendingFuture);
    }

    /**
     * Adds an action to be executed if the topic connection fails. The
     * connection can fail in production mode, if your Collaboration Engine
     * license has expired, or if the number of unique monthly end users has
     * exceeded the quota in your license.
     * <p>
     * If the connection has already failed when calling this method, the action
     * runs immediately.
     * <p>
     * The action is executed through
     * {@link ActionDispatcher#dispatchAction(Command)} of the connection
     * context that was used to open the connection.
     *
     * @param connectionFailedAction
     *            the action to handle topic connection failure, not
     *            {@code null}
     */
    public void onConnectionFailed(
            ConnectionFailedAction connectionFailedAction) {
        Objects.requireNonNull(connectionFailedAction,
                "The connection failed action can't be null");
        /*
         * With the embedded CE, we always know already at this point whether
         * the connection has failed or not, so there's no need to store the
         * action for later. This needs to be updated when we have the
         * standalone CE server.
         */
        if (topicConnectionReference.get() == null) {
            connectionContext
                    .init(new SingleUseActivationHandler(actionDispatcher -> {
                        ConnectionFailedEvent event = new ConnectionFailedEvent(
                                this);
                        actionDispatcher
                                .dispatchAction(() -> connectionFailedAction
                                        .onConnectionFailed(event));
                    }), executor);
        }
    }

}
