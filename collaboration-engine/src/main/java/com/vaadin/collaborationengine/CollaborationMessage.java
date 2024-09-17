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

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Message of a {@link CollaborationMessageList}.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public class CollaborationMessage implements Serializable {
    private UserInfo user;
    private String text;
    private Instant time;

    /**
     * Creates a new message.
     */
    public CollaborationMessage() {
        // Needed for Jackson deserialization
    }

    /**
     * Creates a new message with the specified {@code user} as the message
     * author info, {@code text} as the message content and {@code time} as the
     * message timestamp.
     *
     * @param user
     *            the user-info of the message author
     * @param text
     *            the content of the message
     * @param time
     *            the timestamp of the message
     */
    public CollaborationMessage(UserInfo user, String text, Instant time) {
        this.user = user;
        this.text = text;
        this.time = time;
    }

    /**
     * Gets the message author user-info.
     *
     * @return the user-info
     */
    public UserInfo getUser() {
        return user;
    }

    /**
     * Sets the message author user-info.
     *
     * @param user
     *            the user-info
     */
    public void setUser(UserInfo user) {
        this.user = user;
    }

    /**
     * Gets the message content.
     *
     * @return the message content
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the message content.
     *
     * @param text
     *            the message content
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the message timestamp.
     *
     * @return the message timestamp
     */
    public Instant getTime() {
        return time;
    }

    /**
     * Sets the message timestamp.
     *
     * @param time
     *            the message timestamp
     */
    public void setTime(Instant time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CollaborationMessage that = (CollaborationMessage) o;
        return Objects.equals(user, that.user)
                && Objects.equals(text, that.text)
                && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, text, time);
    }
}
