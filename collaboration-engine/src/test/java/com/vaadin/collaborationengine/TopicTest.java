package com.vaadin.collaborationengine;

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

}
