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

import com.vaadin.collaborationengine.Topic.ChangeDetails;

class ListChange implements ChangeDetails {
    private final String listName;
    private final JsonNode addedItem;

    ListChange(String listName, JsonNode addedItem) {
        this.listName = listName;
        this.addedItem = addedItem;
    }

    String getListName() {
        return listName;
    }

    JsonNode getAddedItem() {
        return addedItem;
    }
}
