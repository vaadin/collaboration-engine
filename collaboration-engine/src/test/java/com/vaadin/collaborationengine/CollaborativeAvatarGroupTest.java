/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.ReflectionUtils;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;

public class CollaborativeAvatarGroupTest {

    private static final String TOPIC_ID = "topic";
    private static final String TOPIC_ID_2 = "topic2";

    private static class Client {
        final UI ui;
        final UserInfo user;
        final CollaborativeAvatarGroup group;

        Client(int index) {
            this(index, TOPIC_ID);
        }

        Client(int index, String topicId) {
            this.ui = new MockUI();
            this.user = new UserInfo();
            user.setName("name" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setImage("image" + index);
            user.setColorIndex(index);
            group = new CollaborativeAvatarGroup(user, topicId);
        }

        void attach() {
            ui.add(group);
        }

        void detach() {
            ui.remove(group);
        }

        void cleanUp() {
            ui.removeAll();
        }

        List<AvatarGroupItem> getItems() {
            // TODO Remove try-catch after AvatarGroup::getItems NPE fixed:
            // https://github.com/vaadin/vaadin-avatar-flow/issues/16
            try {
                return group.getContent().getItems();
            } catch (NullPointerException e) {
                return Collections.emptyList();
            }
        }

        List<String> getItemNames() {
            return group.getContent().getItems().stream()
                    .map(AvatarGroupItem::getName).collect(Collectors.toList());
        }
    }

    private Client client1;
    private Client client2;
    private Client client3;

    private Client clientInOtherTopic;

    @Before
    public void init() {
        client1 = new Client(1);
        client2 = new Client(2);
        client3 = new Client(3);
        clientInOtherTopic = new Client(4, TOPIC_ID_2);
    }

    @After
    public void cleanUp() {
        Stream.of(client1, client2, client3, clientInOtherTopic)
                .forEach(Client::cleanUp);
        TestUtils.clearMap(TOPIC_ID, CollaborativeAvatarGroup.MAP_NAME,
                CollaborativeAvatarGroup.KEY);
        TestUtils.clearMap(TOPIC_ID_2, CollaborativeAvatarGroup.MAP_NAME,
                CollaborativeAvatarGroup.KEY);
    }

    @Test
    public void noInitialAvatar() {
        Assert.assertEquals(Collections.emptyList(), client1.getItems());
    }

    @Test
    public void attach_ownAvatarNotDisplayed() {
        client1.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getItems());
    }

    @Test
    public void attachTwoGroups_othersAvatarDisplayed() {
        client1.attach();
        client2.attach();
        Assert.assertEquals(Arrays.asList("name2"), client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name1"), client2.getItemNames());
    }

    @Test
    public void userPropertiesPropagatedToAvatarGroupItems() {
        client1.attach();
        client2.attach();
        AvatarGroupItem item = client2.getItems().get(0);

        Assert.assertEquals("name1", item.getName());
        Assert.assertEquals("abbreviation1", item.getAbbreviation());
        Assert.assertEquals("image1", item.getImage());
        Assert.assertEquals(1, item.getColorIndex().intValue());
    }

    @Test
    public void detach_avatarRemovedInOtherClient() {
        client1.attach();
        client2.attach();

        client1.detach();
        Assert.assertEquals(Collections.emptyList(), client2.getItems());
    }

    @Test
    public void threeUsers_attach_detach_avatarsUpdated() {
        client1.attach();
        client2.attach();
        client3.attach();

        Assert.assertEquals(Arrays.asList("name2", "name3"),
                client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name1", "name3"),
                client2.getItemNames());
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client3.getItemNames());

        client2.detach();

        Assert.assertEquals(Arrays.asList("name3"), client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name1"), client3.getItemNames());
    }

    @Test
    public void groupsWithDifferentTopicIds_avatarsNotUpdated() {
        client1.attach();
        clientInOtherTopic.attach();

        Assert.assertEquals(Collections.emptyList(), client1.getItems());
        Assert.assertEquals(Collections.emptyList(),
                clientInOtherTopic.getItems());
    }

    @Test
    public void detach_localAvatarsCleared() {
        client1.attach();
        client2.attach();
        client3.attach();

        client1.detach();

        Assert.assertEquals(Collections.emptyList(), client1.getItems());
    }

    @Test
    public void detach_reattach_avatarsUpdated() {
        client1.attach();
        client2.attach();
        client3.attach();

        client1.detach();
        client1.attach();

        Assert.assertEquals(Arrays.asList("name2", "name3"),
                client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name3", "name1"),
                client2.getItemNames());
    }

    private static List<String> blackListedMethods = Arrays.asList("setItems",
            "getItems", "add", "remove");

    @Test
    public void avatarGroup_replicateRelevantAPIs() {
        List<String> avatarGroupMethods = ReflectionUtils
                .getMethodNames(AvatarGroup.class);
        List<String> collaborativeAvatarGroupMethods = ReflectionUtils
                .getMethodNames(CollaborativeAvatarGroup.class);

        List<String> missingMethods = avatarGroupMethods.stream()
                .filter(m -> !blackListedMethods.contains(m)
                        && !collaborativeAvatarGroupMethods.contains(m))
                .collect(Collectors.toList());

        if (!missingMethods.isEmpty()) {
            Assert.fail("Missing wrapper for methods: "
                    + missingMethods.toString());
        }
    }

}
