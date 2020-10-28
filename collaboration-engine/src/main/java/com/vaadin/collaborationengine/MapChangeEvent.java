/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.EventObject;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Event that is fired when the value in a collaboration map changes.
 *
 * @author Vaadin Ltd
 */
public class MapChangeEvent extends EventObject {

    private final String key;
    private final JsonNode oldValue;
    private final JsonNode value;

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
        this.value = change.getValue();
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
     * Gets the old value as an instance of the given class.
     *
     * @param type
     *            the expected type of the returned instance
     * @param <T>
     *            the type of the value from <code>type</code> parameter, e.g.
     *            <code>String</code>
     * @return the old map value, or <code>null</code> if no value was present
     *         previously
     */
    public <T> T getOldValue(Class<T> type) {
        return JsonUtil.toInstance(oldValue, type);
    }

    /**
     * Gets the old value as an instance corresponding to the given type
     * reference.
     *
     * @param typeRef
     *            the expected type reference of the returned instance
     * @param <T>
     *            the type reference of the value from <code>typeRef</code>
     *            parameter, e.g. <code>List<String>></code>
     * @return the old map value, or <code>null</code> if no value was present
     *         previously
     */
    public <T> T getOldValue(TypeReference<T> typeRef) {
        return JsonUtil.toInstance(oldValue, typeRef);
    }

    /**
     * Gets the new value as an instance of the given class.
     *
     * @param type
     *            the expected type of the returned instance
     * @param <T>
     *            the type of the value from <code>type</code> parameter, e.g.
     *            <code>String</code>
     * @return the new map value, or <code>null</code> if the association was
     *         removed
     */
    public <T> T getValue(Class<T> type) {
        return JsonUtil.toInstance(value, type);
    }

    /**
     * Gets the new value as an instance corresponding to the given type
     * reference.
     *
     * @param typeRef
     *            the expected type reference of the returned instance
     * @param <T>
     *            the type reference of the value from `typeRef` parameter, e.g.
     *            <code>List<String>></code>
     * @return the new map value, or <code>null</code> if the association was
     *         removed
     */
    public <T> T getValue(TypeReference<T> typeRef) {
        return JsonUtil.toInstance(value, typeRef);
    }

}
