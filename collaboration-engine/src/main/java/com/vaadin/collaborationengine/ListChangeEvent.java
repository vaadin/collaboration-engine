/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.EventObject;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Event that is fired when the value in a collaboration list changes.
 * 
 * @author Vaadin Ltd
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
