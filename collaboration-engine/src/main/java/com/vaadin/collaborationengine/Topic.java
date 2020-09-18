/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.shared.Registration;

class Topic {

    @FunctionalInterface
    interface ChangeNotifier {
        void onEntryChange(MapChange mapChange);
    }

    private final Map<String, Map<String, Object>> namedMapData = new HashMap<>();
    private final List<ChangeNotifier> changeListeners = new ArrayList<>();

    Registration subscribe(ChangeNotifier changeNotifier) {
        return Registration.addAndRemove(changeListeners, changeNotifier);
    }

    private void fireMapChangeEvent(MapChange change) {
        EventUtil.fireEvents(changeListeners,
                listener -> listener.onEntryChange(change), true);
    }

    Stream<MapChange> getMapData(String mapName) {
        Map<String, Object> mapData = namedMapData.get(mapName);
        if (mapData == null) {
            return Stream.empty();
        }
        return mapData.entrySet().stream().map(entry -> new MapChange(mapName,
                entry.getKey(), null, entry.getValue()));
    }

    Object getMapValue(String mapName, String key) {
        Map<String, Object> map = namedMapData.get(mapName);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    void applyChange(PutChange change) {
        applyChange(change, null);
    }

    boolean applyChange(PutChange change, Predicate<Object> condition) {
        String mapName = change.getMapName();
        String key = change.getKey();

        Map<String, Object> map = namedMapData.computeIfAbsent(mapName,
                name -> new HashMap<>());
        Object oldValue = map.get(key);

        if (condition != null && !condition.test(oldValue)) {
            return false;
        }
        if (Objects.equals(oldValue, change.getValue())) {
            return true;
        }

        Object newValue = change.getValue();
        if (newValue == null) {
            map.remove(key);
        } else {
            map.put(key, newValue);
        }
        fireMapChangeEvent(new MapChange(mapName, key, oldValue, newValue));
        return true;
    }

    boolean applyReplace(ReplaceChange replaceChange) {
        return applyChange(new PutChange(replaceChange), oldValue -> Objects
                .equals(oldValue, replaceChange.getExpectedValue()));
    }

}
