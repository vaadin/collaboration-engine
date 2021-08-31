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
 * Event handler that gets notified on license events. A listener can be set
 * using
 * {@link CollaborationEngine#setLicenseEventHandler(LicenseEventHandler)}.
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
@FunctionalInterface
public interface LicenseEventHandler {

    /**
     * Handles a license event.
     *
     * @param event
     *            the license event, not {@code null}
     */
    void handleLicenseEvent(LicenseEvent event);
}
