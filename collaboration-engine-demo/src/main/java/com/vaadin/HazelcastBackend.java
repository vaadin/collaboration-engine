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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import com.vaadin.collaborationengine.Backend;
import com.vaadin.collaborationengine.JsonUtil;
import com.vaadin.flow.shared.Registration;

public class HazelcastBackend implements Backend {
    private static final class IdAndEvent implements Serializable {
        private final UUID trackingId;
        private final ObjectNode event;

        private IdAndEvent(UUID trackingId, ObjectNode event) {
            this.trackingId = trackingId;
            this.event = event;
        }
    }

    private static class HazelcastEventLog implements EventLog {
        private final IList<IdAndEvent> list;

        private BiConsumer<UUID, ObjectNode> eventConsumer;
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
                BiConsumer<UUID, ObjectNode> eventConsumer) {
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
        public void submitEvent(UUID trackingId, ObjectNode event) {
            list.add(new IdAndEvent(trackingId, event));
        }
    }

    private final HazelcastInstance hz;

    private final IList<IdAndEvent> membershipEvents;

    private final IMap<String, ObjectNode> snapshots;

    public HazelcastBackend(HazelcastInstance hz) {
        this.hz = Objects.requireNonNull(hz);
        membershipEvents = this.hz
                .getList(HazelcastBackend.class.getName() + ".membership");
        this.snapshots = hz
                .getMap(HazelcastBackend.class.getName() + ".snapshots");
        this.hz.getCluster().addMembershipListener(new MembershipListener() {

            @Override
            public void memberRemoved(MembershipEvent membershipEvent) {
                UUID id = membershipEvent.getMember().getUuid();
                ObjectNode event = JsonUtil.createNodeLeave(id);
                membershipEvents.add(new IdAndEvent(UUID.randomUUID(), event));
            }

            @Override
            public void memberAdded(MembershipEvent membershipEvent) {
                UUID id = membershipEvent.getMember().getUuid();
                ObjectNode event = JsonUtil.createNodeJoin(id);
                membershipEvents.add(new IdAndEvent(UUID.randomUUID(), event));
            }
        });
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
    public CompletableFuture<ObjectNode> loadLatestSnapshot(String name) {
        return CompletableFuture.completedFuture(snapshots.get(name));
    }

    @Override
    public void submitSnapshot(String name, ObjectNode snapshot) {
        snapshots.put(name, snapshot);
    }

    @Override
    public EventLog getMembershipEventLog() {
        return new HazelcastEventLog(membershipEvents);
    }
}
