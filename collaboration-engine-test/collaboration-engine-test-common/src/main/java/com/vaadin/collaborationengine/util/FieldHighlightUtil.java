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
package com.vaadin.collaborationengine.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.TimeoutException;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchElement;

public abstract class FieldHighlightUtil extends AbstractCollaborativeViewTest {

    public void assertNoUserTags(TestBenchElement... fields) {
        for (TestBenchElement field : fields) {
            assertUserTags(field);
        }
    }

    public void assertUserTags(TestBenchElement field,
            String... expectedUsers) {
        assertUserTags("Unexpected user tags on field " + field, field,
                expectedUsers);
    }

    public void assertUserTags(String message, TestBenchElement field,
            String... expectedUsers) {

        try {
            // Fix timing issues by waiting for the tags to update
            waitUntil(d -> Arrays.equals(getUserTagNames(field), expectedUsers),
                    3);
        } catch (TimeoutException e) {
            // Continue to assertion to get valuable error message
        }

        Assert.assertArrayEquals(message, expectedUsers,
                getUserTagNames(field));

        validateColorIndices(getUserTags(field));
    }

    public String[] getUserTagNames(TestBenchElement field) {
        return getUserTags(field).stream().map(UserTagElement::getName)
                .toArray(String[]::new);
    }

    private void validateColorIndices(List<UserTagElement> userTags) {
        userTags.stream().map(UserTagElement::getColorIndex)
                .forEach(colorIndex -> Assert.assertTrue(
                        "Invalid color index on a user tag. "
                                + "Expected integer in range 0-9, but was "
                                + colorIndex,
                        colorIndex != null && colorIndex >= 0
                                && colorIndex < 10));
    }

    public List<UserTagElement> getUserTags(TestBenchElement field) {
        if (!field.$("vaadin-user-tags").exists()) {
            return Collections.emptyList();
        }
        TestBenchElement tagsElement = field.$("vaadin-user-tags").first();
        List<TestBenchElement> tagElements = (List<TestBenchElement>) tagsElement
                .getCommandExecutor().executeScript(
                        "arguments[0].requestContentUpdate(); return arguments[0].wrapper"
                                + ".querySelectorAll('vaadin-user-tag')",
                        tagsElement);
        return tagElements.stream()
                .map(tag -> TestBench.wrap(tag, UserTagElement.class))
                .collect(Collectors.toList());
    }
}
