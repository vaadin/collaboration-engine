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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormTestCommon;

public class AvatarGroupTestCommon extends AbstractCollaborativeFormTestCommon {

    @Test
    public void openAndCloseClients_avatarsUpdated() throws Exception {
        Assert.assertEquals(
                "Expected only own avatar when only one client connected",
                newHashSet("User 1"), client1.getAvatarNames());

        ClientState client2 = new ClientState(addClient());

        String message = "When another client has joined, expected both to have two avatars";
        Set<String> expected = newHashSet("User 1", "User 2");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client2.getAvatarNames());

        ClientState client3 = new ClientState(addClient());

        message = "When three clients joined, expected to see the avatars of the other two";
        expected = newHashSet("User 1", "User 2", "User 3");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client2.getAvatarNames());
        Assert.assertEquals(message, expected, client3.getAvatarNames());

        close(client2.client);

        message = "When one of the three clients closed the window, expected one avatar to remain visible for the other two";
        expected = newHashSet("User 1", "User 3");
        Assert.assertEquals(message, expected, client1.getAvatarNames());
        Assert.assertEquals(message, expected, client3.getAvatarNames());
    }

    @Test
    public void avatarAndFieldHighlightHaveSameColorIndex() throws Exception {
        ClientState client2 = new ClientState(addClient());
        client1.focusTextField();

        // Remote driver needs a while until tags are there
        waitUntil(d -> getUserTags(client2.textField).size() > 0, 3);

        Integer fieldColorIndex = getUserTags(client2.textField).get(0)
                .getColorIndex();
        Integer avatarColorIndex = client2.avatars.getAvatarElement(1)
                .getPropertyInteger("colorIndex");

        Assert.assertNotNull(fieldColorIndex);
        Assert.assertEquals(fieldColorIndex, avatarColorIndex);
    }

    private <E> Set<E> newHashSet(E... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
