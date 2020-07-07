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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.CollaborativeBinder.FieldState;
import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.BlurNotifier.BlurEvent;
import com.vaadin.flow.component.FocusNotifier.FocusEvent;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class CollaborativeBinderTest {

    private static class Client {
        TestField field = new TestField();

        CollaborativeMap map;
        CollaborativeBinder<TestBean> binder;
        ActivationHandler activationHandler;

        public Client(CollaborationEngine collaborationEngine) {
            ConnectionContext context = new ConnectionContext() {
                @Override
                public Registration setActivationHandler(
                        ActivationHandler handler) {
                    Client.this.activationHandler = handler;
                    handler.setActive(true);
                    return null;
                }

                @Override
                public void dispatchAction(Command action) {
                    action.execute();
                }
            };

            collaborationEngine.openTopicConnection(context, "topic",
                    topicConnection -> {
                        map = topicConnection.getNamedMap("binder");
                        return null;
                    });
            binder = new CollaborativeBinder<>(TestBean.class, map);
        }
    }

    private CollaborationEngine collaborationEngine;

    private CollaborativeMap map;
    private CollaborativeBinder<TestBean> binder;
    private ActivationHandler activationHandler;

    private TestField field;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        Client client = new Client(collaborationEngine);

        this.field = client.field;
        this.map = client.map;
        this.binder = client.binder;
        this.activationHandler = client.activationHandler;
    }

    @Test
    public void bind_setFieldValue_mapUpdated() {
        binder.bind(field, "value");
        field.setValue("foo");
        Assert.assertEquals("foo", getValueFromMap(map, "value"));
    }

    @Test
    public void bind_setMapValue_fieldUpdated() {
        binder.bind(field, "value");
        setValueToMap(map, "value", "foo");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void setMapValue_bind_fieldUpdated() {
        setValueToMap(map, "value", "foo");
        binder.bind(field, "value");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void bind_fieldHasEmptyValue() {
        binder.bind(field, "value");
        Assert.assertEquals(field.getEmptyValue(), field.getValue());
    }

    @Test
    public void bind_setMapValueNull_fieldHasEmptyValue() {
        setValueToMap(map, "value", "foo");
        binder.bind(field, "value");
        setValueToMap(map, "value", null);
        Assert.assertEquals(field.getEmptyValue(), field.getValue());
    }

    @Test
    public void bind_readBean_mapAndFieldUpdated() {
        binder.bind(field, "value");
        binder.readBean(new TestBean("foo"));
        Assert.assertEquals("foo", getValueFromMap(map, "value"));
        Assert.assertEquals("foo", field.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void bindWithoutPropertyName_throws() {
        binder.bind(field, TestBean::getValue, TestBean::setValue);
    }

    @Test
    public void bind_unbind_changesNotPropagated() {
        Binding<TestBean, String> binding = binder.bind(field, "value");
        binding.unbind();

        field.setValue("foo");
        Assert.assertNull(
                "Map shouldn't have changed after setting the field value",
                getValueFromMap(map, "value"));

        setValueToMap(map, "value", "bar");
        Assert.assertEquals(
                "Field value shouldn't have changed after updating the map",
                "foo", field.getValue());
    }

    @Test
    public void bind_deactivateTopicConnection_changesNotPropagated() {
        binder.bind(field, "value");
        activationHandler.setActive(false);

        field.setValue("foo");
        Assert.assertNull(
                "Map shouldn't have changed after setting the field value",
                getValueFromMap(map, "value"));

        setValueToMap(map, "value", "bar");
        Assert.assertEquals(
                "Field value shouldn't have changed after updating the map",
                "foo", field.getValue());
    }

    @Test
    public void bind_deactivateTopicConnection_listenersRemoved() {
        binder.bind(field, "value");

        Assert.assertTrue(
                "Expected field to have listeners. The test is invalid.",
                field.hasListener(ComponentValueChangeEvent.class)
                        && field.hasListener(FocusEvent.class)
                        && field.hasListener(BlurEvent.class));

        activationHandler.setActive(false);

        Assert.assertFalse("All ValueChangeListeners should have been removed",
                field.hasListener(ComponentValueChangeEvent.class));
        Assert.assertFalse("All focus listeners should have been removed",
                field.hasListener(FocusEvent.class));
        Assert.assertFalse("All blur listeners should have been removed",
                field.hasListener(BlurEvent.class));
    }

    @Test
    public void bindAndSetValue_secondClientBinds_fieldGetsCurrentValue() {
        binder.bind(field, "value");
        field.setValue("foo");

        Client client2 = new Client(collaborationEngine);
        client2.binder.bind(client2.field, "value");

        Assert.assertEquals(
                "The second client should have the field value set by the first client",
                "foo", client2.field.getValue());
    }

    @Test
    public void twoClientsWithBinders_setFieldValueOnOne_otherUpdated() {
        binder.bind(field, "value");

        Client client2 = new Client(collaborationEngine);
        client2.binder.bind(client2.field, "value");

        client2.field.setValue("bar");
        Assert.assertEquals("bar", field.getValue());
    }

    @Test
    public void bind_mapHasNoEditors() {
        binder.bind(field, "value");
        Assert.assertEquals(Collections.emptyList(),
                getEditorsFromMap(map, "value"));
    }

    @Test
    public void bind_focusField_mapHasEditor() {
        binder.bind(field, "value");
        field.focus();
        Assert.assertEquals(Arrays.asList(binder.getLocalUser()),
                getEditorsFromMap(map, "value"));
    }

    @Test
    public void bind_focusField_blurField_mapHasNoEditors() {
        binder.bind(field, "value");
        field.focus();
        field.blur();
        Assert.assertEquals(Collections.emptyList(),
                getEditorsFromMap(map, "value"));
    }

    @Test
    public void bind_focusTwice_mapHasOneEditor() {
        binder.bind(field, "value");
        field.focus();
        field.focus();
        Assert.assertEquals(Arrays.asList(binder.getLocalUser()),
                getEditorsFromMap(map, "value"));
    }

    @Test
    public void twoClientsFocus_mapContainsBothEditors() {
        binder.bind(field, "value");

        Client client2 = new Client(collaborationEngine);
        client2.binder.bind(client2.field, "value");

        field.focus();
        client2.field.focus();

        Assert.assertEquals(
                Arrays.asList(binder.getLocalUser(),
                        client2.binder.getLocalUser()),
                getEditorsFromMap(map, "value"));
    }

    @Test
    public void bind_focus_deactivateTopicConnection_editorRemoved() {
        binder.bind(field, "value");
        field.focus();

        activationHandler.setActive(false);

        Assert.assertEquals(Collections.emptyList(),
                getEditorsFromMap(map, "value"));
    }

    private void setValueToMap(CollaborativeMap map, String key, Object value) {
        FieldState oldState = getFieldState(map, key);
        map.put(key, new FieldState(value,
                oldState != null ? oldState.editors : Collections.emptyList()));
    }

    private Object getValueFromMap(CollaborativeMap map, String key) {
        FieldState fieldState = getFieldState(map, key);
        return fieldState != null ? fieldState.value : null;
    }

    private List<UserInfo> getEditorsFromMap(CollaborativeMap map, String key) {
        return getFieldState(map, key).editors;
    }

    private FieldState getFieldState(CollaborativeMap map, String key) {
        return (FieldState) map.get(key);
    }
}
