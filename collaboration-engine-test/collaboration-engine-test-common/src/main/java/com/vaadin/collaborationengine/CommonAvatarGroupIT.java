package com.vaadin.collaborationengine;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormIT;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CommonAvatarGroupIT extends AbstractCollaborativeFormIT {

    @Test
    public void openAndCloseClients_avatarsUpdated() {
        Assert.assertEquals(
                "Expected only own avatar when only one client connected",
                Arrays.asList("User 1"), client1.getAvatarNames());

        ClientState client2 = new ClientState(addClient());

        String message = "When another client has joined, expected both to have two avatars";
        List<String> expected = Arrays.asList("User 1", "User 2");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client2.getAvatarNames());

        ClientState client3 = new ClientState(addClient());

        message = "When three clients joined, expected to see the avatars of the other two";
        expected = Arrays.asList("User 1", "User 2", "User 3");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client2.getAvatarNames());
        Assert.assertEquals(message, expected, client3.getAvatarNames());

        close(client2.client);

        message = "When one of the three clients closed the window, expected one avatar to remain visible for the other two";
        expected = Arrays.asList("User 1", "User 3");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client3.getAvatarNames());
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
