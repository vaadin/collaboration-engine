package com.vaadin.collaborationengine.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.collaborationengine.Backend;
import com.vaadin.collaborationengine.Backend.EventLog;
import com.vaadin.collaborationengine.JsonUtil;
import com.vaadin.flow.shared.Registration;

public class TestBackendFactory {

    private List<IdAndEvent> events = new ArrayList<>();

    private Map<String, EventLog> eventLogs = new HashMap<>();

    private List<IdAndEvent> membershipEvents = new ArrayList<>();

    private EventLog membershipEventLog = new TestEventLog(membershipEvents);

    private Map<String, ObjectNode> snapshots = new HashMap<>();

    public TestBackend createBackend() {
        return new TestBackend();
    }

    public void join(Backend node) {
        ObjectNode event = JsonUtil.createNodeJoin(node.getNodeId());
        membershipEventLog.submitEvent(UUID.randomUUID(), event);
    };

    public void leave(Backend node) {
        ObjectNode event = JsonUtil.createNodeLeave(node.getNodeId());
        membershipEventLog.submitEvent(UUID.randomUUID(), event);
    }

    private static class IdAndEvent {
        private final UUID id;
        private final ObjectNode event;

        private IdAndEvent(UUID id, ObjectNode event) {
            this.id = id;
            this.event = event;
        }
    }

    private class TestEventLog implements EventLog {

        private final List<IdAndEvent> events;

        private final List<BiConsumer<UUID, ObjectNode>> consumers = new ArrayList<>();

        private TestEventLog(List<IdAndEvent> events) {
            this.events = events;
        }

        @Override
        public void submitEvent(UUID trackingId, ObjectNode eventPayload) {
            events.add(new IdAndEvent(trackingId, eventPayload));
            consumers.forEach(
                    consumer -> consumer.accept(trackingId, eventPayload));
        }

        @Override
        public Registration subscribe(UUID newerThan,
                BiConsumer<UUID, ObjectNode> consumer) {
            Predicate<IdAndEvent> filter = e -> true;
            if (newerThan != null) {
                filter = new Predicate<IdAndEvent>() {
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
    }

    public class TestBackend implements Backend {

        private final UUID id = UUID.randomUUID();

        @Override
        public EventLog openEventLog(String logId) {
            return eventLogs.computeIfAbsent(logId,
                    id -> new TestEventLog(events));
        }

        @Override
        public EventLog getMembershipEventLog() {
            return membershipEventLog;
        }

        @Override
        public UUID getNodeId() {
            return id;
        }

        @Override
        public CompletableFuture<ObjectNode> loadLatestSnapshot(String name) {
            return CompletableFuture.completedFuture(snapshots.get(name));
        }

        @Override
        public void submitSnapshot(String name, ObjectNode snapshot) {
            snapshots.put(name, snapshot);
        }
    }
}
