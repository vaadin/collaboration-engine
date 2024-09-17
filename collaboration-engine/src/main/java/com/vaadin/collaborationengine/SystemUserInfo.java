/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
