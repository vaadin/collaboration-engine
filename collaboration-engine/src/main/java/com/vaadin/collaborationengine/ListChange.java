/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.collaborationengine.Topic.ChangeDetails;

class ListChange implements ChangeDetails {
    private final String listName;

    private final ListChangeType type;
    private final UUID key;

    private final JsonNode oldValue;
    private final JsonNode value;

    private final UUID oldAfter;
    private final UUID after;

    private final UUID oldBefore;
    private final UUID before;

    private final UUID expectedId;

    ListChange(String listName, ListChangeType type, UUID key,
            JsonNode oldValue, JsonNode value, UUID oldAfter, UUID after,
            UUID oldBefore, UUID before, UUID expectedId) {
        this.listName = listName;
        this.type = type;
        this.key = key;
        this.oldValue = oldValue;
        this.value = value;
        this.oldBefore = oldBefore;
        this.before = before;
        this.oldAfter = oldAfter;
        this.after = after;
        this.expectedId = expectedId;
    }

    String getListName() {
        return listName;
    }

    ListChangeType getType() {
        return type;
    }

    UUID getKey() {
        return key;
    }

    JsonNode getOldValue() {
        return oldValue;
    }

    JsonNode getValue() {
        return value;
    }

    UUID getOldAfter() {
        return oldAfter;
    }

    UUID getAfter() {
        return after;
    }

    UUID getOldBefore() {
        return oldBefore;
    }

    UUID getBefore() {
        return before;
    }

    UUID getExpectedId() {
        return expectedId;
    }
}
