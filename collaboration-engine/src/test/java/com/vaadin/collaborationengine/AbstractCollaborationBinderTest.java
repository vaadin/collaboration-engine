package com.vaadin.collaborationengine;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;

import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder;

public class AbstractCollaborationBinderTest {

    public static class BinderTestClient {
        UserInfo user = new UserInfo(UUID.randomUUID().toString());
        TestField field = new TestField();

        CollaborationBinder<TestBean> binder;
        private final UI ui;

        public BinderTestClient() {
            this.ui = new MockUI();
            binder = new CollaborationBinder<>(TestBean.class, user);
            binder.setTopic("topic", () -> null);
        }

        public Binder.Binding<TestBean, String> bind() {
            return binder.bind(field, "value");
        }

        public void attach() {
            ui.add(field);
        }

        public void detach() {
            ui.remove(field);
        }

        public void cleanUp() {
            ui.getChildren()
                    .forEach(component -> ((TestField) component).blur());
            ui.removeAll();
        }
    }

    protected BinderTestClient client;
    protected BinderTestClient client2;
    protected TestField field;
    protected TopicConnection topicConnection;
    protected CollaborationMap map;

    @Before
    public void init() {
        client = new BinderTestClient();
        field = client.field;

        client2 = new BinderTestClient();

        TestUtils.openEagerConnection("topic", topicConnection -> {
            this.topicConnection = topicConnection;
            map = CollaborationBinderUtil.getMap(topicConnection);
        });
    }

    @After
    public void cleanUp() {
        client.cleanUp();
        client2.cleanUp();
        map.put("value", null);
    }

    protected void setSharedValue(String key, Object value) {
        CollaborationBinder.FieldState oldState = getFieldState(key);
        map.put(key, new CollaborationBinder.FieldState(value,
                oldState != null ? oldState.editors : Collections.emptyList()));
    }

    protected Object getSharedValue(String key) {
        CollaborationBinder.FieldState fieldState = getFieldState(key);
        return fieldState != null ? fieldState.value : null;
    }

    protected CollaborationBinder.FieldState getFieldState(String key) {
        return (CollaborationBinder.FieldState) map.get(key);
    }

    protected List<UserInfo> getEditors(String key) {
        if (getFieldState(key) == null) {
            return Collections.emptyList();
        }
        return getFieldState(key).editors.stream()
                .map(focusedEditor -> focusedEditor.user)
                .collect(Collectors.toList());
    }

}
