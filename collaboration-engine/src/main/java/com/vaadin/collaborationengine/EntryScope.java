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
