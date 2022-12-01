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
 * A listener of {@link MembershipEvent}. The listener will be notified of
 * events dispatched when a node joins or leaves the backend.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface MembershipListener {

    /**
     * Handles a membership event.
     *
     * @param event
     *            the event, not <code>null</code>
     */
    void handleMembershipEvent(MembershipEvent event);
}
