/*
 * Copyright (C) 2021 Vaadin Ltd
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
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;

import com.vaadin.collaborationengine.Backend;
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

        private HazelcastEventLog(IList<IdAndEvent> list) {
            this.list = list;
        }

        private synchronized void deliverEvents() {
            while (nextEventIndex < list.size()) {
                IdAndEvent idAndEvent = list.get(nextEventIndex++);
                eventConsumer.accept(idAndEvent.trackingId, idAndEvent.event);
            }
        }

        @Override
        public synchronized Registration subscribe(
                BiConsumer<UUID, ObjectNode> eventConsumer) {
            if (this.eventConsumer != null) {
                throw new IllegalStateException();
            }

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

    public HazelcastBackend(HazelcastInstance hz) {
        this.hz = Objects.requireNonNull(hz);
    }

    @Override
    public EventLog openEventLog(String topicId) {
        return new HazelcastEventLog(hz.getList(topicId));
    }
}
