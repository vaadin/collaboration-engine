/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Objects;
import java.util.UUID;

/**
 * An object which identifies each item of a {@link CollaborationList}.
 *
 * @author Vaadin Ltd
 */
public class ListKey {

    private final UUID key;

    ListKey(UUID key) {
        this.key = Objects.requireNonNull(key);
    }

    /**
     * Gets the unique value of this key.
     *
     * @return the unique value of this key, not <code>null</code>
     */
    public UUID getKey() {
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ListKey) {
            ListKey that = (ListKey) obj;
            return key.equals(that.key);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Creates the {@link ListKey} for the provided <code>key</code> parameter.
     *
     * @param key
     *            the key
     * @return the {@link ListKey}, or <code>null</code> if <code>key</code> was
     *         null
     */
    public static ListKey of(UUID key) {
        if (key == null) {
            return null;
        } else {
            return new ListKey(key);
        }
    }
}
