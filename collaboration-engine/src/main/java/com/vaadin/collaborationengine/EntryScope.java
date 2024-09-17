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

/**
 * The scope of data in a topic.
 *
 * @author Vaadin Ltd
 * @since 4.0
 */
public enum EntryScope {

    /**
     * This is the default scope and entries with this scope will be removed
     * only if explicitly requested.
     */
    TOPIC,

    /**
     * Entries with this scope will be automatically removed once the connection
     * to the topic which created them is deactivated.
     */
    CONNECTION;
}
