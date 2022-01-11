/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.UUID;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("system")
public class SystemContextViewCommon extends VerticalLayout {
    public SystemContextViewCommon() {
        UserInfo localUser = new UserInfo(UUID.randomUUID().toString());
        add(new Span(
                "If this looks empty, it's because no messages have been submitted. "
                        + "To add a message, send a POST request to /submit with the message text as the POST body."),
                new CollaborationMessageList(localUser,
                        SystemContextViewCommon.class.getName()));
    }
}
