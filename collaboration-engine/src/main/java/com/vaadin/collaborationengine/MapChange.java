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

import java.util.Objects;

/**
 * A single change that is emitted from the map data of a {@link Topic}.
 *
 * @author Vaadin Ltd
 */
public class MapChange {
    private final String mapName;
    private final String key;
    private final Object oldValue;
    private final Object newValue;

    public MapChange(String mapName, String key, Object oldValue,
            Object newValue) {
        Objects.requireNonNull(mapName, "Map name can not be null.");
        Objects.requireNonNull(key, "Key can not be null.");
        this.mapName = mapName;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getMapName() {
        return mapName;
    }

    public String getKey() {
        return key;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
