package com.vaadin.collaborationengine;

import static com.vaadin.collaborationengine.JsonUtil.jsonToUsers;
import static com.vaadin.collaborationengine.JsonUtil.usersToJson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonUtilTest {

    private UserInfo user;

    @Before
    public void init() {
        user = new UserInfo("my-id");
        user.setName("my-name");
        user.setAbbreviation("my-abbreviation");
        user.setImage("my-image");
        user.setColorIndex(5);
    }

    @Test
    public void usersList_toJsonAndBack_returnsEqualUsers() {
        List<UserInfo> users = Arrays.asList(new UserInfo("1"),
                new UserInfo("2"));
        Assert.assertEquals(users, jsonToUsers(usersToJson(users)));
    }

    @Test
    public void emptyUsersList_toJsonAndBack_returnsEmptyList() {
        Assert.assertEquals(Collections.emptyList(),
                jsonToUsers(usersToJson(Collections.emptyList())));
    }

    @Test
    public void nullUsersList_toJsonAndBack_returnsNull() {
        Assert.assertEquals(null, jsonToUsers(usersToJson(null)));
    }

    @Test
    public void userInfo_toJsonAndBack_allPropertiesPreserved() {
        UserInfo deserializedUser = jsonToUsers(
                usersToJson(Arrays.asList(user))).get(0);

        Assert.assertEquals("my-id", deserializedUser.getId());
        Assert.assertEquals("my-name", deserializedUser.getName());
        Assert.assertEquals("my-abbreviation",
                deserializedUser.getAbbreviation());
        Assert.assertEquals("my-image", deserializedUser.getImage());
        Assert.assertEquals(5, deserializedUser.getColorIndex());
    }

}
