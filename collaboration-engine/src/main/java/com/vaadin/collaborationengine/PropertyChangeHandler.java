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

import java.io.Serializable;

/**
 * Functional interface that defines how to handle value changes for properties
 * in a topic.
 *
 * @see FormManager#setPropertyChangeHandler(PropertyChangeHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface PropertyChangeHandler extends Serializable {
    /**
     * The property change event.
     */
    interface PropertyChangeEvent {
        /**
         * Gets the property name.
         *
         * @return the property name, not {@code null}
         */
        String getPropertyName();

        /**
         * Gets the new value.
         *
         * @return the value, not {@code null}
         */
        Object getValue();
    }

    /**
     * Handles a change of value for a property in a topic.
     *
     * @param event
     *            the property change event, not {@code null}
     */
    void handlePropertyChange(PropertyChangeEvent event);
}
