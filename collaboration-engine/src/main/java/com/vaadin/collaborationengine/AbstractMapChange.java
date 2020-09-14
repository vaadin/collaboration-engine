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
 * A change of the map data in a {@link Topic}
 *
 * @author Vaadin Ltd
 */
public class AbstractMapChange {
    private final String mapName;
    private final String key;
    private final Object value;

    public AbstractMapChange(String mapName, String key, Object value) {
        Objects.requireNonNull(mapName, "Map name can not be null.");
        Objects.requireNonNull(key, "Key can not be null.");
        this.mapName = mapName;
        this.key = key;
        this.value = value;
    }

    public String getMapName() {
        return mapName;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
