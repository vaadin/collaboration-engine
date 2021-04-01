/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

/**
 * Event handler that gets notified for changes to collaboration lists. A
 * listener can be registered using
 * {@link CollaborationList#subscribe(ListSubscriber)}.
 * 
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface ListSubscriber {

    /**
     * Handles a list change event.
     *
     * @param event
     *            the list change event, not <code>null</code>
     */
    void onListChange(ListChangeEvent event);
}
