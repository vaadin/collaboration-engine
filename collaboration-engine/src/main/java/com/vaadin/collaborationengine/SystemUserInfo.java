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
 * A {@link UserInfo} for non-interaction thread. A system user is immutable.
 * Its color index is always 0 and not registered to users' color index map in
 * Collaboration Engine.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SystemUserInfo extends UserInfo {

    private static final String USER_ID = "<{system-user}>";
    private static final SystemUserInfo instance = new SystemUserInfo();

    private SystemUserInfo() {
        super(USER_ID, 0);
    }

    /**
     * Gets the unique instance of system user info.
     *
     * @return the system user info instance, not {@code null}
     *
     * @since 1.0
     */
    public static SystemUserInfo getInstance() {
        return instance;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }

    @Override
    public void setAbbreviation(String abbreviation) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }

    @Override
    public void setImage(String imageUrl) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }

    @Override
    public void setColorIndex(int colorIndex) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }
}
