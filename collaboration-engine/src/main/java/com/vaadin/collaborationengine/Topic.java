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
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.vaadin.flow.shared.Registration;

class Topic {

    @FunctionalInterface
    interface MapChangeNotifier {
        void onEntryChange(MapChange mapChange);
    }

    @FunctionalInterface
    interface ListChangeNotifier {
        void onListChange(ListChange listChange);
    }

    private final CollaborationEngine collaborationEngine;
    private final Map<String, Map<String, JsonNode>> namedMapData = new HashMap<>();
    private final List<MapChangeNotifier> mapChangeListeners = new ArrayList<>();
    private final Map<String, List<JsonNode>> namedListData = new HashMap<>();
    private final List<ListChangeNotifier> listChangeListeners = new ArrayList<>();
    final Map<String, Duration> expirationTimeouts = new HashMap<>();
    private Instant lastDisconnected;

    Topic(CollaborationEngine collaborationEngine) {
        this.collaborationEngine = collaborationEngine;
    }

    Registration subscribeToMapChange(MapChangeNotifier changeNotifier) {
        Clock clock = collaborationEngine.getClock();
        if (lastDisconnected != null) {
            Instant now = clock.instant();
            expirationTimeouts.forEach((name, timeout) -> {
                if (now.isAfter(lastDisconnected.plus(timeout))) {
                    namedMapData.get(name).clear();
                }
            });
        }
        lastDisconnected = null;
        mapChangeListeners.add(changeNotifier);
        return () -> {
            mapChangeListeners.remove(changeNotifier);
            if (mapChangeListeners.isEmpty()) {
                lastDisconnected = clock.instant();
            }
        };
    }

    Registration subscribeToListChange(ListChangeNotifier changeNotifier) {
        listChangeListeners.add(changeNotifier);
        return () -> listChangeListeners.remove(changeNotifier);
    }

    private void fireMapChangeEvent(MapChange change) {
        EventUtil.fireEvents(mapChangeListeners,
                listener -> listener.onEntryChange(change), true);
    }

    private void fireListChangeEvent(ListChange change) {
        EventUtil.fireEvents(listChangeListeners,
                listener -> listener.onListChange(change), true);
    }

    Stream<MapChange> getMapData(String mapName) {
        Map<String, JsonNode> mapData = namedMapData.get(mapName);
        if (mapData == null) {
            return Stream.empty();
        }
        return mapData.entrySet().stream().map(entry -> new MapChange(mapName,
                entry.getKey(), null, entry.getValue()));
    }

    JsonNode getMapValue(String mapName, String key) {
        Map<String, JsonNode> map = namedMapData.get(mapName);
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        return map.get(key).deepCopy();
    }

    void applyMapChange(PutChange change) {
        applyMapChange(change, null);
    }

    boolean applyMapChange(PutChange change, Predicate<Object> condition) {
        String mapName = change.getMapName();
        String key = change.getKey();

        Map<String, JsonNode> map = namedMapData.computeIfAbsent(mapName,
                name -> new HashMap<>());
        JsonNode oldValue = map.containsKey(key) ? map.get(key)
                : NullNode.getInstance();

        if (condition != null && !condition.test(oldValue)) {
            return false;
        }
        if (Objects.equals(oldValue, change.getValue())) {
            return true;
        }

        JsonNode newValue = change.getValue() == null ? NullNode.getInstance()
                : change.getValue();
        if (newValue instanceof NullNode) {
            map.remove(key);
        } else {
            map.put(key, newValue.deepCopy());
        }
        fireMapChangeEvent(new MapChange(mapName, key, oldValue, newValue));
        return true;
    }

    boolean applyMapReplace(ReplaceChange replaceChange) {
        return applyMapChange(new PutChange(replaceChange), oldValue -> Objects
                .equals(oldValue, replaceChange.getExpectedValue()));
    }

    void applyListChange(ListChange change) {
        String listName = change.getListName();
        JsonNode item = change.getAddedItem();
        getList(listName).add(item);
        fireListChangeEvent(new ListChange(listName, item));
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

}
