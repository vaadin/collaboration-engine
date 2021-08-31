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
 * A change that will be applied to the map data of a {@link Topic} only when
 * this change has the latest snapshot of the changing data.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class ReplaceChange extends AbstractMapChange {
    private final JsonNode expectedValue;

    ReplaceChange(String mapName, String key, JsonNode expectedValue,
            JsonNode newValue) {
        super(mapName, key, newValue);
        this.expectedValue = expectedValue;
    }

    JsonNode getExpectedValue() {
        return expectedValue;
    }

}
