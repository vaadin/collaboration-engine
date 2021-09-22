/*
 * Copyright (C) 2020 Vaadin Ltd
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

class Topic {

    enum ChangeResult {
        ACCEPTED, REJECTED;
    }

    @FunctionalInterface
    interface MapChangeNotifier {
        void onEntryChange(MapChange mapChange);
    }

    @FunctionalInterface
    interface ListChangeNotifier {
        void onListChange(ListChange listChange);
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
    private final Map<String, List<JsonNode>> namedListData = new HashMap<>();
    final Map<String, Duration> mapExpirationTimeouts = new HashMap<>();
    final Map<String, Duration> listExpirationTimeouts = new HashMap<>();
    private Instant lastDisconnected;
    private final List<SerializableBiConsumer<UUID, ObjectNode>> changeListeners = new ArrayList<>();
    private final Map<UUID, SerializableConsumer<ChangeResult>> changeResultTrackers = new ConcurrentHashMap<>();

    Topic(CollaborationEngine collaborationEngine) {
        this.collaborationEngine = collaborationEngine;
    }

    Registration subscribeToChange(
            SerializableBiConsumer<UUID, ObjectNode> changeListener) {
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
                    namedListData.get(name).clear();
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

    private void fireChangeEvent(UUID id, ObjectNode change) {
        EventUtil.fireEvents(changeListeners,
                listener -> listener.accept(id, change), true);
    }

    Stream<MapChange> getMapData(String mapName) {
        Map<String, Entry> mapData = namedMapData.get(mapName);
        if (mapData == null) {
            return Stream.empty();
        }
        return mapData.entrySet().stream().map(entry -> new MapChange(mapName,
                entry.getKey(), null, entry.getValue().data));
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
        ChangeResult result;
        switch (type) {
        case JsonUtil.CHANGE_TYPE_PUT:
            result = applyMapChange(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_APPEND:
            result = applyListChange(change);
            break;
        default:
            throw new UnsupportedOperationException(
                    "Type '" + type + "' is not a supported change type");
        }
        SerializableConsumer<ChangeResult> changeResultTracker = changeResultTrackers
                .remove(trackingId);
        if (changeResultTracker != null) {
            changeResultTracker.accept(result);
        }
        if (ChangeResult.ACCEPTED.equals(result)) {
            fireChangeEvent(trackingId, change);
        }
        return result;
    }

    ChangeResult applyMapChange(UUID changeId, ObjectNode change) {
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
            return ChangeResult.REJECTED;
        }
        if (expectedValue != null && !Objects.equals(oldValue, expectedValue)) {
            return ChangeResult.REJECTED;
        }

        // FIXME ugly (until proper topic data versioning)
        change.set(JsonUtil.CHANGE_OLD_VALUE, oldValue);
        if (newValue instanceof NullNode) {
            map.remove(key);
        } else {
            map.put(key, new Entry(changeId, newValue.deepCopy()));
        }
        return ChangeResult.ACCEPTED;
    }

    ChangeResult applyListChange(ObjectNode change) {
        String name = change.get(JsonUtil.CHANGE_NAME).asText();
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        getList(name).add(item);
        return ChangeResult.ACCEPTED;
    }

    Stream<ListChange> getListChanges(String listName) {
        return getListItems(listName)
                .map(item -> new ListChange(listName, item));
    }

    Stream<JsonNode> getListItems(String listName) {
        return getList(listName).stream();
    }

    private List<JsonNode> getList(String listName) {
        return namedListData.computeIfAbsent(listName,
                name -> new ArrayList<>());
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
