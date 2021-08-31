/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single change that is emitted from the map data of a {@link Topic}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapChange extends AbstractMapChange {
    private final JsonNode oldValue;

    /**
     *
     * @param mapName
     *            Map name, not null.
     * @param key
     *            Map key, not null.
     * @param oldValue
     *            the old value ({@link JsonNode})
     * @param newValue
     *            the new value ({@link JsonNode})
     *
     * @since 1.0
     */
    public MapChange(String mapName, String key, JsonNode oldValue,
            JsonNode newValue) {
        super(mapName, key, newValue);
        this.oldValue = oldValue;
    }

    /**
     *
     * @return The value before the current change.
     *
     * @since 1.0
     */
    public JsonNode getOldValue() {
        return oldValue;
    }
}
