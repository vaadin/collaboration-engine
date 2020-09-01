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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BindingValidationStatusHandler;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

import static com.vaadin.collaborationengine.CollaborationBinderUtil.getMap;

/**
 * Extension of {@link Binder} for creating collaborative forms with
 * {@link CollaborationEngine}. In addition to Binder's data binding mechanism,
 * CollaborationBinder synchronizes the field values between clients which are
 * connected to the same topic via {@link TopicConnection}.
 *
 * @author Vaadin Ltd
 *
 * @param <BEAN>
 *            the bean type
 */
public class CollaborationBinder<BEAN> extends Binder<BEAN> {

    static final class FieldState {
        public final Object value;
        public final List<UserInfo> editors;

        public FieldState(Object value, List<UserInfo> editors) {
            this.value = value;
            this.editors = Collections
                    .unmodifiableList(new ArrayList<>(editors));
        }

        public FieldState(Object value, Stream<UserInfo> editors) {
            this.value = value;
            this.editors = Collections
                    .unmodifiableList(editors.collect(Collectors.toList()));
        }
    }

    protected static class CollaborationBindingBuilderImpl<BEAN, FIELDVALUE, TARGET>
            extends BindingBuilderImpl<BEAN, FIELDVALUE, TARGET> {

        private String propertyName = null;

        protected CollaborationBindingBuilderImpl(
                CollaborationBinder<BEAN> binder, HasValue<?, FIELDVALUE> field,
                Converter<FIELDVALUE, TARGET> converterValidatorChain,
                BindingValidationStatusHandler statusHandler) {
            super(binder, field, converterValidatorChain, statusHandler);
        }

        @Override
        public Binding<BEAN, TARGET> bind(ValueProvider<BEAN, TARGET> getter,
                Setter<BEAN, TARGET> setter) {
            // Capture current propertyName
            final String propertyName = this.propertyName;

            if (propertyName == null) {
                throw new UnsupportedOperationException(
                        "A property name must always be provided when binding with the collaboration binder. "
                                + "Use bind(String propertyName) instead.");
            }

            Binding<BEAN, TARGET> binding = super.bind(getter, setter);

            HasValue<?, ?> field = binding.getField();

            ComponentConnectionContext connectionContext = getBinder().connectionContext;
            if (connectionContext != null) {
                connectionContext.addComponent((Component) field);
            }

            List<Registration> registrations = new ArrayList<>();

            registrations.add(field.addValueChangeListener(event -> getBinder()
                    .setMapValueFromField(propertyName, field)));

            if (field instanceof HasElement) {
                Element element = ((HasElement) field).getElement();

                registrations.add(FieldHighlighter.init(element));

                registrations
                        .add(element.addEventListener("vaadin-highlight-show",
                                e -> getBinder().addEditor(propertyName)));
                registrations
                        .add(element.addEventListener("vaadin-highlight-hide",
                                e -> getBinder().removeEditor(propertyName)));

                registrations.add(() -> getBinder().removeEditor(propertyName));
            }

            getBinder().bindingRegistrations.put(binding,
                    () -> registrations.forEach(Registration::remove));

            getBinder().setFieldValueFromMap(propertyName, field);
            getBinder().fieldToPropertyName.put(field, propertyName);

            return binding;
        }

        @Override
        public Binding<BEAN, TARGET> bind(String propertyName) {
            try {
                this.propertyName = propertyName;
                return super.bind(propertyName);
            } finally {
                this.propertyName = null;
            }
        }

        @Override
        protected CollaborationBinder<BEAN> getBinder() {
            return (CollaborationBinder<BEAN>) super.getBinder();
        }

    }

    private UserInfo localUser;

    private TopicConnection topic;
    private ComponentConnectionContext connectionContext;
    private Registration topicRegistration;

    private final Map<Binding<?, ?>, Registration> bindingRegistrations = new HashMap<>();
    private final Map<HasValue<?, ?>, String> fieldToPropertyName = new HashMap<>();

    /**
     * Creates a new collaboration binder. It uses reflection based on the
     * provided bean type to resolve bean properties.
     * <p>
     * The provided user information is used in the field editing indicators.
     * The name of the user will be displayed to other users when editing a
     * field, and the user's color index will be used to set the field's
     * highlight color.
     *
     * @param beanType
     *            the bean type to use, not <code>null</code>
     * @param localUser
     *            the information of the local user, not <code>null</code>
     */
    public CollaborationBinder(Class<BEAN> beanType, UserInfo localUser) {
        super(beanType);
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
    }

    private void onMapChange(MapChangeEvent event) {
        getBinding(event.getKey()).map(Binding::getField).ifPresent(field -> {
            setFieldValueFromMap(event.getKey(), field);

            List<UserInfo> editors = ((FieldState) event.getValue()).editors;
            FieldHighlighter.setEditors(field, editors, localUser);
        });
    }

    private void onConnectionDeactivate() {
        fieldToPropertyName.values().forEach(this::removeEditor);
    }

    @SuppressWarnings("rawtypes")
    private void setFieldValueFromMap(String propertyName, HasValue field) {
        if (topic == null) {
            // the connection isn't activated.
            return;
        }
        FieldState fieldState = (FieldState) getMap(topic).get(propertyName);
        Object value = fieldState != null ? fieldState.value : null;
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }

    @SuppressWarnings("rawtypes")
    private void setMapValueFromField(String propertyName, HasValue field) {
        Object value = field.isEmpty() ? null : field.getValue();
        if (topic != null) {
            CollaborationBinderUtil.setProperty(topic, propertyName, value);
        }
    }

    private void addEditor(String propertyName) {
        if (topic != null) {
            CollaborationBinderUtil.addEditor(topic, propertyName, localUser);
        }
    }

    private void removeEditor(String propertyName) {
        if (topic != null) {
            CollaborationBinderUtil.removeEditor(topic, propertyName,
                    localUser);
        }
    }

    @Override
    protected void removeBindingInternal(Binding<BEAN, ?> binding) {
        fieldToPropertyName.remove(binding.getField());
        if (connectionContext != null) {
            connectionContext.removeComponent((Component) binding.getField());
        }

        Registration registration = bindingRegistrations.remove(binding);
        if (registration != null) {
            registration.remove();
        }
        super.removeBindingInternal(binding);
    }

    @Override
    protected <FIELDVALUE, TARGET> BindingBuilder<BEAN, TARGET> doCreateBinding(
            HasValue<?, FIELDVALUE> field,
            Converter<FIELDVALUE, TARGET> converter,
            BindingValidationStatusHandler handler) {
        return new CollaborationBindingBuilderImpl<>(this, field, converter,
                handler);
    }

    /**
     * Not supported by the collaboration binder! It requires a property name
     * for binding, so the other overload
     * {@link CollaborationBinder#bind(HasValue, String)} should be used
     * instead.
     * <p>
     * See {@link Binder#bind(HasValue, ValueProvider, Setter)} to learn how to
     * use the method with the regular (non-collaboration) binder.
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to bind, not <code>null</code>
     * @param getter
     *            the function to get the value of the property to the field,
     *            not <code>null</code>
     * @param setter
     *            the function to write the field value to the property or
     *            <code>null</code> if read-only
     * @return the newly created binding
     * @throws UnsupportedOperationException
     *             as the method is not supported by the collaboration binder
     * @deprecated The method does not work with the collaboration binder. Use
     *             {@link CollaborationBinder#bind(HasValue, String)} instead.
     */
    @Override
    @Deprecated
    public <FIELDVALUE> Binding<BEAN, FIELDVALUE> bind(
            HasValue<?, FIELDVALUE> field,
            ValueProvider<BEAN, FIELDVALUE> getter,
            Setter<BEAN, FIELDVALUE> setter) {
        return super.bind(field, getter, setter);
    }

    /**
     * Binds the given field to the property with the given name, as described
     * in {@link Binder#bind(HasValue, String)}.
     * <p>
     * In addition, synchronizes the values with other collaboration binder
     * instances which are connected to the same topic.
     *
     * @param <FIELDVALUE>
     *            the value type of the field to bind
     * @param field
     *            the field to bind, not <code>null</code>
     * @param propertyName
     *            the name of the property to bind, not <code>null</code>
     * @return the newly created binding
     * @throws IllegalArgumentException
     *             if the property name is invalid
     * @throws IllegalArgumentException
     *             if the property has no accessible getter
     */
    @Override
    public <FIELDVALUE> Binding<BEAN, FIELDVALUE> bind(
            HasValue<?, FIELDVALUE> field, String propertyName) {
        return super.bind(field, propertyName);
    }

    /**
     * Binds the member fields found in the given object, as described in
     * {@link Binder#bindInstanceFields(Object)}.
     * <p>
     * In addition, synchronizes the values with other collaboration binder
     * instances which are connected to the same topic.
     *
     * @param objectWithMemberFields
     *            the object that contains (Java) member fields to bind
     * @throws IllegalStateException
     *             if there are incompatible HasValue&lt;T&gt; and property
     *             types
     */
    @Override
    public void bindInstanceFields(Object objectWithMemberFields) {
        super.bindInstanceFields(objectWithMemberFields);
    }

    /**
     * @deprecated This operation is not supported by the collaboration binder.
     *             You can instead provide the bean for populating the fields
     *             using {@link #setTopic}, and write the values back to the
     *             bean using {@link #writeBean}.
     */
    @Override
    @Deprecated
    public void setBean(BEAN bean) {
        throw new UnsupportedOperationException(
                "This operation is not supported by the collaboration binder. "
                        + "You can instead provide the bean for populating "
                        + "the fields with the setTopic method, and write the "
                        + "values back to the bean with the writeBean method.");
    }

    /**
     * @deprecated This operation is not supported by the collaboration binder.
     *             You can instead provide the bean for populating the fields
     *             using {@link #setTopic} to avoid overriding currently edited
     *             values. If you explicitly want to reset the field values for
     *             every user currently editing the fields, you can use
     *             {@link #reset}.
     */
    @Override
    @Deprecated
    public void readBean(BEAN bean) {
        throw new UnsupportedOperationException(
                "This operation is not supported by the collaboration binder. "
                        + "You can instead provide the bean for populating the fields "
                        + "with the setTopic method to avoid overriding currently edited values. "
                        + "If you explicitly want to reset the field values for every user "
                        + "currently editing the fields, you can use the reset method.");
    }

    /**
     * Resets collaborative fields with values from the bound properties of the
     * given bean. The values will be propagated to all collaborating users.
     *
     * @param bean
     *            the bean whose property values to read or {@code null} to
     *            clear bound fields
     */
    public void reset(BEAN bean) {
        super.readBean(bean);
    }

    UserInfo getLocalUser() {
        return localUser;
    }

    /**
     * Sets the topic to use with this binder and initializes the topic contents
     * if not already set. Setting a topic removes the connection to the
     * previous topic (if any) and resets all bindings based on values in the
     * new topic. The bean supplier is used to provide initial values for
     * bindings in case the topic doesn't yet contain any values.
     *
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     * @param initialBeanSupplier
     *            a supplier that is invoked to get a bean from which to read
     *            initial values. Only invoked if there are no property values
     *            in the topic, or if the topic id is <code>null</code>.
     */
    public void setTopic(String topicId,
            SerializableSupplier<BEAN> initialBeanSupplier) {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
            connectionContext = null;
            topic = null;
        }

        if (topicId == null) {
            super.readBean(initialBeanSupplier.get());
        } else {
            super.readBean(null);

            connectionContext = new ComponentConnectionContext();
            fieldToPropertyName.keySet().forEach(
                    field -> connectionContext.addComponent((Component) field));

            topicRegistration = CollaborationEngine.getInstance()
                    .openTopicConnection(connectionContext, topicId,
                            topic -> bindToTopic(topic, initialBeanSupplier));
        }

    }

    private Registration bindToTopic(TopicConnection topic,
            SerializableSupplier<BEAN> initialBeanSupplier) {
        this.topic = topic;

        getMap(topic).subscribe(this::onMapChange);

        initializeBindingsWithoutFieldState(initialBeanSupplier);

        fieldToPropertyName.forEach(
                (field, propName) -> setFieldValueFromMap(propName, field));

        return this::onConnectionDeactivate;
    }

    private void initializeBindingsWithoutFieldState(
            SerializableSupplier<BEAN> initialBeanSupplier) {
        CollaborationMap map = getMap(topic);

        List<String> propertiesWithoutFieldState = fieldToPropertyName.values()
                .stream().filter(propertyName -> map.get(propertyName) == null)
                .collect(Collectors.toList());

        if (propertiesWithoutFieldState.isEmpty()) {
            return;
        }

        BEAN initialBean = initialBeanSupplier.get();

        if (initialBean == null) {
            return;
        }

        propertiesWithoutFieldState.stream().map(this::getBinding)
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(binding -> binding.read(initialBean));
    }
}
