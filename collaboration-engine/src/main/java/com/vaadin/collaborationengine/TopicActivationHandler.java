/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
