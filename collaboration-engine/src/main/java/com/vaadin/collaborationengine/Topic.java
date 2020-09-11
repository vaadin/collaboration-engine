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
import java.util.stream.Stream;

import com.vaadin.flow.function.SerializableBiFunction;
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
        new ArrayList<>(changeListeners).forEach(l -> l.onEntryChange(change));
    }

    Stream<MapChange> getMapData(String mapName) {
        Map<String, Object> mapData = namedMapData.get(mapName);
        if (mapData == null) {
            return Stream.empty();
        }
        return mapData.entrySet().stream().map(entry -> new MapChange(mapName,
                entry.getKey(), null, entry.getValue()));
    }

    <T> T withMap(String name,
            SerializableBiFunction<Map<String, Object>, ChangeNotifier, T> mapHandler) {
        return mapHandler.apply(
                namedMapData.computeIfAbsent(name, key -> new HashMap<>()),
                this::fireMapChangeEvent);
    }

}
