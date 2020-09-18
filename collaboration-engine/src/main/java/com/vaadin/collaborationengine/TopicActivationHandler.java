/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
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
     */
    void setActive(String topicId, boolean isActive);
}
