/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.function.Consumer;

/**
 * Response object when access to Collaboration Engine is requested for a user.
 *
 * @see CollaborationEngine#requestAccess(UserInfo, Consumer)
 *
 * @author Vaadin Ltd
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
     */
    public boolean hasAccess() {
        return hasAccess;
    }
}
