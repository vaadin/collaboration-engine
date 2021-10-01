/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.collaborationengine.Topic.ChangeDetails;

class MapChange implements ChangeDetails {
    private final String mapName;
    private final String key;
    private final JsonNode value;
    private final JsonNode oldValue;
    private final UUID expectedId;

    MapChange(String mapName, String key, JsonNode oldValue, JsonNode newValue,
            UUID expectedId) {
        Objects.requireNonNull(mapName, "Map name can not be null.");
        Objects.requireNonNull(key, MessageUtil.Required.KEY);
        this.mapName = mapName;
        this.key = key;
        this.value = newValue;
        this.oldValue = oldValue;
        this.expectedId = expectedId;
    }

    JsonNode getOldValue() {
        return oldValue;
    }

    String getMapName() {
        return mapName;
    }

    String getKey() {
        return key;
    }

    JsonNode getValue() {
        return value;
    }

    UUID getExpectedId() {
        return expectedId;
    }

    boolean hasChanges() {
        return !Objects.equals(getOldValue(), getValue());
    }
}
