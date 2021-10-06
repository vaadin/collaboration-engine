/*
 * Copyright (C) 2021 Vaadin Ltd
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
 * Functional interface that defines how to handle user presence changes in a
 * topic.
 *
 * @see PresenceManager#setPresenceHandler(PresenceHandler)
 * @author Vaadin Ltd
 * @since 4.0
 */
@FunctionalInterface
public interface PresenceHandler extends Serializable {

    /**
     * The context of the user presence.
     */
    interface PresenceContext extends Serializable {

        /**
         * Gets the user.
         *
         * @return the user, not {@code null}
         */
        UserInfo getUser();
    }

    /**
     * Handles a change of user presence in a topic.
     *
     * @param context
     *            the context of the user presence, not {@code null}
     * @return a registration that will be removed when the user stops being
     *         present
     */
    Registration handlePresence(PresenceContext context);
}
