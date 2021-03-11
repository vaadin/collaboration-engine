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
