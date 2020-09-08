package com.vaadin.collaborationengine.util;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchElement;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;

public class FieldHighlightUtil {

    private FieldHighlightUtil() {
    }

    public static void assertNoUserTags(TestBenchElement... fields) {
        for (TestBenchElement field : fields) {
            assertUserTags(field);
        }
    }

    public static void assertUserTags(TestBenchElement field,
            String... expectedUsers) {
        assertUserTags("Unexpected user tags on field " + field, field,
                expectedUsers);
    }

    public static void assertUserTags(String message, TestBenchElement field,
            String... expectedUsers) {
        List<UserTagElement> userTags = getUserTags(field);

        validateColorIndices(userTags);

        String[] names = userTags.stream().map(UserTagElement::getName)
                .toArray(String[]::new);
        Assert.assertArrayEquals(message, expectedUsers, names);
    }

    private static void validateColorIndices(List<UserTagElement> userTags) {
        userTags.stream().map(UserTagElement::getColorIndex)
                .forEach(colorIndex -> Assert.assertTrue(
                        "Invalid color index on a user tag. "
                                + "Expected integer in range 0-9, but was "
                                + colorIndex,
                        colorIndex != null && colorIndex >= 0
                                && colorIndex < 10));
    }

    public static List<UserTagElement> getUserTags(TestBenchElement field) {
        if (!field.$("vaadin-user-tags").exists()) {
            return Collections.emptyList();
        }
        TestBenchElement tagsElement = field.$("vaadin-user-tags").first();
        List<TestBenchElement> tagElements = (List<TestBenchElement>) tagsElement
                .getCommandExecutor().executeScript(
                        "arguments[0].render(); return Array.from(arguments[0].$.overlay.content.querySelectorAll('vaadin-user-tag'))",
                        tagsElement);
        return tagElements.stream()
                .map(tag -> TestBench.wrap(tag, UserTagElement.class))
                .collect(Collectors.toList());
    }
}
