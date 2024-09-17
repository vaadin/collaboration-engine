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
package com.vaadin.collaborationengine.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.vaadin.collaborationengine.Backend;
import com.vaadin.collaborationengine.Backend.EventLog;
import com.vaadin.collaborationengine.MembershipEvent;
import com.vaadin.collaborationengine.MembershipEvent.MembershipEventType;
import com.vaadin.collaborationengine.MembershipListener;
import com.vaadin.flow.shared.Registration;

public class TestBackendFactory {

    private List<IdAndEvent> events = new ArrayList<>();

    private Map<String, EventLog> eventLogs = new HashMap<>();

    private Set<Backend> nodes = new LinkedHashSet<>();

    private List<MembershipListener> membershipListeners = new ArrayList<>();

    private Map<String, Backend.Snapshot> snapshots = new HashMap<>();

    public TestBackend createBackend() {
        return new TestBackend();
    }

    public void join(Backend node) {
        nodes.add(node);
        membershipListeners.forEach(listener -> listener.handleMembershipEvent(
                new MembershipEvent(MembershipEventType.JOIN, node.getNodeId(),
                        node.getCollaborationEngine())));
    }

    public void leave(Backend node) {
        nodes.remove(node);
        membershipListeners.forEach(listener -> listener.handleMembershipEvent(
                new MembershipEvent(MembershipEventType.LEAVE, node.getNodeId(),
                        node.getCollaborationEngine())));
    }

    private static class IdAndEvent {
        private final UUID id;
        private final String event;

        private IdAndEvent(UUID id, String event) {
            this.id = id;
            this.event = event;
        }
    }

    private class TestEventLog implements EventLog {

        private final List<IdAndEvent> events;

        private final List<BiConsumer<UUID, String>> consumers = new ArrayList<>();

        private TestEventLog(List<IdAndEvent> events) {
            this.events = events;
        }

        @Override
        public void submitEvent(UUID trackingId, String eventPayload) {
            events.add(new IdAndEvent(trackingId, eventPayload));
            consumers.forEach(
                    consumer -> consumer.accept(trackingId, eventPayload));
        }

        @Override
        public Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> consumer)
                throws Backend.EventIdNotFoundException {
            Predicate<IdAndEvent> filter = e -> true;
            if (newerThan != null) {
                Optional<IdAndEvent> newerThanIdAndEvent = events.stream()
                        .filter(item -> newerThan.equals(item.id)).findFirst();
                if (newerThanIdAndEvent.isEmpty()) {
                    throw new Backend.EventIdNotFoundException(
                            "newerThan doesn't " + "exist in the log.");
                }
                filter = new Predicate<>() {
                    boolean found;

                    @Override
                    public boolean test(IdAndEvent idAndEvent) {
                        boolean result = found;
                        found = found || newerThan.equals(idAndEvent.id);
                        return result;
                    }
                };
            }
            events.stream().filter(filter)
                    .forEach(e -> consumer.accept(e.id, e.event));
            consumers.add(consumer);
            return () -> consumers.remove(consumer);
        }

        @Override
        public void truncate(UUID olderThan) {
            Predicate<IdAndEvent> filter = e -> true;
            if (olderThan != null) {
                Optional<IdAndEvent> olderThanIdAndEvent = events.stream()
                        .filter(item -> olderThan.equals(item.id)).findFirst();
                if (olderThanIdAndEvent.isEmpty()) {
                    // NOOP
                    return;
                }
                filter = new Predicate<>() {
                    boolean found;

                    @Override
                    public boolean test(IdAndEvent idAndEvent) {
                        found = found || olderThan.equals(idAndEvent.id);
                        return !found;
                    }
                };
            }
            events.removeIf(filter);
        }
    }

    public class TestBackend extends Backend {

        private final UUID id = UUID.randomUUID();

        @Override
        public EventLog openEventLog(String logId) {
            return eventLogs.computeIfAbsent(logId,
                    id -> new TestEventLog(events));
        }

        @Override
        public Registration addMembershipListener(
                MembershipListener membershipListener) {
            nodes.forEach(node -> membershipListener.handleMembershipEvent(
                    new MembershipEvent(MembershipEventType.JOIN,
                            node.getNodeId(), getCollaborationEngine())));
            membershipListeners.add(membershipListener);
            return () -> membershipListeners.remove(membershipListener);
        }

        @Override
        public UUID getNodeId() {
            return id;
        }

        @Override
        public CompletableFuture<Snapshot> loadLatestSnapshot(String name) {
            Objects.requireNonNull(name, "Name cannot be null");
            return CompletableFuture.completedFuture(snapshots.get(name));
        }

        @Override
        public CompletableFuture<Void> replaceSnapshot(String name,
                UUID expectedId, UUID newId, String payload) {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(newId, "New ID cannot be null");

            Snapshot currentSnapshot = snapshots.computeIfAbsent(name,
                    k -> new Snapshot(null, null));
            if (Objects.equals(expectedId, currentSnapshot.getId())) {
                Snapshot idAndPayload = new Snapshot(newId, payload);
                snapshots.put(name, idAndPayload);
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
