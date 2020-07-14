/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.BlurNotifier.BlurEvent;
import com.vaadin.flow.component.FocusNotifier.FocusEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class CollaborativeBinderTest {

    private class Client {
        TestField field = new TestField();

        CollaborativeBinder<TestBean> binder;
        private final UI ui;

        public Client() {
            this.ui = new MockUI();
            binder = new CollaborativeBinder<>(TestBean.class, "topic");
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

    private Client client;
    private Client client2;
    private TestField field;
    private CollaborativeMap map;

    @Before
    public void init() {
        client = new Client();
        field = client.field;

        client2 = new Client();

        CollaborationEngine.getInstance()
                .openTopicConnection(new ConnectionContext() {
                    @Override
                    public Registration setActivationHandler(
                            ActivationHandler handler) {
                        handler.setActive(true);
                        return null;
                    }

                    @Override
                    public void dispatchAction(Command action) {
                        action.execute();
                    }
                }, "topic", topic -> {
                    map = topic.getNamedMap(
                            CollaborativeBinder.COLLABORATIVE_BINDER_MAP_NAME);
                    return null;
                });
    }

    @After
    public void cleanUp() {
        client.cleanUp();
        client2.cleanUp();
        map.put("value", null);
    }

    private void setSharedValue(String key, Object value) {
        CollaborativeBinder.FieldState oldState = getFieldState(key);
        map.put(key, new CollaborativeBinder.FieldState(value,
                oldState != null ? oldState.editors : Collections.emptyList()));
    }

    private Object getSharedValue(String key) {
        CollaborativeBinder.FieldState fieldState = getFieldState(key);
        return fieldState != null ? fieldState.value : null;
    }

    private CollaborativeBinder.FieldState getFieldState(String key) {
        return (CollaborativeBinder.FieldState) map.get(key);
    }

    private List<UserInfo> getEditors(String key) {
        if (getFieldState(key) == null) {
            return Collections.emptyList();
        }
        return getFieldState(key).editors;
    }

    @Test
    public void bind_setFieldValue_sharedValueNotUpdated() {
        client.bind();
        field.setValue("foo");
        Assert.assertNull(
                "Collaborative value shouldn't be updated in inactivated connection",
                getSharedValue("value"));
    }

    @Test
    public void bind_activate_setFieldValue_sharedValueUpdated() {
        client.bind();
        client.attach();
        field.setValue("foo");
        Assert.assertEquals("foo", getSharedValue("value"));
    }

    @Test
    public void activate_bind_setFieldValue_sharedValueUpdated() {
        client.attach();
        client.bind();
        field.setValue("foo");
        Assert.assertEquals("foo", getSharedValue("value"));
    }

    @Test
    public void bind_setSharedValue_fieldNotUpdated() {
        client.bind();
        setSharedValue("value", "foo");
        Assert.assertNull(
                "Field shouldn't be updated in inactivated connection",
                field.getValue());
    }

    @Test
    public void bind_activate_setSharedValue_fieldUpdated() {
        client.bind();
        client.attach();
        setSharedValue("value", "foo");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void bind_activate_fieldHasEmptyValue() {
        client.bind();
        client.attach();
        Assert.assertEquals(field.getEmptyValue(), field.getValue());
    }

    @Test
    public void bind_activate_setSharedValueNull_fieldHasEmptyValue() {
        client.bind();
        client.attach();
        setSharedValue("value", "foo");
        setSharedValue("value", null);
        Assert.assertEquals(field.getEmptyValue(), field.getValue());
    }

    @Test
    public void bind_activate_readBean_sharedValueAndFieldUpdated() {
        client.bind();
        client.attach();
        client.binder.readBean(new TestBean("foo"));
        Assert.assertEquals("foo", getSharedValue("value"));
        Assert.assertEquals("foo", field.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void bindWithoutPropertyName_throws() {
        client.binder.bind(field, TestBean::getValue, TestBean::setValue);
    }

    @Test
    public void bind_unbind_listenersRemoved() {
        Binder.Binding<TestBean, String> binding = client.bind();
        Assert.assertTrue(
                "Expected field to have listeners. The test is invalid.",
                field.hasListener(ComponentValueChangeEvent.class)
                        && field.hasListener(FocusEvent.class)
                        && field.hasListener(BlurEvent.class));

        binding.unbind();

        Assert.assertFalse("All ValueChangeListeners should have been removed",
                field.hasListener(ComponentValueChangeEvent.class));
        Assert.assertFalse("All focus listeners should have been removed",
                field.hasListener(FocusEvent.class));
        Assert.assertFalse("All blur listeners should have been removed",
                field.hasListener(BlurEvent.class));
    }

    @Test
    public void bind_unbind_changesNotPropagated() {
        Binder.Binding<TestBean, String> binding = client.bind();
        binding.unbind();

        field.setValue("foo");
        Assert.assertNull(
                "Map shouldn't have changed after setting the field value",
                getSharedValue("value"));

        setSharedValue("value", "bar");
        Assert.assertEquals(
                "Field value shouldn't have changed after updating the map",
                "foo", field.getValue());
    }

    @Test
    public void bind_unbind_editorRemoved() {
        Binder.Binding<TestBean, String> bind = client.bind();
        client.attach();
        field.focus();

        bind.unbind();

        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void bind_deactivate_setFieldValue_sharedValueUpdated() {
        client.bind();
        client.attach();
        client.detach(); // connection is deactivated but the binding remains
        field.setValue("bar");
        Assert.assertEquals("bar", getSharedValue("value"));
    }

    @Test
    public void bind_activate_setValue_secondClientBinds_fieldGetsCurrentValue() {
        client.bind();
        client.attach();
        field.setValue("foo");

        client2.binder.bind(client2.field, "value");

        Assert.assertNull(
                "The second client should not receive value when in-activated",
                client2.field.getValue());

        client2.attach();
        Assert.assertEquals(
                "The second client should have the field value set by the first client",
                "foo", client2.field.getValue());
    }

    @Test
    public void twoActivatedClientsWithBinders_setFieldValueOnOne_otherUpdated() {
        client.bind();
        client.attach();

        client2.bind();
        client2.attach();

        client2.field.setValue("bar");
        Assert.assertEquals("bar", field.getValue());
    }

    @Test
    public void twoActivatedClientsWithBinders_setSharedValue_twoClientFieldsUpdated() {
        client.bind();
        client.attach();

        client2.bind();
        client2.attach();

        setSharedValue("value", "baz");

        Assert.assertEquals("baz", field.getValue());
        Assert.assertEquals("baz", client2.field.getValue());
    }

    @Test
    public void bind_active_topicHasNoEditors() {
        client.bind();
        client.attach();
        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void bind_activate_focusField_topicHasEditor() {
        client.bind();
        client.attach();
        field.focus();
        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser()),
                getEditors("value"));
    }

    @Test
    public void bind_activate_focusField_blurField_topicHasNoEditors() {
        client.bind();
        client.attach();
        field.focus();
        field.blur();
        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void bind_focusTwice_topicHasOneEditor() {
        client.bind();
        client.attach();
        field.focus();
        field.focus();
        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser()),
                getEditors("value"));
    }

    @Test
    public void twoActivatedClientsFocus_topicContainsBothEditors() {
        client.bind();
        client.attach();

        client2.bind();
        client2.attach();

        field.focus();
        client2.field.focus();

        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser(),
                client2.binder.getLocalUser()), getEditors("value"));
    }

}
