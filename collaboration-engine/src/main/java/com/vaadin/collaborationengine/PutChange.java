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
 * A change that put a value to the map data of a {@link Topic}
 *
 * @author Vaadin Ltd
 */
class PutChange extends AbstractMapChange {

    PutChange(String mapName, String key, JsonNode value) {
        super(mapName, key, value);
    }

    PutChange(ReplaceChange replaceChange) {
        super(replaceChange.getMapName(), replaceChange.getKey(),
                replaceChange.getValue());
        Objects.requireNonNull(replaceChange, "Change can not be null.");
    }

}
