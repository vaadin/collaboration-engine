/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A linked list where each entry can be looked up using a generated key.
 *
 * @author Vaadin Ltd
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EntryList {
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class ListEntry {
        JsonNode value;
        UUID prev;
        UUID next;
        UUID revisionId;
        UUID scopeOwnerId;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class ListEntrySnapshot {
        final UUID id;
        final JsonNode value;
        final UUID prev;
        final UUID next;
        final UUID revisionId;
        final UUID scopeOwnerId;

        ListEntrySnapshot(UUID id, ListEntry entry) {
            this.id = id;
            this.value = entry.value;
            this.prev = entry.prev;
            this.next = entry.next;
            this.revisionId = entry.revisionId;
            this.scopeOwnerId = entry.scopeOwnerId;
        }

        @JsonCreator
        ListEntrySnapshot(@JsonProperty("id") UUID id,
                @JsonProperty("value") JsonNode value,
                @JsonProperty("prev") UUID prev,
                @JsonProperty("next") UUID next,
                @JsonProperty("revisionId") UUID revisionId,
                @JsonProperty("scopeOwnerId") UUID scopeOwnerId) {
            this.id = id;
            this.value = value;
            this.prev = prev;
            this.next = next;
            this.revisionId = revisionId;
            this.scopeOwnerId = scopeOwnerId;
        }
    }

    private final Map<UUID, ListEntry> entries = new HashMap<>();
    private UUID head;
    private UUID tail;

    void clear() {
        entries.clear();
        head = null;
        tail = null;
    }

    ListEntrySnapshot insertLast(UUID key, JsonNode value, UUID revisionId,
            UUID scopeOwnerId) {
        ListEntry item = new ListEntry();
        item.value = Objects.requireNonNull(value);
        item.revisionId = revisionId;
        item.scopeOwnerId = scopeOwnerId;
        item.prev = tail;

        entries.put(Objects.requireNonNull(key), item);
        setNext(tail, key);
        tail = key;

        return new ListEntrySnapshot(key, item);
    }

    Stream<ListEntrySnapshot> stream() {
        Builder<ListEntrySnapshot> builder = Stream.builder();

        UUID key = head;
        while (key != null) {
            ListEntry entry = entries.get(key);
            builder.add(new ListEntrySnapshot(key, entry));
            key = entry.next;
        }

        return builder.build();
    }

    JsonNode getValue(UUID key) {
        ListEntry item = entries.get(key);

        if (item == null) {
            return null;
        }

        return item.value;
    }

    ListEntrySnapshot getEntry(UUID key) {
        ListEntry entry = entries.get(key);
        if (entry == null) {
            return null;
        } else {
            return new ListEntrySnapshot(key, entry);
        }
    }

    void remove(UUID key) {
        ListEntry item = entries.remove(key);
        if (item != null) {
            setPrev(item.next, item.prev);
            setNext(item.prev, item.next);
        }
    }

    private void setNext(UUID target, UUID value) {
        if (target == null) {
            head = value;
        } else {
            entries.get(target).next = value;
        }
    }

    private void setPrev(UUID target, UUID value) {
        if (target == null) {
            tail = value;
        } else {
            entries.get(target).prev = value;
        }
    }

    void setValue(UUID key, JsonNode newValue, UUID revisionId,
            UUID scopeOwnerId) {
        ListEntry listEntry = entries.get(key);
        listEntry.value = newValue;
        listEntry.revisionId = revisionId;
        listEntry.scopeOwnerId = scopeOwnerId;
    }
}
