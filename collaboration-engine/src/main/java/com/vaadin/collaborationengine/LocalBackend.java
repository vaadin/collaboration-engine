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
