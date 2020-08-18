package com.vaadin.collaborationengine;

import static com.vaadin.collaborationengine.util.FieldHighlightUtil.getUserTags;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class AvatarGroupIT extends AbstractCollaborationFormIT {

    @Test
    public void openAndCloseClients_avatarsUpdated() {
        Assert.assertEquals(
                "Expected no avatars when only one client connected",
                Collections.emptyList(), client1.getAvatarNames());

        ClientState client2 = new ClientState(addClient());

        String message = "When another client has joined, expected both to have an avatar";
        Assert.assertEquals(message, Arrays.asList("User 2"),
                client1.getAvatarNames());
        Assert.assertEquals(message, Arrays.asList("User 1"),
                client2.getAvatarNames());

        ClientState client3 = new ClientState(addClient());

        message = "When three clients joined, expected to see the avatars of the other two";
        Assert.assertEquals(message, Arrays.asList("User 2", "User 3"),
                client1.getAvatarNames());
        Assert.assertEquals(message, Arrays.asList("User 1", "User 3"),
                client2.getAvatarNames());
        Assert.assertEquals(message, Arrays.asList("User 1", "User 2"),
                client3.getAvatarNames());

        close(client2.client);

        message = "When one of the three clients closed the window, expected one avatar to remain visible for the other two";
        Assert.assertEquals(message, Arrays.asList("User 3"),
                client1.getAvatarNames());
        Assert.assertEquals(message, Arrays.asList("User 1"),
                client3.getAvatarNames());
    }

    @Test
    public void avatarAndFieldHighlightHaveSameColorIndex() {
        ClientState client2 = new ClientState(addClient());

        client1.focusTextField();

        Integer fieldColorIndex = getUserTags(client2.textField).get(0)
                .getColorIndex();
        Integer avatarColorIndex = client2.avatars.getAvatarElement(0)
                .getPropertyInteger("colorIndex");

        Assert.assertNotNull(fieldColorIndex);
        Assert.assertEquals(fieldColorIndex, avatarColorIndex);
    }

}
