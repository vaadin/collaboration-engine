/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.flow.shared.Registration;

/**
 * The interface between a cluster backend and Collaboration Engine.
 *
 * @author Vaadin Ltd
 */
public interface Backend {
    /**
     * A strictly ordered log of submitted events.
     */
    interface EventLog {
        /**
         * Submits an event through the backend to all subscribers. The tracking
         * id needs to be delivered to active subscribers but it is not
         * necessary to preserve it for replaying old events to new subscribers.
         *
         * @param trackingId
         *            the tracking id of this event, not <code>null</code>
         * @param eventPayload
         *            the JSON object representing the event, not
         *            <code>null</code>
         */
        void submitEvent(UUID trackingId, ObjectNode eventPayload);

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
                BiConsumer<UUID, ObjectNode> eventConsumer);
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
    EventLog openEventLog(String logId);

    /**
     * Get the event log of this backend node membership events. The returned
     * object can be used to subscribe to events dispatched when a node joins or
     * leaves the backend.
     *
     * @return the object representing the membership event log, not
     *         <code>null</code>
     */
    EventLog getMembershipEventLog();

    /**
     * Gets the unique identifier of this backend node.
     *
     * @return the node id, not <code>null</code>
     */
    UUID getNodeId();

    CompletableFuture<ObjectNode> loadLatestSnapshot(String name);

    void submitSnapshot(String name, ObjectNode snapshot);
}
