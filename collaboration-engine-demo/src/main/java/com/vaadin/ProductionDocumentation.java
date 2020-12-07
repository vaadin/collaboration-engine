package com.vaadin;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Code snippets used in licensing/production documentation.
 */
public class ProductionDocumentation extends VerticalLayout {

    private UserService userService;

    public ProductionDocumentation() {
        definitionOfEndUser();
        requestAccess();
        checkUserPermissions();
    }

    public class MyVaadinInitListener implements VaadinServiceInitListener {
        @Override
        public void serviceInit(ServiceInitEvent event) {
            System.setProperty("vaadin.ce.dataDir",
                    "/Users/steve/vaadin/collaboration-engine/");
        }
    }

    private void definitionOfEndUser() {
        String userId = "steve@example.com";
        String name = "Steve";
        UserInfo userInfo = new UserInfo(userId, name);
        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                userInfo, "app");
        add(avatarGroup);
    }

    private void requestAccess() {
        Component component = new Div();

        //@formatter:off
        UI ui = UI.getCurrent();
        UserInfo userInfo = new UserInfo("steve@example.com", "Steve");
        CollaborationEngine.getInstance().requestAccess(ui, userInfo, hasAccess -> {
            component.setVisible(hasAccess);
        });
        //@formatter:on
    }

    private void checkUserPermissions() {
        User userEntity = userService.getCurrentUser();
        if (userEntity.getRoles().contains(Role.ADMIN)) {
            UserInfo userInfo = new UserInfo(userEntity.getId(),
                    userEntity.getName(), userEntity.getImageUrl());

            CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                    userInfo, "avatars");

            add(avatarGroup);
        }
    }
}
