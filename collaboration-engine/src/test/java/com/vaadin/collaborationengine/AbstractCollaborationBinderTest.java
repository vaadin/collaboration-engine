package com.vaadin.collaborationengine;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;

import com.vaadin.collaborationengine.CollaborationBinder.FieldState;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.collaborationengine.util.TestLocalDateField;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder;

public class AbstractCollaborationBinderTest {

    public static class BinderTestClient {
        UserInfo user = new UserInfo(UUID.randomUUID().toString());
        TestField field = new TestField();
        TestLocalDateField localDateField = new TestLocalDateField();

        CollaborationBinder<TestBean> binder;
        private final UI ui;

        public BinderTestClient(CollaborationEngine ce) {
            this.ui = new MockUI();
            binder = new CollaborationBinder<>(TestBean.class, user, ce);
            binder.setTopic("topic", () -> null);
        }

        public Binder.Binding<TestBean, String> bind() {
            return binder.forField(field).bind("value");
        }

        public Binder.Binding<TestBean, LocalDate> bindLocalDate() {
            return binder.forField(localDateField).bind("localDate");
        }

        public void attach() {
            attach(field);
        }

        public void attach(Component child) {
            ui.add(child);
        }

        public void detach() {
            ui.remove(field);
            ui.remove(localDateField);
        }
    }

    protected CollaborationEngine ce;

    protected BinderTestClient client;
    protected BinderTestClient client2;
    protected TestField field;
    protected TopicConnection topicConnection;
    protected CollaborationMap map;

    @Before
    public void init() {
        ce = new CollaborationEngine();
        TestUtil.setDummyCollaborationEngineConfig(ce);

        client = new BinderTestClient(ce);
        field = client.field;

        client2 = new BinderTestClient(ce);

        TestUtils.openEagerConnection(ce, "topic", topicConnection -> {
            this.topicConnection = topicConnection;
            map = CollaborationBinderUtil.getMap(topicConnection);
        });
    }

    protected void setSharedValue(String key, Object value) {
        CollaborationBinderUtil.setFieldValue(topicConnection, key, value);
    }

    protected <T> T getSharedValue(String key, Class<T> type) {
        return JsonUtil.toInstance(getFieldState(key).value, type);
    }

    protected FieldState getFieldState(String key) {
        return CollaborationBinderUtil.getFieldState(topicConnection, key);
    }

    protected List<UserInfo> getEditors(String key) {
        return getFieldState(key).editors.stream()
                .map(focusedEditor -> focusedEditor.user)
                .collect(Collectors.toList());
    }

}
