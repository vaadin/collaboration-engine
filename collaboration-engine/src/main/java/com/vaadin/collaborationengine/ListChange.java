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
 * A single change that is emitted from the list data of a {@link Topic}.
 *
 * @author Vaadin Ltd
 */
class ListChange {

    private final String listName;

    private final JsonNode addedItem;

    /**
     * Creates a single list change.
     *
     * @param addedItem
     *            the item added to the list
     */
    ListChange(String listName, JsonNode addedItem) {
        this.listName = listName;
        this.addedItem = addedItem;
    }

    /**
     * Gets the name of the changed list.
     *
     * @return the list name
     */
    public String getListName() {
        return listName;
    }

    /**
     * Gets the item added to the list.
     * 
     * @return the added item
     */
    public JsonNode getAddedItem() {
        return addedItem;
    }
}
