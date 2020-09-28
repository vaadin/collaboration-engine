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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BindingValidationStatusHandler;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.ReflectTools;
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

    private static final List<Class<?>> SUPPORTED_CLASS_TYPES = Arrays.asList(
            String.class, Boolean.class, Integer.class, Double.class,
            BigDecimal.class, LocalDate.class, LocalTime.class,
            LocalDateTime.class, Enum.class);
    private static final List<Class<?>> SUPPORTED_PARAMETERIZED_TYPES = Arrays
            .asList(List.class, Set.class);

    private static class FieldConfiguration {
        private final SerializableFunction<Object, Object> serializer;
        private final SerializableFunction<Object, Object> deserializer;
        private final Type deserializationType;

        private FieldConfiguration(Type deserializationType,
                SerializableFunction<Object, Object> serializer,
                SerializableFunction<Object, Object> deserializer) {
            this.serializer = Objects.requireNonNull(serializer,
                    "serializer cannot be null");
            this.deserializer = Objects.requireNonNull(deserializer,
                    "deserializer cannot be null");
            this.deserializationType = Objects
                    .requireNonNull(deserializationType, "type cannot be null");
        }

        private FieldConfiguration(Type deserializationType) {
            this(deserializationType, SerializableFunction.identity(),
                    SerializableFunction.identity());
        }

        private static void set(HasValue<?, ?> field, Type fieldType) {
            throwIfTypeNotSupported(fieldType, null);
            FieldConfiguration configuration = new FieldConfiguration(
                    fieldType);
            configuration.store(field);
        }

        private static <T> void set(HasValue<?, T> field,
                SerializableFunction<T, String> serializer,
                SerializableFunction<String, T> deserializer) {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            FieldConfiguration configuration = new FieldConfiguration(
                    String.class, (SerializableFunction) serializer,
                    (SerializableFunction) deserializer);
            configuration.store(field);
        }

        private void store(HasValue<?, ?> field) {
            if (field instanceof Component) {
                Component fieldAsComponent = (Component) field;
                ComponentUtil.setData(fieldAsComponent,
                        FieldConfiguration.class, this);
            } else {
                throw new IllegalArgumentException(
                        "CollaborationBinder can only be used with component fields. The provided field is of type "
                                + field.getClass().getName());
            }
        }

        private static FieldConfiguration getAndClear(HasValue<?, ?> field) {
            if (field instanceof Component) {
                Component fieldAsComponent = (Component) field;
                FieldConfiguration wrapper = ComponentUtil
                        .getData(fieldAsComponent, FieldConfiguration.class);
                ComponentUtil.setData(fieldAsComponent,
                        FieldConfiguration.class, null);
                return wrapper;
            }

            return null;
        }
    }

    static final class FieldState {
        public final Object value;
        public final List<FocusedEditor> editors;

        public FieldState(Object value, List<FocusedEditor> editors) {
            this.value = value;
            this.editors = Collections
                    .unmodifiableList(new ArrayList<>(editors));
        }
    }

    /**
     * Maps the focused user to the index of the focused element inside the
     * field. The index is needed for components such as radio button group,
     * where the highlight should be displayed on an individual radio button
     * inside the group.
     */
    static final class FocusedEditor {
        public final UserInfo user;
        public final int fieldIndex;

        @JsonCreator
        public FocusedEditor(@JsonProperty("user") UserInfo user,
                @JsonProperty("fieldIndex") int fieldIndex) {
            this.user = user;
            this.fieldIndex = fieldIndex;
        }
    }

    protected static class CollaborationBindingBuilderImpl<BEAN, FIELDVALUE, TARGET>
            extends BindingBuilderImpl<BEAN, FIELDVALUE, TARGET> {

        private String propertyName = null;
        private boolean typeIsConverted = false;
        private FieldConfiguration explicitFieldConfiguration;

        protected CollaborationBindingBuilderImpl(
                CollaborationBinder<BEAN> binder, HasValue<?, FIELDVALUE> field,
                Converter<FIELDVALUE, TARGET> converterValidatorChain,
                BindingValidationStatusHandler statusHandler) {
            super(binder, field, converterValidatorChain, statusHandler);

            explicitFieldConfiguration = FieldConfiguration.getAndClear(field);
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

            registrations.add(FieldHighlighter.setupForField(field,
                    propertyName, getBinder()));

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

        @Override
        protected <NEWTARGET> BindingBuilder<BEAN, NEWTARGET> withConverter(
                Converter<TARGET, NEWTARGET> converter,
                boolean resetNullRepresentation) {
            if (resetNullRepresentation) {
                // Flag implies that this is a "real" converter
                typeIsConverted = true;
            }
            return super.withConverter(converter, resetNullRepresentation);
        }

        @Override
        public BindingBuilder<BEAN, TARGET> withNullRepresentation(
                TARGET nullRepresentation) {
            /*
             * The null representation is internally implemented as a converter,
             * even though it doesn't change the type of the value. Because of
             * this, we reset the flag to its previous value after it becomes
             * set by the super call that sets a converter.
             */
            boolean typeWasConverted = typeIsConverted;
            try {
                return super.withNullRepresentation(nullRepresentation);
            } finally {
                typeIsConverted = typeWasConverted;
            }
        }

    }

    private UserInfo localUser;

    private TopicConnection topic;
    private ComponentConnectionContext connectionContext;
    private Registration topicRegistration;

    private final Map<Binding<?, ?>, Registration> bindingRegistrations = new HashMap<>();
    private final Map<HasValue<?, ?>, String> fieldToPropertyName = new HashMap<>();
    private final Map<String, FieldConfiguration> propertyConfiguration = new HashMap<>();

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

    @Override
    protected BindingBuilder<BEAN, ?> configureBinding(
            BindingBuilder<BEAN, ?> baseBinding,
            PropertyDefinition<BEAN, ?> definition) {
        CollaborationBindingBuilderImpl<?, ?, ?> binding = (CollaborationBindingBuilderImpl<?, ?, ?>) baseBinding;

        FieldConfiguration configuration = findFieldConfiguration(binding,
                definition);

        propertyConfiguration.put(definition.getName(), configuration);
        return super.configureBinding(baseBinding, definition);
    }

    private static void throwIfTypeNotSupported(Type type,
            Supplier<String> contextSupplier) {
        Objects.requireNonNull(type, "Type cannot be null");
        if (type instanceof Class<?>) {
            if (isAssignableFromAny(SUPPORTED_CLASS_TYPES, (Class<?>) type)) {
                return;
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            if (isAssignableFromAny(SUPPORTED_PARAMETERIZED_TYPES,
                    (Class<?>) parameterizedType.getRawType())) {

                for (Type typeArgument : parameterizedType
                        .getActualTypeArguments()) {
                    throwIfTypeNotSupported(typeArgument, contextSupplier);
                }

                return;
            }
        }

        StringBuilder messageBuilder = new StringBuilder("The type ")
                .append(type.getTypeName());

        if (contextSupplier != null) {
            messageBuilder.append(' ').append(contextSupplier.get());
        }

        messageBuilder.append(" is not supported. Supported types are: ")
                .append(Stream
                        .concat(SUPPORTED_CLASS_TYPES.stream(),
                                SUPPORTED_PARAMETERIZED_TYPES.stream())
                        .map(Class::getName).collect(Collectors.joining(", ")))
                .append(". For collections, the element type must be among the supported types.");

        throw new IllegalStateException(messageBuilder.toString());
    }

    private static boolean isAssignableFromAny(List<Class<?>> types,
            Class<?> type) {
        return types.stream()
                .anyMatch(candidate -> candidate.isAssignableFrom(type));
    }

    private static FieldConfiguration findFieldConfiguration(
            CollaborationBindingBuilderImpl<?, ?, ?> builder,
            PropertyDefinition<?, ?> propertyDefinition) {
        if (builder.explicitFieldConfiguration != null) {
            // Use explicilty defined config if available
            return builder.explicitFieldConfiguration;
        }

        if (!builder.typeIsConverted) {
            /*
             * Can use the property type as long as there is no converter. A
             * converter would imply that the bean type is not the same as the
             * field type.
             */
            Class<?> propertyType = propertyDefinition.getType();

            throwIfTypeNotSupported(propertyType,
                    () -> "of property '" + builder.propertyName + "'");

            return new FieldConfiguration(propertyType);
        }

        throw new IllegalStateException(
                "Could not infer field type for property '"
                        + builder.propertyName
                        + "'. Configure the property using an overload of forField or forMemberField that allows explicitly defining the field type.");
    }

    private void onMapChange(MapChangeEvent event) {
        getBinding(event.getKey()).map(Binding::getField).ifPresent(field -> {
            String propertyName = event.getKey();

            FieldState fieldState = CollaborationBinderUtil.getFieldState(topic,
                    propertyName, getPropertyType(propertyName));

            setFieldValueFromFieldState(field, propertyName, fieldState);

            FieldHighlighter.setEditors(field, fieldState.editors, localUser);
        });
    }

    // non-private for testing purposes
    Type getPropertyType(String propertyName) {
        return propertyConfiguration.get(propertyName).deserializationType;
    }

    private void onConnectionDeactivate() {
        fieldToPropertyName.values().forEach(this::removeEditor);
        this.topic = null;
    }

    @SuppressWarnings("rawtypes")
    private void setFieldValueFromMap(String propertyName, HasValue field) {
        if (topic == null) {
            // the connection isn't activated.
            return;
        }
        setFieldValueFromFieldState(field, propertyName,
                CollaborationBinderUtil.getFieldState(topic, propertyName,
                        getPropertyType(propertyName)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setFieldValueFromFieldState(HasValue field,
            String propertyName, FieldState fieldState) {
        Object stateValue = fieldState.value;
        if (stateValue == null) {
            field.clear();
        } else {
            FieldConfiguration fieldConfiguration = propertyConfiguration
                    .get(propertyName);
            field.setValue(fieldConfiguration.deserializer.apply(stateValue));
        }
    }

    @SuppressWarnings("rawtypes")
    private void setMapValueFromField(String propertyName, HasValue field) {
        if (topic != null) {
            Object value = field.isEmpty() ? null : field.getValue();
            if (value != null) {
                value = propertyConfiguration.get(propertyName).serializer
                        .apply(value);
            }
            CollaborationBinderUtil.setFieldValue(topic, propertyName, value);
        }
    }

    void addEditor(String propertyName, int fieldIndex) {
        if (topic != null) {
            CollaborationBinderUtil.addEditor(topic, propertyName, localUser,
                    fieldIndex);
        }
    }

    void removeEditor(String propertyName) {
        if (topic != null) {
            CollaborationBinderUtil.removeEditor(topic, propertyName,
                    localUser);
        }
    }

    @Override
    protected void removeBindingInternal(Binding<BEAN, ?> binding) {
        // Registration should be removed first, so it can e.g. remove editors
        // in map.
        // If the attached component is removed from the context first and the
        // connection is deactivated, registration removal can't update map
        // value.
        Registration registration = bindingRegistrations.remove(binding);
        if (registration != null) {
            registration.remove();
        }

        String propertyName = fieldToPropertyName.remove(binding.getField());
        propertyConfiguration.remove(propertyName);
        if (connectionContext != null) {
            connectionContext.removeComponent((Component) binding.getField());
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
            fieldToPropertyName.keySet()
                    .forEach(FieldHighlighter::removeEditors);
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

    /**
     * {@inheritDoc}
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. The value type to use for
     * deserializing the value is automatically determined based on the bean
     * property type. The type must be defined separately using another overload
     * of this method in case a converter is used or if the property type is
     * parameterized.
     *
     * @see #forField(HasValue, Class)
     * @see #forField(HasValue, SerializableFunction, SerializableFunction)
     * @see #forCollectionField(HasValue, Class, Class)
     */
    @Override
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forField(
            HasValue<?, FIELDVALUE> field) {
        // Overridden only to supplement documentation
        return super.forField(field);
    }

    /**
     * Creates a new binding for the given field and type. The returned builder
     * may be further configured before invoking
     * {@link BindingBuilder#bind(String)} which completes the binding. Until
     * {@code Binding.bind} is called, the binding has no effect.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows
     * explicitly defining the type to use. This is necessary when a converter
     * is used since it's then not possible to derive the type from the bean
     * property.
     *
     * @see #forField(HasValue)
     * @see #forField(HasValue, SerializableFunction, SerializableFunction)
     * @see #forCollectionField(HasValue, Class, Class)
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param fieldType
     *            the type of the field value, not <code>null</code>
     * @return the new binding builder
     */
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forField(
            HasValue<?, FIELDVALUE> field, Class<FIELDVALUE> fieldType) {
        FieldConfiguration.set(field, fieldType);
        return forField(field);
    }

    /**
     * Creates a new binding for the given field and conversion callbacks. The
     * returned builder may be further configured before invoking
     * {@link BindingBuilder#bind(String)} which completes the binding. Until
     * {@code Binding.bind} is called, the binding has no effect.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows defining
     * callbacks to convert between the field value and the value that is sent
     * over the network. This is necessary when using complex objects that are
     * not suitable to be sent as-is over the network.
     *
     * @see #forField(HasValue)
     * @see #forField(HasValue, Class)
     * @see #forCollectionField(HasValue, Class, Class)
     *
     * @param <FIELDVALUE>
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param serializer
     *            a callback that receives a field value (not <code>null</code>)
     *            and return the value to send over the network (not
     *            <code>null</code>). The callback cannot be <code>null</code>
     * @param deserializer
     *            a callback that receives a value produced by the serializer
     *            callback (not <code>null</code>) and return the field value to
     *            use. The callback cannot be <code>null</code>
     * @return the new binding builder
     */
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forField(
            HasValue<?, FIELDVALUE> field,
            SerializableFunction<FIELDVALUE, String> serializer,
            SerializableFunction<String, FIELDVALUE> deserializer) {
        FieldConfiguration.set(field, serializer, deserializer);
        return forField(field);
    }

    /**
     * Creates a new binding for the given collection field and types. The
     * returned builder may be further configured before invoking
     * {@link BindingBuilder#bind(String)} which completes the binding. Until
     * {@code Binding.bind} is called, the binding has no effect.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows
     * explicitly defining the collection type and element type to use.
     *
     * @see #forField(HasValue)
     * @see #forField(HasValue, Class)
     * @see #forField(HasValue, SerializableFunction, SerializableFunction)
     *
     * @param <FIELDVALUE>
     *            the base type of the collection
     * @param <ELEMENT>
     *            the collection element type
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param collectionType
     *            the base type of the collection, not <code>null</code>
     * @param elementType
     *            the type of the elements in the collection, not
     *            <code>null</code>
     * @return the new binding builder
     */
    public <FIELDVALUE extends Collection<ELEMENT>, ELEMENT> BindingBuilder<BEAN, FIELDVALUE> forCollectionField(
            HasValue<?, FIELDVALUE> field,
            Class<? super FIELDVALUE> collectionType,
            Class<ELEMENT> elementType) {
        FieldConfiguration.set(field, ReflectTools
                .createParameterizedType(collectionType, elementType));
        return forField(field);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. The value type to use for
     * deserializing the value is automatically determined based on the bean
     * property type. The type must be defined separately using another overload
     * of this method in case a converter is used or if the property type is
     * parameterized.
     *
     * @see #forMemberField(HasValue, Class)
     * @see #forMemberField(HasValue, SerializableFunction,
     *      SerializableFunction)
     * @see #forMemberCollectionField(HasValue, Class, Class)
     */
    @Override
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forMemberField(
            HasValue<?, FIELDVALUE> field) {
        // Overridden only to supplement documentation
        return super.forMemberField(field);
    }

    /**
     * Creates a new binding for the given field and type. The returned builder
     * may be further configured before invoking
     * {@link #bindInstanceFields(Object)}. Unlike with the
     * {@link #forField(HasValue)} method, no explicit call to
     * {@link BindingBuilder#bind(String)} is needed to complete this binding in
     * the case that the name of the field matches a field name found in the
     * bean.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows
     * explicitly defining the type to use. This is necessary when a converter
     * is used since it's then not possible to derive the type from the bean
     * property.
     *
     * @see #forMemberField(HasValue)
     * @see #forMemberField(HasValue, SerializableFunction,
     *      SerializableFunction)
     * @see #forMemberCollectionField(HasValue, Class, Class)
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param fieldType
     * @return the new binding builder
     *
     */
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forMemberField(
            HasValue<?, FIELDVALUE> field, Class<FIELDVALUE> fieldType) {
        FieldConfiguration.set(field, fieldType);
        return forMemberField(field);
    }

    /**
     * Creates a new binding for the given field and conversion callbacks. The
     * returned builder may be further configured before invoking
     * {@link #bindInstanceFields(Object)}. Unlike with the
     * {@link #forField(HasValue)} method, no explicit call to
     * {@link BindingBuilder#bind(String)} is needed to complete this binding in
     * the case that the name of the field matches a field name found in the
     * bean.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows defining
     * callbacks to convert between the field value and the value that is sent
     * over the network. This is necessary when using complex objects that are
     * not suitable to be sent as-is over the network.
     *
     * @see #forMemberField(HasValue)
     * @see #forMemberField(HasValue, Class)
     * @see #forMemberCollectionField(HasValue, Class, Class)
     *
     * @param <FIELDVALUE>
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param serializer
     *            a callback that receives a field value (not <code>null</code>)
     *            and return the value to send over the network (not
     *            <code>null</code>). The callback cannot be <code>null</code>
     * @param deserializer
     *            a callback that receives a value produced by the serializer
     *            callback (not <code>null</code>) and return the field value to
     *            use. The callback cannot be <code>null</code>
     * @return the new binding builder
     */
    public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forMemberField(
            HasValue<?, FIELDVALUE> field,
            SerializableFunction<FIELDVALUE, String> serializer,
            SerializableFunction<String, FIELDVALUE> deserializer) {
        FieldConfiguration.set(field, serializer, deserializer);
        return forField(field);
    }

    /**
     * Creates a new binding for the given collection field and types. The
     * returned builder may be further configured before invoking
     * {@link #bindInstanceFields(Object)}. Unlike with the
     * {@link #forField(HasValue)} method, no explicit call to
     * {@link BindingBuilder#bind(String)} is needed to complete this binding in
     * the case that the name of the field matches a field name found in the
     * bean.
     * <p>
     * The field value will be sent over the network to synchronize the value
     * with other users also editing the same field. This method allows
     * explicitly defining the collection type and element type to use.
     *
     * @see #forMemberField(HasValue)
     * @see #forMemberField(HasValue, Class)
     * @see #forMemberField(HasValue, SerializableFunction,
     *      SerializableFunction)
     *
     * @param <FIELDVALUE>
     *            the base type of the collection
     * @param <ELEMENT>
     *            the collection element type
     * @param field
     *            the field to be bound, not <code>null</code>
     * @param collectionType
     *            the base type of the collection, not <code>null</code>
     * @param elementType
     *            the type of the elements in the collection, not
     *            <code>null</code>
     * @return the new binding builder
     */
    public <FIELDVALUE extends Collection<ELEMENT>, ELEMENT> BindingBuilder<BEAN, FIELDVALUE> forMemberCollectionField(
            HasValue<?, FIELDVALUE> field,
            Class<? super FIELDVALUE> collectionType,
            Class<ELEMENT> elementType) {
        FieldConfiguration.set(field, ReflectTools
                .createParameterizedType(collectionType, elementType));
        return forMemberField(field);
    }
}
