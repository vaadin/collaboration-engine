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

import java.text.MessageFormat;
import java.util.EventObject;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event that is fired during license lifecycle, e.g. when the grace period
 * starts/ends or when the license is expiring.
 *
 * @author Vaadin Ltd
 * @since 3.0
 * @deprecated license events will not be received by any listener
 */
@Deprecated(since = "6.3", forRemoval = true)
public class LicenseEvent extends EventObject {

    /**
     * The license event type.
     * 
     * @deprecated license events will not be received by any listener
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public enum LicenseEventType {

        /**
         * An event of this type is fired when the grace period starts.
         */
        @JsonProperty("gracePeriodStarted")
        GRACE_PERIOD_STARTED(
                "The Vaadin Collaboration Engine license end-user quota "
                        + "has exceeded. Collaboration Engine has started a "
                        + "30 day grace period ending on {0}, during which the "
                        + "quota is ten times bigger. This grace period gives "
                        + "time to react to the exceeding limit without impacting "
                        + "the user experience. Contact a Vaadin sales "
                        + "representative to obtain a license that fits the "
                        + "application needs."),

        /**
         * An event of this type is fired when the grace period ends.
         */
        @JsonProperty("gracePeriodEnded")
        GRACE_PERIOD_ENDED(
                "The Vaadin Collaboration Engine grace period has ended. This "
                        + "means that the licensed end-user quota will be "
                        + "enforced to its original value and exceeding requests "
                        + "to access Collaboration Engine will be denied. "
                        + "Contact a Vaadin sales representative to obtain a "
                        + "license that fits the application needs."),

        /**
         * An event of this type is fired when the license is expiring in less
         * than 31 days.
         */
        @JsonProperty("licenseExpiresSoon")
        LICENSE_EXPIRES_SOON(
                "The Vaadin Collaboration Engine license will expire on {0}. "
                        + "Once the license is expired, collaborative features "
                        + "won't be accessible to the end-users until a new "
                        + "license is obtained. Check the license expiration "
                        + "date and contact a Vaadin sales representative to "
                        + "renew before it expires."),

        /**
         * An event of this type is fired when the license is expired.
         */
        @JsonProperty("licenseExpired")
        LICENSE_EXPIRED(
                "The Vaadin Collaboration Engine license has expired. This means "
                        + "that collaborative features are not accessible to "
                        + "the end-users until a new license is obtained. "
                        + "Contact a Vaadin sales representative to renew the "
                        + "license and restore collaborative features.");

        private final String messageTemplate;

        private LicenseEventType(String messageTemplate) {
            this.messageTemplate = messageTemplate;
        }

        /**
         * Creates the message describing this event-type.
         *
         * @param args
         *            the message arguments
         * @return the event-type message
         */
        String createMessage(Object... args) {
            return MessageFormat.format(messageTemplate, args);
        }
    }

    private final LicenseEventType type;

    private final String message;

    /**
     * Creates a new license event.
     *
     * @param collaborationEngine
     *            the Collaboration Engine
     * @param type
     *            the type of the event
     * @param message
     *            the event message
     */
    LicenseEvent(CollaborationEngine collaborationEngine, LicenseEventType type,
            String message) {
        super(collaborationEngine);
        this.type = type;
        this.message = message;
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    public LicenseEventType getType() {
        return type;
    }

    /**
     * Gets the message describing the event.
     *
     * @return the message describing the event
     */
    public String getMessage() {
        return message;
    }

    @Override
    public CollaborationEngine getSource() {
        return (CollaborationEngine) super.getSource();
    }
}
