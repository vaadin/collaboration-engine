package com.vaadin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

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

    @SpringComponent
    public static class CollaborationEngineConfiguration {

        private static final Logger LOGGER = LoggerFactory
                .getLogger(CollaborationEngineConfiguration.class);

        public CollaborationEngineConfiguration() {
            System.setProperty("vaadin.ce.dataDir",
                    "/Users/steve/vaadin/collaboration-engine/");
            CollaborationEngine.getInstance().setLicenseEventHandler(event -> {
                LOGGER.error(event.getMessage());
            });
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
        UserInfo userInfo = new UserInfo("steve@example.com", "Steve");
        CollaborationEngine.getInstance().requestAccess(userInfo, response -> {
            component.setVisible(response.hasAccess());
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
