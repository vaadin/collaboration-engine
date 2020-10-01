package com.vaadin;

import java.io.ByteArrayInputStream;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

/**
 * Code snippets used in CollaborationAvatarGroup's reference documentation.
 */
public class AvatarGroupDocumentation extends VerticalLayout {

    private CollaborationAvatarGroup avatarGroup;
    private UserService userService;

    public AvatarGroupDocumentation() {
        User userEntity = userService.getCurrentUser();
        UserInfo userInfo = new UserInfo(userEntity.getId());

        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                userInfo);
        avatarGroup.setTopic("avatars");
        add(avatarGroup);

        Avatar ownAvatar = new Avatar();
        ownAvatar.setName(userEntity.getName());
        ownAvatar.setImage(userEntity.getImageUrl());

        add(ownAvatar);
    }

    private void imageProvider() {
        avatarGroup.setImageProvider(userInfo -> {
            StreamResource streamResource = new StreamResource(
                    "avatar_" + userInfo.getId(), () -> {
                        User userEntity = userService
                                .findById(userInfo.getId());
                        byte[] imageBytes = userEntity.getImage();
                        return new ByteArrayInputStream(imageBytes);
                    });
            streamResource.setContentType("image/png");
            return streamResource;
        });
    }

    private void personSelected(long personId) {
        avatarGroup.setTopic("person/" + personId);
    }
}
