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
 * Event handler that gets notified for changes to collaboration maps. A
 * listener can be registered using
 * {@link CollaborationMap#subscribe(MapSubscriber)}.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface MapSubscriber {
    /**
     * Handles a map change event.
     *
     * @param event
     *            the map change event, not <code>null</code>
     */
    void onMapChange(MapChangeEvent event);
}
