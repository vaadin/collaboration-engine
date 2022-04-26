/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */

package com.vaadin.collaborationengine;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Functional interface that defines how to handle highlight changes for
 * properties in a topic.
 *
 * @see FormManager#setHighlightHandler(HighlightHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface HighlightHandler {
    /**
     * The context of the highlight.
     */
    interface HighlightContext extends Serializable {
        /**
         * Gets the user.
         *
         * @return the user, not {@code null}
         */
        UserInfo getUser();

        /**
         * Gets the property name.
         *
         * @return the property name, not {@code null}
         */
        String getPropertyName();

        /**
         * Gets the field index.
         *
         * @return the field index
         */
        int getFieldIndex();
    }

    /**
     * Handles a change of highlight for a property in a topic.
     *
     * @param context
     *            the context of the highlight, not {@code null}
     * @return a registration that will be removed with the highlight is toggled
     *         off
     */
    Registration handleHighlight(HighlightContext context);
}
