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
