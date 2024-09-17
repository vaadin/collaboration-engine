/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * An object which identifies each item of a {@link CollaborationList}.
 *
 * @author Vaadin Ltd
 */
public class ListKey implements Serializable {

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
