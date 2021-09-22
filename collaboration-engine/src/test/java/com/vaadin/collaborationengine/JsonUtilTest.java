package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.VaadinService;

public class JsonUtilTest {

    private UserInfo user;
    private VaadinService service;
    private CollaborationEngine ce;

    @Before
    public void init() {
        service = new MockService();
        ce = new CollaborationEngine();
        service.getContext().setAttribute(CollaborationEngine.class, ce);
        VaadinService.setCurrent(service);

        user = new UserInfo("my-id", "my-name", "my-image");
        user.setAbbreviation("my-abbreviation");
        user.setColorIndex(5);
    }

    @After
    public void cleanUp() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void usersList_toJsonAndBack_returnsEqualUsers() {
        List<UserInfo> users = Arrays.asList(new UserInfo("1"),
                new UserInfo("2"));
        JsonNode usersNode = JsonUtil.toJsonNode(users);
        Assert.assertEquals(users,
                JsonUtil.toInstance(usersNode, JsonUtil.LIST_USER_TYPE_REF));
    }

    @Test
    public void emptyUsersList_toJsonAndBack_returnsEmptyList() {
        Assert.assertEquals(Collections.emptyList(), JsonUtil.toInstance(
                JsonUtil.toJsonNode(Collections.emptyList()), List.class));
    }

    @Test
    public void nullUsersList_toJsonAndBack_returnsNull() {
        Assert.assertNull(
                JsonUtil.toInstance(JsonUtil.toJsonNode(null), Object.class));
    }

    @Test
    public void userInfo_toJsonAndBack_allPropertiesPreserved() {
        UserInfo deserializedUser = JsonUtil
                .toInstance(JsonUtil.toJsonNode(Arrays.asList(user)),
                        JsonUtil.LIST_USER_TYPE_REF)
                .get(0);

        Assert.assertEquals("my-id", deserializedUser.getId());
        Assert.assertEquals("my-name", deserializedUser.getName());
        Assert.assertEquals("my-abbreviation",
                deserializedUser.getAbbreviation());
        Assert.assertEquals("my-image", deserializedUser.getImage());
        Assert.assertEquals(5, deserializedUser.getColorIndex());
    }

    @Test
    public void usersList_toJson_noRedundantData() {
        List<UserInfo> users = Collections.singletonList(user);
        String jsonUsers = JsonUtil.toJsonNode(users).toString();
        Assert.assertEquals(
                "[{\"id\":\"my-id\",\"name\":\"my-name\",\"abbreviation\":\"my-abbreviation\",\"image\":\"my-image\",\"colorIndex\":5}]",
                jsonUsers);
    }

    @Test
    public void userInfo_toJsonNode() {
        JsonNode userJson = JsonUtil.toJsonNode(user);
        Assert.assertEquals("my-id", userJson.get("id").textValue());
        Assert.assertEquals("my-name", userJson.get("name").textValue());
        Assert.assertEquals("my-abbreviation",
                userJson.get("abbreviation").textValue());
        Assert.assertEquals("my-image", userJson.get("image").textValue());
        Assert.assertEquals(5, userJson.get("colorIndex").intValue());
    }

    @Test
    public void literalNull_toUUID_returnsNull() {
        Assert.assertNull(JsonUtil.toUUID(null));
    }

    @Test
    public void nullJsonNode_toUUID_returnsNull() {
        Assert.assertNull(JsonUtil.toUUID(NullNode.instance));
    }

    @Test
    public void uuidString_toUUID_returnsUUID() {
        UUID uuid = UUID.randomUUID();
        ObjectNode node = JsonUtil.getObjectMapper().createObjectNode();
        node.put("uuid", uuid.toString());
        Assert.assertEquals(uuid, JsonUtil.toUUID(node.get("uuid")));
    }
}
