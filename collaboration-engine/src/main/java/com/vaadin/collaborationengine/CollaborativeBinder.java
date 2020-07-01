package com.vaadin.collaborationengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BindingValidationStatusHandler;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

/**
 * Extension of {@link Binder} for creating collaborative forms with
 * {@link CollaborationEngine}. In addition to Binder's data binding mechanism,
 * CollaborativeBinder synchronizes the field values between clients which are
 * connected to the same topic via {@link TopicConnection}.
 *
 * @param <BEAN>
 *            the bean type
 */
public class CollaborativeBinder<BEAN> extends Binder<BEAN> {

    protected static class CollaborativeBindingBuilderImpl<BEAN, FIELDVALUE, TARGET>
            extends BindingBuilderImpl<BEAN, FIELDVALUE, TARGET> {

        private String propertyName = null;

        protected CollaborativeBindingBuilderImpl(
                CollaborativeBinder<BEAN> binder, HasValue<?, FIELDVALUE> field,
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
                        "A property name must always be provided when binding with the collaborative binder. "
                                + "Use bind(String propertyName) instead.");
            }

            Binding<BEAN, TARGET> binding = super.bind(getter, setter);

            HasValue field = binding.getField();
            Registration registration = field
                    .addValueChangeListener(event -> getBinder()
                            .setMapValueFromField(propertyName, field));
            getBinder().bindingRegistrations.put(binding, registration);

            getBinder().setFieldValueFromMap(propertyName, field);

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
        protected CollaborativeBinder<BEAN> getBinder() {
            return (CollaborativeBinder<BEAN>) super.getBinder();
        }

    }

    private final CollaborativeMap map;
    private final Map<Binding<?, ?>, Registration> bindingRegistrations = new HashMap<>();

    /**
     * Creates a new collaborative binder. It uses reflection based on the
     * provided bean type to resolve bean properties. The provided collaborative
     * map is used for propagating value changes between clients.
     *
     * @param beanType
     *            the bean type to use, not <code>null</code>
     * @param map
     *            the collaborative map to use, not <code>null</code>
     */
    public CollaborativeBinder(Class<BEAN> beanType, CollaborativeMap map) {
        super(beanType);
        this.map = Objects.requireNonNull(map, "Map cannot be null");
        Registration mapRegistration = map.subscribe(this::onMapChange);
        map.getConnection().addRegistration(() -> {
            getBindings().forEach(this::removeBinding);
            mapRegistration.remove();
        });
    }

    private void onMapChange(MapChangeEvent event) {
        getBinding(event.getKey())
                .ifPresent(binding -> setFieldValueFromMap(event.getKey(),
                        binding.getField()));
    }

    @SuppressWarnings("rawtypes")
    private void setFieldValueFromMap(String propertyName, HasValue field) {
        Object value = map.get(propertyName);
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }

    @SuppressWarnings("rawtypes")
    private void setMapValueFromField(String propertyName, HasValue field) {
        map.put(propertyName, field.isEmpty() ? null : field.getValue());
    }

    @Override
    protected void removeBindingInternal(Binding<BEAN, ?> binding) {
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
        return new CollaborativeBindingBuilderImpl<>(this, field, converter,
                handler);
    }

    /**
     * Not supported by the collaborative binder! It requires a property name
     * for binding, so the other overload
     * {@link CollaborativeBinder#bind(HasValue, String)} should be used
     * instead.
     * <p>
     * See {@link Binder#bind(HasValue, ValueProvider, Setter)} to learn how to
     * use the method with the regular (non-collaborative) binder.
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
     *             as the method is not supported by the collaborative binder
     * @deprecated The method does not work with the collaborative binder. Use
     *             {@link CollaborativeBinder#bind(HasValue, String)} instead.
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
     * In addition, synchronizes the values with other collaborative binder
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
     * In addition, synchronizes the values with other collaborative binder
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
}
