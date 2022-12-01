/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.vaadin.collaborationengine.MembershipEvent.MembershipEventType;
import com.vaadin.flow.shared.Registration;

/**
 * A simple backend implementation that only distributes events locally and
 * assumes that there is no previous history for event logs.
 *
 * @author Vaadin Ltd
 */
public class LocalBackend extends Backend {
    private static class LocalEventLog implements EventLog {
        private final String topicId;
        private BiConsumer<UUID, String> consumer;

        private LocalEventLog(String topicId) {
            this.topicId = topicId;
        }

        @Override
        public Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> consumer)
                throws Backend.EventIdNotFoundException {
            if (this.consumer != null) {
                throw new IllegalStateException(
                        "Already subscribed to " + topicId);
            }
            this.consumer = consumer;
            return () -> this.consumer = null;
        }

        @Override
        public void submitEvent(UUID trackingId, String event) {
            if (consumer == null) {
                throw new IllegalStateException("Not subscribed to " + topicId);
            }
            consumer.accept(trackingId, event);
        }

        @Override
        public void truncate(UUID olderThan) {
            // NOOP
        }
    }

    private final UUID id = UUID.randomUUID();

    @Override
    public EventLog openEventLog(String topicId) {
        return new LocalEventLog(topicId);
    }

    @Override
    public Registration addMembershipListener(
            MembershipListener membershipListener) {
        membershipListener.handleMembershipEvent(new MembershipEvent(
                MembershipEventType.JOIN, id, getCollaborationEngine()));
        return () -> {
            // NOOP
        };
    }

    @Override
    public UUID getNodeId() {
        return id;
    }

    @Override
    public CompletableFuture<Snapshot> loadLatestSnapshot(String name) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> replaceSnapshot(String name, UUID expectedId,
            UUID newId, String payload) {
        return CompletableFuture.completedFuture(null);
    }
}
