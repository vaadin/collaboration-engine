/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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

        @Override
        public synchronized Registration subscribe(UUID newerThan,
                BiConsumer<UUID, String> eventConsumer) {
            if (this.eventConsumer != null) {
                throw new IllegalStateException();
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
                            throw new UnsupportedOperationException();
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
    }

    private final HazelcastInstance hz;

    private final IMap<String, String> snapshots;

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
    public CompletableFuture<String> loadLatestSnapshot(String name) {
        return CompletableFuture.completedFuture(snapshots.get(name));
    }

    @Override
    public void submitSnapshot(String name, String snapshot) {
        snapshots.put(name, snapshot);
    }
}
