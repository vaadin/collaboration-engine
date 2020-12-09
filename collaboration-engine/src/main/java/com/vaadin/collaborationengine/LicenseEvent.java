/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.EventObject;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event that is fired during license lifecycle, e.g. when the grace period
 * starts/ends or when the license is expiring.
 *
 * @author Vaadin Ltd
 */
public class LicenseEvent extends EventObject {

    /**
     * The license event type.
     */
    public enum LicenseEventType {

        /**
         * An event of this type is fired when the grace period starts.
         */
        @JsonProperty("gracePeriodStarted")
        GRACE_PERIOD_STARTED,

        /**
         * An event of this type is fired when the grace period ends.
         */
        @JsonProperty("gracePeriodEnded")
        GRACE_PERIOD_ENDED,

        /**
         * An event of this type is fired when the license is expiring in less
         * than 31 days.
         */
        @JsonProperty("licenseExpiresSoon")
        LICENSE_EXPIRES_SOON,

        /**
         * An event of this type is fired when the license is expired.
         */
        @JsonProperty("licenseExpired")
        LICENSE_EXPIRED;
    }

    private final LicenseEventType type;

    /**
     * Creates a new license event.
     *
     * @param collaborationEngine
     *            the Collaboration Engine
     * @param type
     *            the type of the event
     */
    LicenseEvent(CollaborationEngine collaborationEngine,
            LicenseEventType type) {
        super(collaborationEngine);
        this.type = type;
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    public LicenseEventType getType() {
        return type;
    }

    @Override
    public CollaborationEngine getSource() {
        return (CollaborationEngine) super.getSource();
    }
}
