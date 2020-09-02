package com.vaadin.collaborationengine;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CollaborationBinderUtilTest
        extends AbstractCollaborationBinderTest {

    @Before
    public void init2() {
        client.bind();
        client.attach();
        field.showHighlight();
    }

    @Test
    public void setFieldValue_fieldUpdated() {
        CollaborationBinderUtil.setFieldValue(topicConnection, "value", "foo");

        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void addEditors_editorsUpdated() {
        CollaborationBinderUtil.addEditor(topicConnection, "value",
                new UserInfo("1"));
        CollaborationBinderUtil.addEditor(topicConnection, "value",
                new UserInfo("2"));

        Assert.assertEquals(Arrays.asList(client.user, new UserInfo("1"),
                new UserInfo("2")), getEditors("value"));
    }

    @Test
    public void addDuplicateEditor_noDuplicates() {
        String id = "987";

        CollaborationBinderUtil.addEditor(topicConnection, "value",
                new UserInfo(id));
        CollaborationBinderUtil.addEditor(topicConnection, "value",
                new UserInfo(id));

        Assert.assertEquals(Arrays.asList(client.user, new UserInfo(id)),
                getEditors("value"));
    }

    @Test
    public void removeEditor_editorRemoved() {
        String id = "46";

        CollaborationBinderUtil.addEditor(topicConnection, "value",
                new UserInfo(id));
        CollaborationBinderUtil.removeEditor(topicConnection, "value",
                new UserInfo(id));

        Assert.assertEquals(Arrays.asList(client.user), getEditors("value"));
    }

    @Test
    public void removeNonExistingEditor_doesNothing() {
        CollaborationBinderUtil.removeEditor(topicConnection, "value",
                new UserInfo("not an editor"));
        Assert.assertEquals(Arrays.asList(client.user), getEditors("value"));
    }

}
