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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A change of the map data in a {@link Topic}
 *
 * @author Vaadin Ltd
 */
public class AbstractMapChange {
    private final String mapName;
    private final String key;
    private final JsonNode value;

    public AbstractMapChange(String mapName, String key, JsonNode value) {
        Objects.requireNonNull(mapName, "Map name can not be null.");
        Objects.requireNonNull(key, MessageUtil.Required.KEY);
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

    public JsonNode getValue() {
        return value;
    }
}
