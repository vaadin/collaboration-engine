/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
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
