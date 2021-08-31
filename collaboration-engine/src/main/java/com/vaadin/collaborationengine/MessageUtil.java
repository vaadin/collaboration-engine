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
