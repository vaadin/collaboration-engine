/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

/**
 * Event handler that gets notified for changes to collaboration maps. A
 * listener can be registered using
 * {@link CollaborationMap#subscribe(MapSubscriber)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface MapSubscriber {
    /**
     * Handles a map change event.
     *
     * @param event
     *            the map change event, not <code>null</code>
     *
     * @since 1.0
     */
    void onMapChange(MapChangeEvent event);
}
