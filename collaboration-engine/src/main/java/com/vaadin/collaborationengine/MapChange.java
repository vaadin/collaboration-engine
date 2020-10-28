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
 */
public class MapChange extends AbstractMapChange {
    private final JsonNode oldValue;

    public MapChange(String mapName, String key, JsonNode oldValue,
            JsonNode newValue) {
        super(mapName, key, newValue);
        this.oldValue = oldValue;
    }

    public JsonNode getOldValue() {
        return oldValue;
    }
}
