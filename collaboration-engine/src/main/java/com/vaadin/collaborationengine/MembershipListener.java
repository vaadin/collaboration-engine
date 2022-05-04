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
