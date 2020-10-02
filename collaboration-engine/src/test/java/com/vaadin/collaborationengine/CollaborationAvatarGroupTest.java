package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
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
import com.vaadin.flow.server.StreamResource;

public class CollaborationAvatarGroupTest {

    private static class TestStreamResource extends StreamResource {
        public TestStreamResource(String name) {
            super(name, () -> null);
        }
    }

    private static final String TOPIC_ID = "topic";
    private static final String TOPIC_ID_2 = "topic2";

    private static class Client {
        final UI ui;
        final UserInfo user;
        CollaborationAvatarGroup group;

        Client(int index) {
            this(index, TOPIC_ID);
        }

        Client(int index, String topicId) {
            this.ui = new MockUI();
            this.user = new UserInfo(UUID.randomUUID().toString());
            user.setName("name" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setImage("image" + index);
            user.setColorIndex(index);
            group = new CollaborationAvatarGroup(user, topicId);
        }

        void attach() {
            ui.add(group);
        }

        void detach() {
            ui.remove(group);
        }

        void setGroupTopic(String topicId) {
            group.setTopic(topicId);
        }

        void cleanUp() {
            ui.removeAll();
        }

        List<AvatarGroupItem> getItems() {
            return group.getContent().getItems();
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
        TestUtils.clearMap(TOPIC_ID, CollaborationAvatarGroup.MAP_NAME,
                CollaborationAvatarGroup.KEY);
        TestUtils.clearMap(TOPIC_ID_2, CollaborationAvatarGroup.MAP_NAME,
                CollaborationAvatarGroup.KEY);

        UI.setCurrent(null);
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

    @Test
    public void setTopic_closeExistingConnection() {
        client1.attach();
        client2.attach();

        client1.setGroupTopic("new topic");
        client3.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getItemNames());
    }

    @Test
    public void setTopic_showAvatarsFromNewTopic() {
        client1.attach();
        client2.attach();

        Client newClient = new Client(9, "new topic");
        newClient.attach();

        client1.setGroupTopic("new topic");
        Assert.assertEquals(Arrays.asList(newClient.user.getName()),
                client1.getItemNames());

        Client newClient1 = new Client(10, "new topic");
        newClient1.attach();
        Assert.assertEquals(Arrays.asList(newClient.user.getName(),
                newClient1.user.getName()), client1.getItemNames());
    }

    @Test
    public void setTopic_nullTopic_closeConnectionAndRemoveAvatars() {
        client1.attach();
        client2.attach();

        client1.setGroupTopic(null);
        client3.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getItemNames());
    }

    @Test
    public void nullTopic_setTopic_avatarsUpdated() {
        client2.attach();
        client3.attach();

        client1.group = new CollaborationAvatarGroup(new UserInfo("userid"),
                null);
        client1.setGroupTopic(TOPIC_ID);
        client1.attach();
        Assert.assertEquals(Arrays.asList("name2", "name3"),
                client1.getItemNames());
    }

    private static List<String> blackListedMethods = Arrays.asList("setItems",
            "getItems", "add", "remove");

    @Test
    public void avatarGroup_replicateRelevantAPIs() {
        List<String> avatarGroupMethods = ReflectionUtils
                .getMethodNames(AvatarGroup.class);
        List<String> collaborationAvatarGroupMethods = ReflectionUtils
                .getMethodNames(CollaborationAvatarGroup.class);

        List<String> missingMethods = avatarGroupMethods.stream()
                .filter(m -> !blackListedMethods.contains(m)
                        && !collaborationAvatarGroupMethods.contains(m))
                .collect(Collectors.toList());

        if (!missingMethods.isEmpty()) {
            Assert.fail("Missing wrapper for methods: "
                    + missingMethods.toString());
        }
    }

    @Test
    public void collaborationMapValueEncodedAsString() {
        client1.attach();
        AtomicBoolean done = new AtomicBoolean(false);
        TestUtils.openEagerConnection(TOPIC_ID, topicConnection -> {
            Object mapValue = topicConnection
                    .getNamedMap(CollaborationAvatarGroup.MAP_NAME)
                    .get(CollaborationAvatarGroup.KEY);
            Assert.assertThat(mapValue, CoreMatchers.instanceOf(String.class));
            Assert.assertThat((String) mapValue,
                    CoreMatchers.containsString(client1.user.getId()));
            done.set(true);
        });
        Assert.assertTrue("Topic connection callback has not run", done.get());
    }

    @Test
    public void imageProvider_beforeAttach_streamResourceIsUsed() {
        UI.setCurrent(client1.ui);
        client1.group.setImageProvider(
                user -> new TestStreamResource(user.getName()));
        client1.attach();
        client2.attach();

        List<AvatarGroupItem> items = client1.getItems();
        Assert.assertEquals(1, items.size());
        AvatarGroupItem item = items.get(0);

        Assert.assertThat(item.getImage(),
                CoreMatchers.startsWith("VAADIN/dynamic"));
        Assert.assertEquals("name2", item.getImageResource().getName());
    }

    @Test
    public void imageProvider_afterAttach_streamResourceIsUsed() {
        UI.setCurrent(client1.ui);
        client1.attach();
        client2.attach();

        client1.group.setImageProvider(
                user -> new TestStreamResource(user.getName()));

        List<AvatarGroupItem> items = client1.getItems();
        Assert.assertEquals(1, items.size());

        AvatarGroupItem item = items.get(0);

        Assert.assertThat(item.getImage(),
                CoreMatchers.startsWith("VAADIN/dynamic"));
        Assert.assertEquals("name2", item.getImageResource().getName());
    }

    @Test
    public void imageProvider_nullStream_noImage() {
        UI.setCurrent(client1.ui);
        client1.attach();
        client2.attach();

        client1.group.setImageProvider(user -> null);

        List<AvatarGroupItem> items = client1.getItems();
        Assert.assertEquals(1, items.size());

        AvatarGroupItem item = items.get(0);

        Assert.assertNull(item.getImage());
        Assert.assertNull(item.getImageResource());
    }

    @Test
    public void imageProvider_clearProvider_imageIsSetFromUserInfo() {
        UI.setCurrent(client1.ui);
        client1.group.setImageProvider(
                user -> new TestStreamResource(user.getName()));
        client1.attach();
        client2.attach();

        client1.group.setImageProvider(null);

        List<AvatarGroupItem> items = client1.getItems();
        Assert.assertEquals(1, items.size());
        AvatarGroupItem item = items.get(0);

        Assert.assertNull(item.getImageResource());
        Assert.assertEquals("image2", item.getImage());
    }

}
