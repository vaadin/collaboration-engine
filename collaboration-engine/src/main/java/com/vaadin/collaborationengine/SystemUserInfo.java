/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

/**
 * A {@link UserInfo} for non-interaction thread. A system user is immutable.
 * Its color index is always 0 and not registered to users' color index map in
 * Collaboration Engine.
 *
 * @author Vaadin Ltd
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
     */
    public static SystemUserInfo get() {
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
    public void setImage(String image) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }

    @Override
    public void setColorIndex(int colorIndex) {
        throw new UnsupportedOperationException(
                "The system user cannot be modified.");
    }
}
