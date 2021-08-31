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
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Event that is fired when the value in a collaboration list changes.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public class ListChangeEvent extends EventObject {

    private final JsonNode addedItem;

    /**
     * Creates a new event.
     *
     * @param list
     *            the list source of the event
     * @param change
     *            the list change
     */
    ListChangeEvent(CollaborationList list, ListChange change) {
        super(list);
        addedItem = change.getAddedItem();
    }

    @Override
    public CollaborationList getSource() {
        return (CollaborationList) super.getSource();
    }

    /**
     * Gets the added item as an instance of the given class.
     *
     * @param type
     *            the class of the expected type of the returned instance
     * @param <T>
     *            the type of the class given as the <code>type</code> argument
     * @return the added item
     */
    public <T> Optional<T> getAddedItem(Class<T> type) {
        if (addedItem != null) {
            T item = JsonUtil.toInstance(addedItem, type);
            return Optional.of(item);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the added item as an instance of the given type reference.
     *
     * @param type
     *            the expected type reference of the returned instance
     * @param <T>
     *            the type of the reference given as the <code>type</code>
     *            argument
     * @return the added item
     */
    public <T> Optional<T> getAddedItem(TypeReference<T> type) {
        if (addedItem != null) {
            T item = JsonUtil.toInstance(addedItem, type);
            return Optional.of(item);
        } else {
            return Optional.empty();
        }
    }
}
