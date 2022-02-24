/*
 * Copyright 2020-2022 Vaadin Ltd.
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

    private final UUID oldPrev;
    private final UUID prev;

    private final UUID oldNext;
    private final UUID next;

    private final UUID expectedId;

    ListChange(String listName, ListChangeType type, UUID key,
            JsonNode oldValue, JsonNode value, UUID oldPrev, UUID prev,
            UUID oldNext, UUID next, UUID expectedId) {
        this.listName = listName;
        this.type = type;
        this.key = key;
        this.oldValue = oldValue;
        this.value = value;
        this.oldPrev = oldPrev;
        this.prev = prev;
        this.oldNext = oldNext;
        this.next = next;
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

    UUID getOldNext() {
        return oldNext;
    }

    UUID getNext() {
        return next;
    }

    UUID getOldPrev() {
        return oldPrev;
    }

    UUID getPrev() {
        return prev;
    }

    UUID getExpectedId() {
        return expectedId;
    }
}
