/*
 * Copyright 2020-2022 Vaadin Ltd.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
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

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class Entry {

        final UUID revisionId;

        final JsonNode data;

        final UUID scopeOwnerId;

        @JsonCreator
        public Entry(@JsonProperty("id") UUID id,
                @JsonProperty("data") JsonNode data,
                @JsonProperty("scopeOwnerId") UUID scopeOwnerId) {
            this.revisionId = id;
            this.data = data;
            this.scopeOwnerId = scopeOwnerId;
        }
    }

    static class Snapshot {
        private static final TypeReference<Map<String, EntryList>> LISTS_TYPE = new TypeReference<Map<String, EntryList>>() {
        };
        private static final TypeReference<Map<String, Map<String, Entry>>> MAPS_TYPE = new TypeReference<Map<String, Map<String, Entry>>>() {
        };
        private static final TypeReference<List<UUID>> BACKEND_NODES_TYPE = new TypeReference<List<UUID>>() {
        };

        private static final String LATEST = "latest";
        private static final String LISTS = "lists";
        private static final String MAPS = "maps";
        private static final String BACKEND_NODES = "backend-nodes";
        private final ObjectNode objectNode;

        Snapshot(ObjectNode objectNode) {
            this.objectNode = Objects.requireNonNull(objectNode);
        }

        static Snapshot fromTopic(Topic topic, UUID latestChangeId) {
            ObjectNode objectNode = JsonUtil.getObjectMapper()
                    .createObjectNode();
            objectNode.put(LATEST, latestChangeId.toString());
            objectNode.set(LISTS, JsonUtil.toJsonNode(topic.namedListData));
            objectNode.set(MAPS, JsonUtil.toJsonNode(topic.namedMapData));
            objectNode.set(BACKEND_NODES,
                    JsonUtil.toJsonNode(topic.backendNodes));
            return new Snapshot(objectNode);
        }

        ObjectNode toObjectNode() {
            return objectNode;
        }

        Map<String, EntryList> getLists() {
            return JsonUtil.toInstance(objectNode.get(LISTS), LISTS_TYPE);
        }

        Map<String, Map<String, Entry>> getMaps() {
            return JsonUtil.toInstance(objectNode.get(MAPS), MAPS_TYPE);
        }

        List<UUID> getBackendNodes() {
            return JsonUtil.toInstance(objectNode.get(BACKEND_NODES),
                    BACKEND_NODES_TYPE);
        }

    }

    private final String id;
    private final CollaborationEngine collaborationEngine;
    private final Map<String, Map<String, Entry>> namedMapData = new HashMap<>();
    private final Map<String, EntryList> namedListData = new HashMap<>();
    final Map<String, Duration> mapExpirationTimeouts = new HashMap<>();
    final Map<String, Duration> listExpirationTimeouts = new HashMap<>();
    private Instant lastDisconnected;
    private final List<SerializableBiConsumer<UUID, ChangeDetails>> changeListeners = new ArrayList<>();
    private final Map<UUID, SerializableConsumer<ChangeResult>> changeResultTrackers = new ConcurrentHashMap<>();
    private final List<UUID> backendNodes = new ArrayList<>();
    private final Backend.EventLog eventLog;
    private boolean leader;
    private int changeCount;

    Topic(String id, CollaborationEngine collaborationEngine,
            Backend.EventLog eventLog) {
        this.id = id;
        this.collaborationEngine = collaborationEngine;
        this.eventLog = eventLog;
        final Backend backend = getBackend();
        backend.getMembershipEventLog().subscribe(null,
                (changeId, changeNode) -> {
                    String type = changeNode.get(JsonUtil.CHANGE_TYPE).asText();
                    if (JsonUtil.CHANGE_NODE_LEAVE.equals(type)) {
                        handleNodeLeave(changeNode);
                    }
                });
        backend.loadLatestSnapshot(id).thenAccept(this::initializeFromSnapshot);
    }

    UUID getCurrentNodeId() {
        return getBackend().getNodeId();
    }

    private Backend getBackend() {
        return collaborationEngine.getConfiguration().getBackend();
    }

    private void initializeFromSnapshot(ObjectNode snapshot) {
        if (snapshot != null) {
            UUID latestChange = JsonUtil.toUUID(snapshot.get(Snapshot.LATEST));
            loadSnapshot(new Snapshot(snapshot));
            eventLog.subscribe(latestChange, this::applyChange);
        } else {
            eventLog.subscribe(null, this::applyChange);
        }

        ObjectNode nodeEvent = JsonUtil.createNodeJoin(getCurrentNodeId());

        eventLog.submitEvent(UUID.randomUUID(), nodeEvent);
    }

    synchronized void handleNodeLeave(ObjectNode changeNode) {
        UUID nodeId = UUID
                .fromString(changeNode.get(JsonUtil.CHANGE_NODE_ID).asText());
        Backend backend = this.collaborationEngine.getConfiguration()
                .getBackend();
        backendNodes.remove(nodeId);
        if (!backendNodes.isEmpty()
                && backendNodes.get(0).equals(backend.getNodeId())) {
            becomeLeader();
        }
        if (leader) {
            cleanupStaleEntries(nodeId::equals);
        }
    }

    private void cleanupStaleEntries(Predicate<UUID> isStale) {
        namedMapData.entrySet().stream().flatMap(
                map -> map.getValue().entrySet().stream().filter(entry -> {
                    return isStale.test(entry.getValue().scopeOwnerId);
                }).map(entry -> {
                    ObjectNode change = JsonUtil.createPutChange(map.getKey(),
                            entry.getKey(), null, null, null);
                    change.put(JsonUtil.CHANGE_EXPECTED_ID,
                            entry.getValue().revisionId.toString());
                    return change;
                })).collect(Collectors.toList()).forEach(change -> {
                    eventLog.submitEvent(UUID.randomUUID(), change);
                });
        namedListData.entrySet().stream()
                .flatMap(list -> list.getValue().stream().filter(entry -> {
                    return isStale.test(entry.scopeOwnerId);
                }).map(entry -> {
                    ObjectNode change = JsonUtil.createListSetChange(
                            list.getKey(), entry.id.toString(), null, null);
                    change.put(JsonUtil.CHANGE_EXPECTED_ID,
                            entry.revisionId.toString());
                    return change;
                })).collect(Collectors.toList()).forEach(change -> {
                    eventLog.submitEvent(UUID.randomUUID(), change);
                });
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
                if (now.isAfter(lastDisconnected.plus(timeout))
                        && namedMapData.containsKey(name)) {
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
        return mapData.entrySet().stream()
                .map(entry -> new MapChange(mapName, MapChangeType.PUT,
                        entry.getKey(), null, entry.getValue().data, null,
                        entry.getValue().revisionId));
    }

    JsonNode getMapValue(String mapName, String key) {
        Map<String, Entry> map = namedMapData.get(mapName);
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        return map.get(key).data.deepCopy();
    }

    synchronized ChangeResult applyChange(UUID trackingId, ObjectNode change) {
        changeCount++;
        String type = change.get(JsonUtil.CHANGE_TYPE).asText();
        ChangeDetails details;
        switch (type) {
        case JsonUtil.CHANGE_TYPE_PUT:
            details = applyMapPut(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_REPLACE:
            details = applyMapReplace(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_PREPEND:
            details = applyListPrepend(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_APPEND:
            details = applyListAppend(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_INSERT_BEFORE:
            details = applyListInsertBefore(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_INSERT_AFTER:
            details = applyListInsertAfter(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_MOVE_BEFORE:
            details = applyListMoveBefore(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_MOVE_AFTER:
            details = applyListMoveAfter(trackingId, change);
            break;
        case JsonUtil.CHANGE_TYPE_LIST_SET:
            details = applyListSet(trackingId, change);
            break;
        case JsonUtil.CHANGE_NODE_JOIN:
            UUID nodeId = UUID
                    .fromString(change.get(JsonUtil.CHANGE_NODE_ID).asText());
            if (backendNodes.isEmpty() && collaborationEngine.getConfiguration()
                    .getBackend().getNodeId().equals(nodeId)) {
                becomeLeader();
            }
            backendNodes.add(nodeId);
            return ChangeResult.ACCEPTED;
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
        if (leader && changeCount % 100 == 0) {
            getBackend().submitSnapshot(id,
                    Snapshot.fromTopic(this, trackingId).toObjectNode());
        }
        return result;
    }

    void loadSnapshot(Snapshot snapshot) {
        if (!namedListData.isEmpty() || !namedMapData.isEmpty()
                || !backendNodes.isEmpty()) {
            throw new IllegalStateException(
                    "You can only load snapshots for empty topics");
        }
        namedListData.putAll(snapshot.getLists());
        namedMapData.putAll(snapshot.getMaps());
        backendNodes.addAll(snapshot.getBackendNodes());
    }

    private void becomeLeader() {
        leader = true;
        Set<UUID> backendNodesCopy = new HashSet<>(backendNodes);
        cleanupStaleEntries(id -> !backendNodesCopy.contains(id));
    }

    boolean isLeader() {
        return leader;
    }

    ChangeDetails applyMapPut(UUID changeId, ObjectNode change) {
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
            map.put(key, new Entry(changeId, newValue.deepCopy(),
                    JsonUtil.toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER))));
        }
        return new MapChange(mapName, MapChangeType.PUT, key, oldValue,
                newValue, JsonUtil.toUUID(expectedId), changeId);
    }

    ChangeDetails applyMapReplace(UUID changeId, ObjectNode change) {
        String mapName = change.get(JsonUtil.CHANGE_NAME).asText();
        String key = change.get(JsonUtil.CHANGE_KEY).asText();
        JsonNode expectedValue = change.get(JsonUtil.CHANGE_EXPECTED_VALUE);
        JsonNode newValue = change.get(JsonUtil.CHANGE_VALUE);

        Map<String, Entry> map = namedMapData.computeIfAbsent(mapName,
                name -> new HashMap<>());
        JsonNode oldValue = map.containsKey(key) ? map.get(key).data
                : NullNode.getInstance();

        if (expectedValue != null && !Objects.equals(oldValue, expectedValue)) {
            return null;
        }

        if (newValue instanceof NullNode) {
            map.remove(key);
        } else {
            map.put(key, new Entry(changeId, newValue.deepCopy(),
                    JsonUtil.toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER))));
        }
        return new MapChange(mapName, MapChangeType.REPLACE, key, oldValue,
                newValue, null, changeId);
    }

    ChangeDetails applyListPrepend(UUID id, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        UUID scopeOwnerId = JsonUtil
                .toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER));
        ListEntrySnapshot insertedEntry = getOrCreateList(listName)
                .insertFirst(id, item, id, scopeOwnerId);

        return new ListChange(listName, ListChangeType.INSERT, id, null, item,
                null, null, null, insertedEntry.next, null, id);
    }

    ChangeDetails applyListAppend(UUID id, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        UUID scopeOwnerId = JsonUtil
                .toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER));
        ListEntrySnapshot insertedEntry = getOrCreateList(listName)
                .insertLast(id, item, id, scopeOwnerId);

        return new ListChange(listName, ListChangeType.INSERT, id, null, item,
                null, insertedEntry.prev, null, null, null, id);
    }

    ChangeDetails applyListInsertBefore(UUID id, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        UUID key = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_KEY));
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        UUID scopeOwnerId = JsonUtil
                .toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER));
        EntryList list = getOrCreateList(listName);

        ListEntrySnapshot entry = list.getEntry(key);
        if (entry == null) {
            return null;
        }

        ListEntrySnapshot insertedEntry = list.insertBefore(key, id, item, id,
                scopeOwnerId);

        return new ListChange(listName, ListChangeType.INSERT, id, null, item,
                null, insertedEntry.prev, null, insertedEntry.next, null, id);
    }

    ChangeDetails applyListInsertAfter(UUID id, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        UUID key = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_KEY));
        JsonNode item = change.get(JsonUtil.CHANGE_ITEM);
        UUID scopeOwnerId = JsonUtil
                .toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER));
        EntryList list = getOrCreateList(listName);

        ListEntrySnapshot entry = list.getEntry(key);
        if (entry == null) {
            return null;
        }

        ListEntrySnapshot insertedEntry = list.insertAfter(key, id, item, id,
                scopeOwnerId);

        return new ListChange(listName, ListChangeType.INSERT, id, null, item,
                null, insertedEntry.prev, null, insertedEntry.next, null, id);
    }

    ChangeDetails applyListMoveBefore(UUID trackingId, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        UUID keyToFind = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_KEY));
        UUID keyToMove = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_OTHER_KEY));
        EntryList list = getOrCreateList(listName);

        ListEntrySnapshot entryToFind = list.getEntry(keyToFind);
        if (entryToFind == null) {
            return null;
        }
        ListEntrySnapshot entryToMove = list.getEntry(keyToMove);
        if (entryToMove == null) {
            return null;
        }

        list.moveBefore(keyToFind, keyToMove, trackingId);

        return new ListChange(listName, ListChangeType.MOVE, keyToMove,
                entryToMove.value, entryToMove.value, entryToMove.prev,
                entryToFind.prev, entryToMove.next, keyToFind, null,
                trackingId);
    }

    ChangeDetails applyListMoveAfter(UUID trackingId, ObjectNode change) {
        String listName = change.get(JsonUtil.CHANGE_NAME).asText();
        UUID keyToFind = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_KEY));
        UUID keyToMove = JsonUtil.toUUID(change.get(JsonUtil.CHANGE_OTHER_KEY));
        EntryList list = getOrCreateList(listName);

        ListEntrySnapshot entryToFind = list.getEntry(keyToFind);
        if (entryToFind == null) {
            return null;
        }
        ListEntrySnapshot entryToMove = list.getEntry(keyToMove);
        if (entryToMove == null) {
            return null;
        }

        list.moveAfter(keyToFind, keyToMove, trackingId);

        return new ListChange(listName, ListChangeType.MOVE, keyToMove,
                entryToMove.value, entryToMove.value, entryToMove.prev,
                keyToFind, entryToMove.next, entryToFind.next, null,
                trackingId);
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
                    expectedId, null);
        } else {
            JsonNode oldValue = entry.value;
            UUID scopeOwnerId = JsonUtil
                    .toUUID(change.get(JsonUtil.CHANGE_SCOPE_OWNER));
            list.setValue(key, newValue, trackingId, scopeOwnerId);
            return new ListChange(listName, ListChangeType.SET, key, oldValue,
                    newValue, entry.prev, entry.prev, entry.next, entry.next,
                    expectedId, trackingId);
        }
    }

    Stream<ListChange> getListChanges(String listName) {
        return getListItems(listName).map(item -> new ListChange(listName,
                ListChangeType.INSERT, item.id, null, item.value, null,
                item.prev, null, null, null, item.revisionId));
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
