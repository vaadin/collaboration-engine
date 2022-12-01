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

import com.vaadin.flow.shared.Registration;

/**
 * Functional interface that defines how to handle a user when it becomes
 * present in a topic.
 *
 * @see PresenceManager#setNewUserHandler(NewUserHandler)
 * @author Vaadin Ltd
 * @since 3.2
 * @deprecated Use {@link PresenceHandler} instead
 */
@Deprecated
@FunctionalInterface
public interface NewUserHandler extends Serializable {

    /**
     * Handles a user when it becomes present in a topic.
     *
     * @param user
     *            the user that becomes present
     * @return a registration that will be removed when the user stops being
     *         present
     */
    Registration handleNewUser(UserInfo user);
}
