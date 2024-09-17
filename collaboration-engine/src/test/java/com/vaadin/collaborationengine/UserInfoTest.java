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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.ReflectionUtils;
import com.vaadin.collaborationengine.util.TestUtils;

public class UserInfoTest {

    private SystemUserInfo systemUserInfo;

    @Before
    public void init() {
        systemUserInfo = SystemUserInfo.getInstance();
    }

    @Test
    public void systemUser_alwaysGetSameInstance() {
        SystemUserInfo systemUserInfo1 = SystemUserInfo.getInstance();
        assertSame(systemUserInfo1, systemUserInfo);
    }

    @Test
    public void systemUser_alwaysHasZeroColorIndex() {
        for (int i = 0; i < 20; i++) {
            assertEquals(0, SystemUserInfo.getInstance().getColorIndex());
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

    @Test
    public void serializeUserInfo() {
        UserInfo deserializedUserInfo = TestUtils.serialize(systemUserInfo);

        assertEquals(systemUserInfo.getName(), deserializedUserInfo.getName());
        assertEquals(systemUserInfo.getColorIndex(),
                deserializedUserInfo.getColorIndex());
    }
}
