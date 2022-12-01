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
 * @author Vaadin Ltd
 * @since 3.2
 */
final class MessageUtil {
    private MessageUtil() {
        // Only static helpers
    }

    /**
     * Messages to be used with
     * {@link java.util.Objects#requireNonNull(Object, String)}
     */
    static final class Required {
        static final String MESSAGE_TEMPLATE = " cannot be null";
        static final String KEY = "Key" + MESSAGE_TEMPLATE;

        private Required() {
            // Only static constants
        }
    }
}
