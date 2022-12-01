/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.hazelcast.cluster.InitialMembershipEvent;
import com.hazelcast.cluster.InitialMembershipListener;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import com.vaadin.collaborationengine.Backend;
import com.vaadin.collaborationengine.MembershipEvent.MembershipEventType;
import com.vaadin.collaborationengine.MembershipListener;
import com.vaadin.flow.shared.Registration;

public class HazelcastBackend extends Backend {
    private static final class IdAndEvent implements Serializable {
        private final UUID trackingId;
        private final String event;

        private IdAndEvent(UUID trackingId, String event) {
            this.trackingId = trackingId;
            this.event = event;
        }
    }

    private static class HazelcastEventLog implements EventLog {
        private final IList<IdAndEvent> list;

        private BiConsumer<UUID, String> eventConsumer;
        private int nextEventIndex = 0;

        private UUID newerThan;

        private HazelcastEventLog(IList<IdAndEvent> list) {
            this.list = list;
        }

        private synchronized void deliverEvents() {
            while (nextEventIndex < list.size()) {
                IdAndEvent idAndEvent = list.get(nextEventIndex++);
                if (this.newerThan == null) {
                    eventConsumer.accept(idAndEvent.trackingId,
                            idAndEvent.event);
                } else {
                    if (idAndEvent.trackingId.equals(newerThan)) {
                        this.newerThan = null;
                    }
                }
            }
        }

        private synchronized void handleRemoveItem() {
            if (nextEventIndex > 0) {
                nextEventIndex--;
            }
        }

        @Override
        public synchronized Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> eventConsumer)
                throws EventIdNotFoundException {
            if (this.eventConsumer != null) {
                throw new IllegalStateException();
            }

            if (newerThan != null) {
                Optional<IdAndEvent> newerThanIdAndEvent = list.stream()
                        .filter(item -> newerThan.equals(item.trackingId))
                        .findFirst();
                if (newerThanIdAndEvent.isEmpty()) {
                    throw new EventIdNotFoundException(
                            "newerThan doesn't " + "exist in the log.");
                }
            }
            this.newerThan = newerThan;
            this.eventConsumer = eventConsumer;
            nextEventIndex = 0;

            UUID registrationId = list
                    .addItemListener(new ItemListener<IdAndEvent>() {
                        @Override
                        public void itemAdded(ItemEvent<IdAndEvent> item) {
                            deliverEvents();
                        }

                        @Override
                        public void itemRemoved(ItemEvent<IdAndEvent> item) {
                            handleRemoveItem();
                        }
                    }, false);

            // Deliver initial events
            deliverEvents();

            return () -> {
                synchronized (this) {
                    list.removeItemListener(registrationId);
                    this.eventConsumer = null;
                }
            };
        }

        @Override
        public void submitEvent(UUID trackingId, String event) {
            list.add(new IdAndEvent(trackingId, event));
        }

        @Override
        public synchronized void truncate(UUID olderThan) {
            Predicate<IdAndEvent> filter = e -> true;
            if (olderThan != null) {
                Optional<IdAndEvent> olderThanIdAndEvent = list.stream()
                        .filter(item -> olderThan.equals(item.trackingId))
                        .findFirst();
                if (olderThanIdAndEvent.isEmpty()) {
                    // NOOP
                    return;
                }
                filter = new Predicate<>() {
                    boolean found;

                    @Override
                    public boolean test(IdAndEvent idAndEvent) {
                        found = found
                                || olderThan.equals(idAndEvent.trackingId);
                        return !found;
                    }
                };
            }
            list.removeIf(filter);
        }
    }

    private final HazelcastInstance hz;

    private final IMap<String, Snapshot> snapshots;

    public HazelcastBackend(HazelcastInstance hz) {
        this.hz = Objects.requireNonNull(hz);
        this.snapshots = hz
                .getMap(HazelcastBackend.class.getName() + ".snapshots");
    }

    @Override
    public Registration addMembershipListener(
            MembershipListener membershipListener) {
        UUID registrationId = hz.getCluster()
                .addMembershipListener(new InitialMembershipListener() {

                    @Override
                    public void init(InitialMembershipEvent event) {
                        event.getMembers()
                                .forEach(member -> submitEvent(
                                        MembershipEventType.JOIN,
                                        member.getUuid()));
                    }

                    @Override
                    public void memberAdded(MembershipEvent membershipEvent) {
                        submitEvent(MembershipEventType.JOIN,
                                membershipEvent.getMember().getUuid());
                    }

                    @Override
                    public void memberRemoved(MembershipEvent membershipEvent) {
                        submitEvent(MembershipEventType.LEAVE,
                                membershipEvent.getMember().getUuid());
                    }

                    private void submitEvent(MembershipEventType type,
                            UUID id) {
                        membershipListener.handleMembershipEvent(
                                new com.vaadin.collaborationengine.MembershipEvent(
                                        type, id, getCollaborationEngine()));
                    }
                });
        return () -> hz.getCluster().removeMembershipListener(registrationId);
    }

    @Override
    public EventLog openEventLog(String topicId) {
        return new HazelcastEventLog(hz.getList(topicId));
    }

    @Override
    public UUID getNodeId() {
        return hz.getCluster().getLocalMember().getUuid();
    }

    @Override
    public CompletableFuture<Snapshot> loadLatestSnapshot(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return CompletableFuture.completedFuture(snapshots.get(name));
    }

    @Override
    public CompletableFuture<Void> replaceSnapshot(String name, UUID expectedId,
            UUID newId, String payload) {
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
