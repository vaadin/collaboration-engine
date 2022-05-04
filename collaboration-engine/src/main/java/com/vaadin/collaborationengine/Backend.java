/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.vaadin.flow.shared.Registration;

/**
 * The interface between a cluster backend and Collaboration Engine.
 *
 * @author Vaadin Ltd
 */
public abstract class Backend {

    /**
     * A strictly ordered log of submitted events.
     */
    public interface EventLog {
        /**
         * Submits an event through the backend to all subscribers. The tracking
         * id needs to be delivered to active subscribers but it is not
         * necessary to preserve it for replaying old events to new subscribers.
         *
         * @param trackingId
         *            the tracking id of this event, not <code>null</code>
         * @param eventPayload
         *            the payload representing the event, not <code>null</code>
         */
        void submitEvent(UUID trackingId, String eventPayload);

        /**
         * Adds a subscriber to receive all past and future events for this
         * event log. A newly added subscriber should initially receive all
         * previous events in the log based on their original order so that it
         * can catch up with the latest state. New events should be delivered
         * (in order) only after all previous events have been delivered. It is
         * not allowed to invoke the consumer again until the previous
         * invocation has returned.
         *
         *
         * @param newerThan
         *            if not <code>null</code>, only events after the event with
         *            the provided UUID will be considered.
         * @param eventConsumer
         *            a consumer that should receive all events, not
         *            <code>null</code>
         * @return a registration to remove the event consumer, not
         *         <code>null</code>
         */
        Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> eventConsumer);
    }

    private CollaborationEngine collaborationEngine;

    /**
     * Gets the {@link CollaborationEngine} used by this backend.
     *
     * @return the {@link CollaborationEngine} instance, or <code>null</code> if
     *         not set
     */
    public final CollaborationEngine getCollaborationEngine() {
        return collaborationEngine;
    }

    /**
     * Sets the {@link CollaborationEngine} instance for this backend.
     *
     * @param collaborationEngine
     *            the {@link CollaborationEngine} instance, not
     *            <code>null</code>
     */
    public final void setCollaborationEngine(
            CollaborationEngine collaborationEngine) {
        this.collaborationEngine = Objects.requireNonNull(collaborationEngine);
    }

    /**
     * Opens an event log with the given id. The returned object can be used to
     * capture any common state related to this particular event log. An actual
     * underlying connection is not needed until
     * {@link EventLog#subscribe(UUID, BiConsumer)}} is invoked, but it is still
     * recommended to make this method fail fast in case it would not be
     * possible to open an actual underlying connection later.
     *
     * @param logId
     *            the id of the event log to open, not <code>null</code>
     * @return an object representing the event log, not <code>null</code>
     */
    public abstract EventLog openEventLog(String logId);

    /**
     * Adds a listener of membership events. The listener will be notified of
     * events dispatched when a node joins or leaves the backend.
     *
     * @param membershipListener
     *            the listener, not <code>null</code>
     * @return a registration that can be used to remove the listener, not
     *         <code>null</code>
     */
    public abstract Registration addMembershipListener(
            MembershipListener membershipListener);

    /**
     * Gets the unique identifier of this backend node.
     *
     * @return the node id, not <code>null</code>
     */
    public abstract UUID getNodeId();

    /**
     * Loads the latest snapshot of data identified by the given name. To submit
     * a snapshot see {@link #submitSnapshot(String, String)}.
     *
     * @param name
     *            the name identifying the data, not <code>null</code>
     * @return a completable future resolved with the snapshot, not
     *         <code>null</code>
     */
    public abstract CompletableFuture<String> loadLatestSnapshot(String name);

    /**
     * Submits a snapshots of data identifies by the given name. The latest
     * submitted snapshot for that name can be loaded with
     * {@link #loadLatestSnapshot(String)}.
     *
     * @param name
     *            the name identifying the date, not <code>null</code>
     * @param snapshot
     *            the snapshot, not <code>null</code>
     */
    public abstract void submitSnapshot(String name, String snapshot);
}
