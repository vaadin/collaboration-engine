/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.Instant;

class CollaborationMessageListItem {
    private UserInfo user;
    private String text;
    private Instant time;

    public CollaborationMessageListItem() {
        // Needed for Jackson deserialization
    }

    public CollaborationMessageListItem(UserInfo user, String text,
            Instant time) {
        this.user = user;
        this.text = text;
        this.time = time;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }
}
