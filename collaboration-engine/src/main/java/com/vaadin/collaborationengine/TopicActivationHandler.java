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
 * Defines when to execute a callback based on topic activation.
 * <p>
 * A topic is not active when there is no activated {@link TopicConnection}
 * connecting to it locally. Otherwise, a topic is activated when its first
 * active topic connection is opened.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
interface TopicActivationHandler {

    /**
     * Activates or deactivates a topic based on the number of its active topic
     * connections.
     *
     * @param topicId
     *            the topic id
     * @param isActive
     *            {@code true} if the first topic connection was activated,
     *            {@code false} if the last topic connection was deactivated
     *
     * @since 1.0
     */
    void setActive(String topicId, boolean isActive);
}
