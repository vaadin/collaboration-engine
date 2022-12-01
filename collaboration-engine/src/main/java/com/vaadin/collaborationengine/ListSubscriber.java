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
 * Event handler that gets notified for changes to collaboration lists. A
 * listener can be registered using
 * {@link CollaborationList#subscribe(ListSubscriber)}.
 *
 * @author Vaadin Ltd
 * @since 3.1
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
