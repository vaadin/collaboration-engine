/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.collaborationengine.EntryList.ListEntrySnapshot;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

class Topic {

    enum ChangeResult {
        ACCEPTED, REJECTED;
    }

    /**
     * Marker interface to have something more specific than Object in method
     * signatures.
     */
    interface ChangeDetails {
        // Marker interface
    }

    private static class Entry {

        private final UUID revisionId;

        private final JsonNode data;

        public Entry(UUID id, JsonNode data) {
            this.revisionId = id;
            this.data = data;
        }
    }

    private final CollaborationEngine collaborationEngine;
    private final Map<String, Map<String, Entry>> namedMapData = new HashMap<>();
    private final Map<String, EntryList> namedListData = new HashMap<>();
    final Map<String, Duration> mapExpirationTimeouts = new HashMap<>();
    final Map<String, Duration> listExpirationTimeouts = new HashMap<>();
    private Instant lastDisconnected;
    private final List<SerializableBiConsumer<UUID, ChangeDetails>> changeListeners = new ArrayList<>();
    private final Map<UUID, SerializableConsumer<ChangeResult>> changeResultTrackers = new ConcurrentHashMap<>();

    Topic(CollaborationEngine collaborationEngine) {
        this.collaborationEngine = collaborationEngine;
    }

    Registration subscribeToChange(
            SerializableBiConsumer<UUID, ChangeDetails> changeListener) {
        clearExpiredData();
        changeListeners.add(changeListener);
        return Registration.combine(
                () -> changeListeners.remove(changeListener),
                this::updateLastDisconnected);
    }

    private void clearExpiredData() {
        Clock clock = collaborationEngine.getClock();
        if (lastDisconnected != null) {
            Instant now = clock.instant();
            mapExpirationTimeouts.forEach((name, timeout) -> {
                if (now.isAfter(lastDisconnected.plus(timeout))) {
                    namedMapData.get(name).clear();
                }
            });
            listExpirationTimeouts.forEach((name, timeout) -> {
                if (now.isAfter(lastDisconnected.plus(timeout))) {
                    getList(name).ifPresent(EntryList::clear);
                }
            });
        }
        lastDisconnected = null;
    }

    private void updateLastDisconnected() {
        if (changeListeners.isEmpty()) {
            lastDisconnected = collaborationEngine.getClock().instant();
        }
    }

    Stream<MapChange> getMapData(String mapName) {
        Map<String, Entry> mapData = namedMapData.get(mapName);
        if (mapData == null) {
            return Stream.empty();
        }
        return mapData.entrySet().stream().map(entry -> new MapChange(mapName,
                entry.getKey(), null, entry.getValue().data, null));
    }

    JsonNode getMapValue(String mapName, String key) {
        Map<String, Entry> map = namedMapData.get(mapName);
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        return map.get(key).data.deepCopy();
    }

    synchronized ChangeResult applyChange(UUID trackingId, ObjectNode change) {
        String type = change.get(JsonUtil.CHANGE_TYPE).asText();
        ChangeDetails details;
        switch (type) {
        case JsonUtil.CHANGE_TYPE_PUT:
            details = applyMapChange(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_APPEND:
            details = applyListAppend(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_LIST_SET:
            details = applyListSet(trackingId, change);
            break;
        default:
            throw new UnsupportedOperationException(
                    "Type '" + type + "' is not a supported change type");
        }
        ChangeResult result = details != null ? ChangeResult.ACCEPTED
                : ChangeResult.REJECTED;

        SerializableConsumer<ChangeResult> changeResultTracker = changeResultTrackers
                .remove(trackingId);
        if (changeResultTracker != null) {
            changeResultTracker.accept(result);
        }
        if (ChangeResult.ACCEPTED.equals(result)) {
            EventUtil.fireEvents(changeListeners,
                    listener -> listener.accept(trackingId, details), true);
        }
        return result;
    }

    ChangeDetails applyMapChange(UUID changeId, ObjectNode change) {
        String mapName = change.get(JsonUtil.CHANGE_NAME).asText();
        String key = change.get(JsonUtil.CHANGE_KEY).asText();
        JsonNode expectedValue = change.get(JsonUtil.CHANGE_EXPECTED_VALUE);
        JsonNode expectedId = change.get(JsonUtil.CHANGE_EXPECTED_ID);
        JsonNode newValue = change.get(JsonUtil.CHANGE_VALUE);

        Map<String, Entry> map = namedMapData.computeIfAbsent(mapName,
                name -> new HashMap<>());
        JsonNode oldValue = map.containsKey(key) ? map.get(key).data
                : NullNode.getInstance();
        UUID oldChangeId = map.containsKey(key) ? map.get(key).revisionId
                : null;

        if (expectedId != null
                && !Objects.equals(oldChangeId, JsonUtil.toUUID(expectedId))) {
            return null;
        }
        if (expectedValue != null && !Objects.equals(oldValue, expectedValue)) {
            return null;
        }

        if (newValue instanceof NullNode) {
            map.remove(key);
        } else {
            map.put(key, new Entry(changeId, newValue.deepCopy()));
        }
        return new MapChange(mapName, key, oldValue, newValue,
                JsonUtil.toUUID(expectedId));
    }

    ChangeDetails applyListAppend(UUID id, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        ListEntrySnapshot insertedEntry = getOrCreateList(listName)
                .insertLast(id, item, id);

        return new ListChange(listName, ListChangeType.INSERT, id, null, item,
                null, insertedEntry.prev, null, null, null);
    }

    private ChangeDetails applyListSet(UUID trackingId, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        UUID key = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_KEY));
        JsonNode newValue = change.get(JsonUtil.CHANGE_VALUE);
        UUID expectedId = JsonUtil
                .toUUID(change.get(JsonUtil.CHANGE_EXPECTED_ID));
        EntryList list = getOrCreateList(listName);

        ListEntrySnapshot entry = list.getEntry(key);
        if (entry == null) {
            return null;
        }
        if (expectedId != null
                && !Objects.equals(entry.revisionId, expectedId)) {
            return null;
        }
        if (newValue.isNull()) {
            list.remove(key);
            return new ListChange(listName, ListChangeType.SET, key,
                    entry.value, null, entry.prev, null, entry.next, null,
                    expectedId);
        } else {
            JsonNode oldValue = entry.value;
            list.setValue(key, newValue, trackingId);
            return new ListChange(listName, ListChangeType.SET, key, oldValue,
                    newValue, entry.prev, entry.prev, entry.next, entry.next,
                    expectedId);
        }
    }

    Stream<ListChange> getListChanges(String listName) {
        return getListItems(listName).map(
                item -> new ListChange(listName, ListChangeType.INSERT, item.id,
                        null, item.value, null, item.prev, null, null, null));
    }

    Stream<ListEntrySnapshot> getListItems(String listName) {
        return getList(listName).map(EntryList::stream)
                .orElseGet(Stream::empty);
    }

    JsonNode getListValue(String listName, UUID key) {
        return getList(listName).map(list -> list.getValue(key)).orElse(null);
    }

    private EntryList getOrCreateList(String listName) {
        return namedListData.computeIfAbsent(listName, name -> new EntryList());
    }

    private Optional<EntryList> getList(String listName) {
        return Optional.ofNullable(namedListData.get(listName));
    }

    void setChangeResultTracker(UUID id,
            SerializableConsumer<ChangeResult> changeResultTracker) {
        SerializableConsumer<ChangeResult> oldTracker = changeResultTrackers
                .putIfAbsent(id, changeResultTracker);
        if (oldTracker != null) {
            throw new IllegalStateException(
                    "Cannot set a change-result tracker for an id with one already set");
        }
    }

    // For testing
    boolean hasChangeListeners() {
        return !changeListeners.isEmpty();
    }
}
