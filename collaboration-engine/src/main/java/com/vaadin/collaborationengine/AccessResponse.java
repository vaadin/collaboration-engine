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

import java.util.function.Consumer;

/**
 * Response object when access to Collaboration Engine is requested for a user.
 *
 * @see CollaborationEngine#requestAccess(UserInfo, Consumer)
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public class AccessResponse {

    private final boolean hasAccess;

    AccessResponse(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

    /**
     * Gets the info of whether the user has access to Collaboration Engine or
     * not.
     *
     * @return {@code true} if the user has access, {@code false} otherwise.
     *
     * @since 3.0
     */
    public boolean hasAccess() {
        return hasAccess;
    }
}
