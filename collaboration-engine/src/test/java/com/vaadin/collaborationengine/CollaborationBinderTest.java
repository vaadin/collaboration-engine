package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.CollaborationBinder.FieldState;
import com.vaadin.collaborationengine.util.GenericTestField;
import com.vaadin.collaborationengine.util.TestBean;
import com.vaadin.collaborationengine.util.TestField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.internal.ReflectTools;

public class CollaborationBinderTest extends AbstractCollaborationBinderTest {

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
    public void bind_activate_resetBean_sharedValueAndFieldUpdated() {
        client.bind();
        client.attach();
        client.binder.reset(new TestBean("foo"));
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
                        && field.hasFieldHighlightShowListener()
                        && field.hasFieldHighlightHideListener());

        binding.unbind();

        Assert.assertFalse("All ValueChangeListeners should have been removed",
                field.hasListener(ComponentValueChangeEvent.class));
        Assert.assertFalse(
                "All listeners for showing highlight should have been removed",
                field.hasFieldHighlightShowListener());
        Assert.assertFalse(
                "All listeners for hiding highlight should have been removed",
                field.hasFieldHighlightHideListener());
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
        field.showHighlight();

        bind.unbind();

        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void bind_deactivate_setFieldValue_sharedValueUpdated() {
        client.bind();
        client.attach();
        client.detach();

        field.setValue("bar");
        Assert.assertNull(
                "Binder which has a deactivated connection should not update the map",
                getSharedValue("value"));
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
        field.showHighlight();
        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser()),
                getEditors("value"));
    }

    @Test
    public void bind_activate_focusField_blurField_topicHasNoEditors() {
        client.bind();
        client.attach();
        field.showHighlight();
        field.hideHighlight();
        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void bind_focusTwice_topicHasOneEditor() {
        client.bind();
        client.attach();
        field.showHighlight();
        field.showHighlight();
        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser()),
                getEditors("value"));
    }

    @Test
    public void twoActivatedClientsFocus_topicContainsBothEditors() {
        client.bind();
        client.attach();

        client2.bind();
        client2.attach();

        field.showHighlight();
        client2.field.showHighlight();

        Assert.assertEquals(Arrays.asList(client.binder.getLocalUser(),
                client2.binder.getLocalUser()), getEditors("value"));
    }

    @Test
    public void focus_deactivate_editorRemoved() {
        client.bind();
        client.attach();

        client2.bind();
        client2.attach();

        field.showHighlight();
        client2.field.showHighlight();

        client.detach();

        Assert.assertEquals(Arrays.asList(client2.binder.getLocalUser()),
                getEditors("value"));

        client2.detach();

        Assert.assertEquals(Collections.emptyList(), getEditors("value"));
    }

    @Test
    public void setTopicWhenAttached_topicEmpty_topicInitialized() {
        // Prevent attach from populating the regular topic
        client.binder.setTopic("another", () -> null);

        client.bind();
        client.attach();

        client.binder.setTopic("topic", () -> new TestBean("a"));

        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void setTopicWhenDetached_topicEmpty_topicInitialized() {
        client.binder.setTopic("topic", () -> new TestBean("a"));

        client.bind();
        client.attach();

        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void setTopicWhenAttached_initializedTopic_topicValueRetained() {
        // Prevent attach from using the regular topic
        client.binder.setTopic("another", () -> null);

        setSharedValue("value", "a");

        client.bind();
        client.attach();

        client.binder.setTopic("topic", () -> new TestBean("b"));

        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void setTopicWhenDetached_initializedTopic_topicValueRetained() {
        setSharedValue("value", "a");

        client.binder.setTopic("topic", () -> new TestBean("b"));

        client.bind();
        client.attach();

        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void setTopic_initializedTopic_supplierNotCalled() {
        setSharedValue("value", "a");

        client.binder.setTopic("topic", () -> {
            throw new AssertionError();
        });

        client.bind();
        client.attach();
    }

    @Test
    public void changeTopic_ignoreOldTopic() {
        client.bind();
        client.attach();

        client.binder.setTopic("another", () -> null);

        setSharedValue("value", "a");
        Assert.assertTrue(client.field.isEmpty());

        client.field.setValue("b");
        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void clearTopic_ignoreOldTopic() {
        client.bind();
        client.attach();

        client.binder.setTopic(null, () -> null);

        setSharedValue("value", "a");
        Assert.assertTrue(client.field.isEmpty());

        client.field.setValue("b");
        Assert.assertEquals("a", getSharedValue("value"));
    }

    @Test
    public void clearTopic_clientsNotConnected() {
        client.bind();
        client.attach();
        client.binder.setTopic(null, () -> null);

        client2.bind();
        client2.attach();
        client2.binder.setTopic(null, () -> null);

        client2.field.setValue("b");

        Assert.assertTrue(client.field.isEmpty());
        Assert.assertFalse(client.field.hasListener(AttachEvent.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readBean_operationNotSupported() {
        client.binder.readBean(new TestBean());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setBean_operationNotSupported() {
        client.binder.setBean(new TestBean());
    }

    @Test
    public void collaborationMapValueEncodedAsString() {
        client.bind();
        client.attach();
        client.field.setValue("foo");

        Object mapValue = map.get("value");
        Assert.assertThat(mapValue, CoreMatchers.instanceOf(String.class));
        Assert.assertThat((String) mapValue,
                CoreMatchers.containsString("foo"));
    }

    @Test
    public void simpleBinding_typeInfered() {
        client.bind();
        Assert.assertEquals(String.class,
                client.binder.getPropertyType("value"));
    }

    @Test
    public void simpleBindingWithNullRepresentation_typeInfered() {
        client.binder.forField(client.field).withNullRepresentation("foo")
                .bind("value");
        Assert.assertEquals(String.class,
                client.binder.getPropertyType("value"));
    }

    @Test(expected = IllegalStateException.class)
    public void bindingGenericFieldWithConverter_explicitTypeDefinitionRequired() {
        client.binder.forField(new GenericTestField<>())
                .withConverter(String::valueOf, Integer::valueOf).bind("value");
    }

    @Test
    public void bindingGenericFieldWithConverter_explicitTypeProvided_propertyTypeAsDefined() {
        client.binder.forField(new GenericTestField<>(), Integer.class)
                .withConverter(String::valueOf, Integer::valueOf).bind("value");
        Assert.assertEquals(Integer.class,
                client.binder.getPropertyType("value"));
    }

    @Test
    public void complexTypeWithExplicitDefinition_valueSerializedAndDeserializedProperly() {
        GenericTestField<List<Double>> field = new GenericTestField<>();

        client.binder.forCollectionField(field, List.class, Double.class)
                .withNullRepresentation(Collections.emptyList())
                .withConverter(presentationValue -> presentationValue.stream()
                        .map(String::valueOf).collect(Collectors.joining(",")),
                        modelValue -> Stream.of(modelValue.split(","))
                                .map(Double::valueOf)
                                .collect(Collectors.toList()))
                .bind("value");
        client.attach(field);

        field.setValue(Arrays.asList(1d, 0.1d));
        FieldState fieldState = CollaborationBinderUtil.getFieldState(
                topicConnection, "value",
                ReflectTools.createParameterizedType(List.class, Double.class));
        Assert.assertEquals(Arrays.asList(1d, 0.1d), fieldState.value);

        CollaborationBinderUtil.setFieldValue(topicConnection, "value",
                Arrays.asList(0.1d, 1d));
        Assert.assertEquals(Arrays.asList(0.1d, 1d), field.getValue());
    }

    @Test
    public void complexTypeWithCallbacks_valueSerializedAndDeserializedProperly() {
        GenericTestField<TestBean> field = new GenericTestField<>();

        client.binder.forField(field, TestBean::getValue, TestBean::new)
                // Converter to allow using the existing bean property with the
                // binder that is already set up
                .withConverter(TestBean::getValue, TestBean::new).bind("value");
        client.attach(field);

        Assert.assertEquals(String.class,
                client.binder.getPropertyType("value"));

        field.setValue(new TestBean("Lorem"));
        FieldState fieldState = CollaborationBinderUtil
                .getFieldState(topicConnection, "value", String.class);
        Assert.assertEquals("Lorem", fieldState.value);

        CollaborationBinderUtil.setFieldValue(topicConnection, "value",
                "Ipsum");
        Assert.assertEquals("Ipsum", field.getValue().getValue());
    }

    @Test
    public void bindInstanceFields_simpleCase_fieldHasExpectedType() {
        class Target {
            private TestField value = new TestField();
        }

        client.binder.bindInstanceFields(new Target());

        Assert.assertEquals(String.class,
                client.binder.getPropertyType("value"));
    }

    @Test(expected = IllegalStateException.class)
    public void bindInstanceFields_converterNotExplicitType_throws() {
        class Target {
            private GenericTestField<Integer> value = new GenericTestField<>();
        }

        Target target = new Target();
        client.binder.forMemberField(target.value)
                .withConverter(String::valueOf, Integer::valueOf);
        client.binder.bindInstanceFields(target);
    }

    @Test
    public void bindInstanceFields_converterWithExplicitType_fieldHasExpectedType() {
        class Target {
            private GenericTestField<Integer> value = new GenericTestField<>();
        }

        Target target = new Target();
        client.binder.forMemberField(target.value, Integer.class)
                .withConverter(String::valueOf, Integer::valueOf);
        client.binder.bindInstanceFields(target);

        Assert.assertEquals(Integer.class,
                client.binder.getPropertyType("value"));
    }

    @Test(expected = IllegalStateException.class)
    public void unsupportedExplicitType_throwsFromForField() {
        client.binder.forField(new GenericTestField<>(), TestBean.class);
    }

    @Test(expected = IllegalStateException.class)
    public void unsupportedPropertyType_throwsWhenBinding() {
        class BeanWithUnsupportedPropertyType {
            public void setValue(TestBean value) {

            }

            public TestBean getValue() {
                return null;
            }
        }

        CollaborationBinder<BeanWithUnsupportedPropertyType> binder = new CollaborationBinder<>(
                BeanWithUnsupportedPropertyType.class, client.user);
        binder.forField(new GenericTestField<TestBean>()).bind("value");
    }

    @Test(expected = IllegalStateException.class)
    public void unsupportedTypeParameter_rejected() {
        client.binder.forCollectionField(new GenericTestField<List<TestBean>>(),
                List.class, TestBean.class);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("rawtypes")
    public void rawFieldType_rejected() {
        client.binder.forField(new GenericTestField<List>(), List.class);
    }

    @Test(expected = NullPointerException.class)
    public void nullFieldType_rejected() {
        client.binder.forField(client.field, null);
    }
}
