package com.vaadin.collaborationengine;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class UserInfoTest {

    private SystemUserInfo systemUserInfo;

    @Before
    public void init() {
        systemUserInfo = SystemUserInfo.get();
    }

    @Test
    public void systemUser_alwaysGetSameInstance() {
        SystemUserInfo systemUserInfo1 = SystemUserInfo.get();
        assertSame(systemUserInfo1, systemUserInfo);
    }

    @Test
    public void systemUser_alwaysHasZeroColorIndex() {
        for (int i = 0; i < 20; i++) {
            assertEquals(0, SystemUserInfo.get().getColorIndex());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void systemUser_setNameNotAllow() {
        systemUserInfo.setName("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void systemUser_setAbbreviationNotAllow() {
        systemUserInfo.setAbbreviation("AB");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void systemUser_setImageNotAllow() {
        systemUserInfo.setImage("image-data");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void systemUser_setColorIndexNameNotAllow() {
        systemUserInfo.setColorIndex(1);
    }

    @Test
    public void systemUser_allBaseSettersAreBlocked() {
        Set<String> baseSetters = ReflectionUtils.getMethodNames(UserInfo.class)
                .stream().filter(methodName -> methodName.startsWith("set"))
                .collect(Collectors.toSet());
        Set<String> subclassSetters = ReflectionUtils
                .getDeclaredMethodNames(SystemUserInfo.class).stream()
                .filter(methodName -> methodName.startsWith("set"))
                .collect(Collectors.toSet());
        Assert.assertTrue(
                "System user must override all setters from the base class to be immutable.",
                subclassSetters.containsAll(baseSetters));
    }
}
