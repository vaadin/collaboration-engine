package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

    public static class AvatarGroupTestClient {
        final UI ui;
        final UserInfo user;
        CollaborationAvatarGroup group;

        AvatarGroupTestClient(int index, CollaborationEngine ce) {
            this(index, TOPIC_ID, ce);
        }

        AvatarGroupTestClient(int index, String topicId,
                CollaborationEngine ce) {
            this.ui = new MockUI();
            this.user = new UserInfo("id" + index, "name" + index,
                    "image" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setColorIndex(index);
            group = new CollaborationAvatarGroup(user, topicId, ce);
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

        List<AvatarGroupItem> getItems() {
            return group.getContent().getItems();
        }

        List<String> getItemNames() {
            return group.getContent().getItems().stream()
                    .map(AvatarGroupItem::getName).collect(Collectors.toList());
        }
    }

    private CollaborationEngine ce;

    private AvatarGroupTestClient client1;
    private AvatarGroupTestClient client2;
    private AvatarGroupTestClient client3;

    private AvatarGroupTestClient clientInOtherTopic;

    private AvatarGroupTestClient clientInMultipleTabs1;
    private AvatarGroupTestClient clientInMultipleTabs2;

    @Before
    public void init() {
        ce = new CollaborationEngine();
        TestUtil.setDummyCollaborationEngineConfig(ce);
        client1 = new AvatarGroupTestClient(1, ce);
        client2 = new AvatarGroupTestClient(2, ce);
        client3 = new AvatarGroupTestClient(3, ce);
        clientInOtherTopic = new AvatarGroupTestClient(4, TOPIC_ID_2, ce);
        clientInMultipleTabs1 = new AvatarGroupTestClient(5, ce);
        clientInMultipleTabs2 = new AvatarGroupTestClient(5, ce);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
    }

    @Test
    public void beforeAttach_ownAvatarDisplayed() {
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void beforeAttachNoOwnAvatar_noInitialAvatar() {
        client1.group.setOwnAvatarVisible(false);
        Assert.assertEquals(Collections.emptyList(), client1.getItems());
    }

    @Test
    public void attach_ownAvatarDisplayed() {
        client1.attach();
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void attachTwoGroups_bothAvatarsDisplayed() {
        client1.attach();
        client2.attach();
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client2.getItemNames());
    }

    @Test
    public void attachSameUserTwice_avatarDisplayedOnce() {
        clientInMultipleTabs1.attach();
        clientInMultipleTabs2.attach();
        Assert.assertEquals(Arrays.asList("name5"),
                clientInMultipleTabs1.getItemNames());
        Assert.assertEquals(Arrays.asList("name5"),
                clientInMultipleTabs2.getItemNames());
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
        Assert.assertEquals(Arrays.asList("name2"), client2.getItemNames());
    }

    @Test
    public void threeUsers_attach_detach_avatarsUpdated() {
        client1.attach();
        client2.attach();
        client3.attach();

        List<String> expected = Arrays.asList("name1", "name2", "name3");
        Assert.assertEquals(expected, client1.getItemNames());
        Assert.assertEquals(expected, client2.getItemNames());
        Assert.assertEquals(expected, client3.getItemNames());

        client2.detach();

        expected = Arrays.asList("name1", "name3");
        Assert.assertEquals(expected, client1.getItemNames());
        Assert.assertEquals(expected, client3.getItemNames());
    }

    @Test
    public void groupsWithDifferentTopicIds_avatarsNotUpdated() {
        client1.attach();
        clientInOtherTopic.attach();

        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name4"),
                clientInOtherTopic.getItemNames());
    }

    @Test
    public void detach_onlyLocalAvatarDisplayed() {
        client1.attach();
        client2.attach();
        client3.attach();

        client1.detach();

        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void detach_reattach_avatarsUpdated() {
        client1.attach();
        client2.attach();
        client3.attach();

        client1.detach();
        client1.attach();

        List<String> expected = Arrays.asList("name2", "name3", "name1");
        Assert.assertEquals(expected, client1.getItemNames());
        Assert.assertEquals(expected, client2.getItemNames());
    }

    @Test
    public void attachSameUserTwice_detachOne_avatarNotRemoved() {
        clientInMultipleTabs1.attach();
        clientInMultipleTabs2.attach();
        clientInMultipleTabs2.detach();
        Assert.assertEquals(Arrays.asList("name5"),
                clientInMultipleTabs1.getItemNames());
    }

    @Test
    public void attachSameUserTwice_detachBoth_avatarRemoved() {
        client1.attach();
        clientInMultipleTabs1.attach();
        clientInMultipleTabs2.attach();
        clientInMultipleTabs1.detach();
        clientInMultipleTabs2.detach();
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void setTopic_closeExistingConnection() {
        client1.attach();
        client2.attach();

        client1.setGroupTopic("new topic");
        client3.attach();
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void setTopic_showAvatarsFromNewTopic() {
        client1.attach();
        client2.attach();

        AvatarGroupTestClient newClient = new AvatarGroupTestClient(9,
                "new topic", ce);
        newClient.attach();

        client1.setGroupTopic("new topic");
        Assert.assertEquals(
                Arrays.asList(newClient.user.getName(), client1.user.getName()),
                client1.getItemNames());

        AvatarGroupTestClient newClient1 = new AvatarGroupTestClient(10,
                "new topic", ce);
        newClient1.attach();
        Assert.assertEquals(Arrays.asList(newClient.user.getName(),
                client1.user.getName(), newClient1.user.getName()),
                client1.getItemNames());
    }

    @Test
    public void setTopic_nullTopic_closeConnectionAndRemoveRemoteAvatars() {
        client1.attach();
        client2.attach();

        client1.setGroupTopic(null);
        client3.attach();
        Assert.assertEquals(Arrays.asList("name1"), client1.getItemNames());
    }

    @Test
    public void nullTopic_setTopic_avatarsUpdated() {
        client2.attach();
        client3.attach();

        client1.group = new CollaborationAvatarGroup(client1.user, null, ce);
        client1.setGroupTopic(TOPIC_ID);
        client1.attach();
        Assert.assertEquals(Arrays.asList("name2", "name3", "name1"),
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
    public void collaborationMapValueEncodedAsJsonNode() {
        client1.attach();
        AtomicBoolean done = new AtomicBoolean(false);
        TestUtils.openEagerConnection(ce, TOPIC_ID, topicConnection -> {
            List<UserInfo> mapValue = topicConnection
                    .getNamedMap(CollaborationAvatarGroup.MAP_NAME)
                    .get(CollaborationAvatarGroup.MAP_KEY,
                            JsonUtil.LIST_USER_TYPE_REF);
            List<String> ids = mapValue.stream().map(UserInfo::getId)
                    .collect(Collectors.toList());
            Assert.assertTrue(ids.contains(client1.user.getId()));
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
        Assert.assertEquals(2, items.size());
        AvatarGroupItem item = items.get(1);

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
        Assert.assertEquals(2, items.size());

        AvatarGroupItem item = items.get(1);

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
        Assert.assertEquals(2, items.size());

        AvatarGroupItem item = items.get(1);

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
        Assert.assertEquals(2, items.size());
        AvatarGroupItem item = items.get(1);

        Assert.assertNull(item.getImageResource());
        Assert.assertEquals("image2", item.getImage());
    }

    @Test
    public void setOwnAvatarVisibleFalse_ownAvatarNotIncluded() {
        client1.group.setOwnAvatarVisible(false);
        client2.group.setOwnAvatarVisible(false);

        client1.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getItemNames());

        client2.attach();
        Assert.assertEquals(Arrays.asList("name2"), client1.getItemNames());
        Assert.assertEquals(Arrays.asList("name1"), client2.getItemNames());
    }

    @Test
    public void attachGroup_toggleOwnAvatarVisible_avatarsUpdated() {
        client1.attach();
        client2.attach();

        client1.group.setOwnAvatarVisible(false);
        Assert.assertEquals(Arrays.asList("name2"), client1.getItemNames());

        client1.group.setOwnAvatarVisible(true);
        Assert.assertEquals(Arrays.asList("name1", "name2"),
                client1.getItemNames());
    }
}
