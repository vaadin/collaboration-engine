package com.vaadin.collaborationengine;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopicTest {

    private Topic topic;

    @Before
    public void init() {
        topic = new Topic();
    }

    @Test
    public void applyChange_newMap_mapCreatedWithNewEntry() {
        PutChange change = new PutChange("foo", "bar", "baz");
        topic.applyChange(change);
        Assert.assertEquals("baz", topic.getMapValue("foo", "bar"));
    }

    @Test
    public void applyChange_existingMap_mapEntryUpdated() {
        PutChange change = new PutChange("foo", "bar", "baz");
        topic.applyChange(change);
        PutChange change1 = new PutChange("foo", "bar", "qux");
        topic.applyChange(change1);

        Assert.assertEquals("qux", topic.getMapValue("foo", "bar"));
    }

    @Test
    public void applyChange_existingMap_emptyValue_mapEntryRemoved() {
        PutChange change = new PutChange("foo", "bar", "baz");
        topic.applyChange(change);
        PutChange change1 = new PutChange("foo", "bar", null);
        topic.applyChange(change1);

        Assert.assertNull(topic.getMapValue("foo", "bar"));
    }

    @Test
    public void applyReplace_havingLatestExpectedValue_success() {
        PutChange change = new PutChange("foo", "bar", "baz");
        topic.applyChange(change);
        ReplaceChange replaceChange = new ReplaceChange("foo", "bar", "baz",
                "qux");

        Assert.assertTrue(topic.applyReplace(replaceChange));
        Assert.assertEquals("qux", topic.getMapValue("foo", "bar"));
    }

    @Test
    public void applyReplace_havingWrongExpectedValue_fail() {
        PutChange change = new PutChange("foo", "bar", "baz");
        topic.applyChange(change);
        ReplaceChange replaceChange = new ReplaceChange("foo", "bar", "foo",
                "qux");

        Assert.assertFalse(topic.applyReplace(replaceChange));
        Assert.assertEquals("baz", topic.getMapValue("foo", "bar"));
    }

    @Test
    public void throwingSubscriber_removedAndOthersStillInvoked() {
        AtomicInteger count = new AtomicInteger(0);

        topic.subscribe(event -> {
            // Assert doesn't throw a runtime exception, which means that it
            // would not be caught
            Assert.assertEquals(
                    "Throwing subscriber sould be run before working subscriber",
                    0, count.get());
            throw new RuntimeException("Fail in purpose");
        });

        topic.subscribe(event -> count.getAndIncrement());

        try {
            topic.applyChange(new PutChange("map", "key", "value"));
            Assert.fail("Exception expected");
        } catch (RuntimeException expected) {
        }

        Assert.assertEquals(
                "Working subscriber should be notified even though another subscriber failed",
                1, count.get());

        // No try-catch needed - failing subscriber should have been removed
        topic.applyChange(new PutChange("map", "key", "value2"));

        Assert.assertEquals("Non-failing subscriber should still be notified",
                2, count.get());
    }

}
