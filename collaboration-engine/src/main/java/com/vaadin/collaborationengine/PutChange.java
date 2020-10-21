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
