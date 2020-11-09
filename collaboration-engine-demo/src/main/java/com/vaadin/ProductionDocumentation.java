package com.vaadin;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Code snippets used in licensing/production documentation.
 */
public class ProductionDocumentation extends VerticalLayout {

    private UserService userService;

    public ProductionDocumentation() {
        definitionOfEndUser();
        checkUserPermissions();
    }

    @WebServlet(initParams = {
            @WebInitParam(name = "ce.data.dir", value = "~/.vaadin/ce/") })
    public class MyServlet extends VaadinServlet {
    }

    private void definitionOfEndUser() {
        String userId = "steve@example.com";
        String name = "Steve";
        UserInfo userInfo = new UserInfo(userId, name);
        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                userInfo, "app");
        add(avatarGroup);
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
