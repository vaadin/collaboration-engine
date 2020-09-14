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

/**
 * A change that will be applied to the map data of a {@link Topic} only when
 * this change has the latest snapshot of the changing data.
 *
 * @author Vaadin Ltd
 */
public class ReplaceChange extends AbstractMapChange {
    private final Object expectedValue;

    public ReplaceChange(String mapName, String key, Object expectedValue,
            Object newValue) {
        super(mapName, key, newValue);
        this.expectedValue = expectedValue;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

}
