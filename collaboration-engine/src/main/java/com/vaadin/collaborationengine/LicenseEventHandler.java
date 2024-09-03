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
 * Event handler that gets notified on license events. The handler can be set
 * when creating an instance of {@link CollaborationEngineConfiguration}.
 *
 * @author Vaadin Ltd
 * @since 3.0
 * @deprecated any implementation of this interface won't receive any events,
 *             prefer using the default {@link CollaborationEngineConfiguration}
 *             constructor
 */
@Deprecated(since = "6.3", forRemoval = true)
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
