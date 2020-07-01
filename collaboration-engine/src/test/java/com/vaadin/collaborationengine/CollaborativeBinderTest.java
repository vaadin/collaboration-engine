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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.server.Command;

public class CollaborativeBinderTest {

    private static class Client {
        TestField field = new TestField();

        CollaborativeMap map;
        Binder<TestBean> binder;
        ActivationHandler activationHandler;

        public Client(CollaborationEngine collaborationEngine) {
            ConnectionContext context = new ConnectionContext() {
                @Override
                public void setActivationHandler(ActivationHandler handler) {
                    Client.this.activationHandler = handler;
                    handler.setActive(true);
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
    private Binder<TestBean> binder;
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
        Assert.assertEquals("foo", map.get("value"));
    }

    @Test
    public void bind_setMapValue_fieldUpdated() {
        binder.bind(field, "value");
        map.put("value", "foo");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void setMapValue_bind_fieldUpdated() {
        map.put("value", "foo");
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
        map.put("value", "foo");
        binder.bind(field, "value");
        map.put("value", null);
        Assert.assertEquals(field.getEmptyValue(), field.getValue());
    }

    @Test
    public void bind_readBean_mapAndFieldUpdated() {
        binder.bind(field, "value");
        binder.readBean(new TestBean("foo"));
        Assert.assertEquals("foo", map.get("value"));
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
                map.get("value"));

        map.put("value", "bar");
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
                map.get("value"));

        map.put("value", "bar");
        Assert.assertEquals(
                "Field value shouldn't have changed after updating the map",
                "foo", field.getValue());
    }

    @Test
    public void bind_deactivateTopicConnection_fieldHasNoValueChangeListeners() {
        binder.bind(field, "value");
        activationHandler.setActive(false);
        Assert.assertFalse("All ValueChangeListeners should have been removed",
                field.hasListener(ComponentValueChangeEvent.class));
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
}
