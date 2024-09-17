/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
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
     * This exception is thrown by the {@link EventLog::subscribe()} method if
     * the provided {@code UUID} does not exist in the log.
     */
    public static class EventIdNotFoundException extends Exception {
        public EventIdNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * The {@code Snapshot} class is used to submit and retrieve a snapshot
     * payload using the {@link Backend::replaceSnapshot()} and
     * {@link Backend::loadLatestSnapshot()} methods. A UUID is provided to
     * uniquely identify potentially identical payloads.
     */
    public static class Snapshot implements Serializable {
        private final UUID id;
        private final String payload;

        public Snapshot(UUID id, String payload) {
            this.id = id;
            this.payload = payload;
        }

        public UUID getId() {
            return id;
        }

        public String getPayload() {
            return payload;
        }
    }

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
         * <p>
         * If the provided {@code newerThan} ID is not null and is not found in
         * the event log (perhaps due to truncation), a
         * {@code EventIdNotFoundException} should be thrown. When the exception
         * is caught by the code calling this method, it may want to re-attempt
         * the subscription with another ID. A Collaboration Engine topic, for
         * example, will try to load the latest snapshot and subscribe from the
         * ID associated with it.
         *
         * @param newerThan
         *            if not <code>null</code>, only events after the event with
         *            the provided UUID will be considered.
         * @param eventConsumer
         *            a consumer that should receive all events, not
         *            <code>null</code>
         * @return a registration to remove the event consumer, not
         *         <code>null</code>
         * @throws EventIdNotFoundException
         *             when the provided UUID does not exist in the event log.
         */
        Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> eventConsumer)
                throws EventIdNotFoundException;

        /**
         * Removes all events in the log before the given id. If a {@code null}
         * id is passed, then all events are removed.
         *
         * @param olderThan
         *            the oldest UUID to retain
         */
        void truncate(UUID olderThan);
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
     * a snapshot see {@link #replaceSnapshot(String, UUID, UUID, String)}.
     *
     * @param name
     *            the name identifying the data, not <code>null</code>
     * @return a completable future resolved with the UUID and snapshot, not
     *         <code>null</code>
     */
    public abstract CompletableFuture<Snapshot> loadLatestSnapshot(String name);

    /**
     * Submits a snapshot payload of data identified by the given name. The
     * latest submitted snapshot for that name can be loaded with
     * {@link #loadLatestSnapshot(String)}.
     *
     * @param name
     *            the name identifying the date, not <code>null</code>
     * @param expectedId
     *            the unique ID of the expected current snapshot
     * @param newId
     *            the unique ID that the new snapshot will be stored with, not
     *            {@code null}
     * @param payload
     *            the snapshot payload, not <code>null</code>
     * @return a completable future that will be resolved when the operation
     *         completes, not {@code null}
     */
    public abstract CompletableFuture<Void> replaceSnapshot(String name,
            UUID expectedId, UUID newId, String payload);
}
