/*
 * Copyright 2020-2022 Vaadin Ltd.
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

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.flow.shared.Registration;

/**
 * A simple backend implementation that only distributes events locally and
 * assumes that there is no previous history for event logs.
 *
 * @author Vaadin Ltd
 */
public class LocalBackend implements Backend {
    private static class LocalEventLog implements EventLog {
        private final String topicId;
        private BiConsumer<UUID, String> consumer;

        private LocalEventLog(String topicId) {
            this.topicId = topicId;
        }

        @Override
        public Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> consumer) {
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
    }

    private final UUID id = UUID.randomUUID();

    @Override
    public EventLog openEventLog(String topicId) {
        return new LocalEventLog(topicId);
    }

    @Override
    public EventLog getMembershipEventLog() {
        return new EventLog() {

            @Override
            public Registration subscribe(UUID newerThan,
                    BiConsumer<UUID, String> eventConsumer) {
                assert newerThan == null;
                ObjectNode event = JsonUtil.createNodeJoin(id);
                String payload = JsonUtil.toString(event);
                eventConsumer.accept(UUID.randomUUID(), payload);
                return () -> {
                    // NOOP
                };
            }

            @Override
            public void submitEvent(UUID trackingId, String eventPayload) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public UUID getNodeId() {
        return id;
    }

    @Override
    public CompletableFuture<String> loadLatestSnapshot(String name) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void submitSnapshot(String name, String snapshot) {
        // NOOP
    }
}
