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
 * Functional interface that defines how to handle value changes for properties
 * in a topic.
 *
 * @see FormManager#setPropertyChangeHandler(PropertyChangeHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface PropertyChangeHandler {
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
