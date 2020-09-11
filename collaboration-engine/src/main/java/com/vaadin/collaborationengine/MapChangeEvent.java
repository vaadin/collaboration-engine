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

import java.util.EventObject;
import java.util.Objects;

/**
 * Event that is fired when the value in a collaboration map changes.
 *
 * @author Vaadin Ltd
 */
public class MapChangeEvent extends EventObject {

    private final String key;
    private final Object oldValue;
    private final Object value;

    /**
     * Creates a new map change event.
     * 
     * @param source
     *            the collaboration map for which the event is fired, not
     *            <code>null</code>
     * @param change
     *            detail of the change, not <code>null</code>
     */
    public MapChangeEvent(CollaborationMap source, MapChange change) {
        super(source);
        Objects.requireNonNull(change, "Entry change must not be null");
        this.key = change.getKey();
        this.oldValue = change.getOldValue();
        this.value = change.getNewValue();
    }

    @Override
    public CollaborationMap getSource() {
        return (CollaborationMap) super.getSource();
    }

    /**
     * Gets the updated map key.
     *
     * @return the updated map key, not <code>null</code>
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the old value.
     *
     * @return the old map value, or <code>null</code> if no value was present
     *         previously
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new value.
     *
     * @return the new map value, or <code>null</code> if the association was
     *         removed
     */
    public Object getValue() {
        return value;
    }

}
