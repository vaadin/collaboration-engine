package com.vaadin.collaborationengine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestEnum;

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

    @Test
    public void setFieldValue_worksWithSupportedValueTypes() {

        BiConsumer<Object, Class<?>> test = (value, type) -> {
            CollaborationBinderUtil.setFieldValue(topicConnection, "propName",
                    value);
            Assert.assertEquals(value, CollaborationBinderUtil
                    .getFieldState(topicConnection, "propName", type).value);
        };

        test.accept("foo", String.class);
        test.accept(true, Boolean.class);
        test.accept(1, Integer.class);
        test.accept(1.5, Double.class);

        test.accept(new BigDecimal(2.5), BigDecimal.class);

        test.accept(LocalDate.of(2020, 2, 3), LocalDate.class);
        test.accept(LocalTime.of(1, 2, 3, 4), LocalTime.class);
        test.accept(LocalDateTime.of(2020, 2, 3, 4, 5, 6, 7),
                LocalDateTime.class);

        test.accept(TestEnum.FOO, TestEnum.class);

        test.accept(Arrays.asList("foo", "bar"), List.class);
        test.accept(new HashSet(Arrays.asList("foo", "bar")), Set.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setFieldValue_unsupportedType_throws() {
        CollaborationBinderUtil.setFieldValue(topicConnection, "propName",
                new TestBean());
    }

    @Test(expected = IllegalStateException.class)
    public void setFieldValue_unsupportedTypeInCollection_throws() {
        CollaborationBinderUtil.setFieldValue(topicConnection, "propName",
                Arrays.asList(1, 2));
    }

}
