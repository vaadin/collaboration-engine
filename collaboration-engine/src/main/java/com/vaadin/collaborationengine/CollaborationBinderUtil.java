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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.collaborationengine.CollaborationBinder.FieldState;
import com.vaadin.collaborationengine.CollaborationBinder.FocusedEditor;

/**
 * Utility methods for {@link CollaborationBinder}.
 *
 * @author Vaadin Ltd
 */
public class CollaborationBinderUtil {

    private static class CustomMapper extends ObjectMapper {
        public CustomMapper() {
            registerModule(new JavaTimeModule());
        }
    }

    private static final String COLLABORATION_BINDER_MAP_NAME = CollaborationBinder.class
            .getName();

    private static final FieldState EMPTY_FIELD_STATE = new FieldState(null,
            Collections.emptyList());
    private static String EMPTY_FIELD_STATE_JSON;

    private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
            String.class, Boolean.class, Integer.class, Double.class,
            BigDecimal.class, LocalDate.class, LocalTime.class,
            LocalDateTime.class, Enum.class, List.class, Set.class);

    private static final List<Class<?>> SUPPORTED_TYPES_INSIDE_COLLECTION = Arrays
            .asList(String.class);

    private static final TypeReference EDITORS_TYPE_REF = new TypeReference<List<FocusedEditor>>() {
    };

    static {
        try {
            EMPTY_FIELD_STATE_JSON = new CustomMapper()
                    .writeValueAsString(EMPTY_FIELD_STATE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to encode the empty field state as a JSON string.",
                    e);
        }
    }

    private CollaborationBinderUtil() {
        // Utility methods only
    }

    /**
     * Sets the property value in the collaboration binders connected to the
     * same topic, updating the fields which are bound to that property for all
     * connected clients.
     * <p>
     * The collaboration binder already takes care of propagating field value
     * changes among collaborating users. This method may be useful when the
     * data changes outside a UI thread, and the new values should be made
     * visible to the users.
     *
     * @param topicConnection
     *            the topic connection, not {@code null}
     * @param propertyName
     *            the name of the property to update, not {@code null}
     * @param value
     *            the new value to set, can be {@code null} to clear the fields
     */
    public static void setFieldValue(TopicConnection topicConnection,
            String propertyName, Object value) {

        Objects.requireNonNull(topicConnection,
                "Topic connection can't be null.");
        Objects.requireNonNull(propertyName, "Property name can't be null.");

        updateMapValue(topicConnection, propertyName,
                state -> updateFieldValueInJson(state, value));
    }

    /**
     * Signals that the user is currently editing the field that is bound to the
     * property. This is indicated to the collaborating users by highlighting
     * the field.
     * <p>
     * If the user is already an editor of the field, this method does nothing.
     * <p>
     * The collaboration binder already takes care of updating the active
     * editors when users focus the fields. This method is needed only if you
     * need to explicitly control the users who are displayed as the active
     * editors.
     *
     * @param topicConnection
     *            the topic connection, not {@code null}
     * @param propertyName
     *            the name of the property bound to the edited field, not
     *            {@code null}
     * @param user
     *            information of the user to add as the editor of the field, not
     *            {@code null}
     */
    public static void addEditor(TopicConnection topicConnection,
            String propertyName, UserInfo user) {
        addEditor(topicConnection, propertyName, user, 0);
    }

    /**
     * Signals that the user is currently editing the field that is bound to the
     * property. This is indicated to the collaborating users by highlighting
     * the field.
     * <p>
     * If the user is already an editor of the field, this method does nothing.
     * <p>
     * The collaboration binder already takes care of updating the active
     * editors when users focus the fields. This method is needed only if you
     * need to explicitly control the users who are displayed as the active
     * editors.
     *
     * @param topicConnection
     *            the topic connection, not {@code null}
     * @param propertyName
     *            the name of the property bound to the edited field, not
     *            {@code null}
     * @param user
     *            information of the user to add as the editor of the field, not
     *            {@code null}
     * @param fieldIndex
     *            index of the focused element inside the field, for example a
     *            radio button inside a radio button group
     */
    public static void addEditor(TopicConnection topicConnection,
            String propertyName, UserInfo user, int fieldIndex) {

        Objects.requireNonNull(topicConnection,
                "Topic connection can't be null.");
        Objects.requireNonNull(propertyName, "Property name can't be null.");
        Objects.requireNonNull(user, "User can't be null.");

        updateMapValue(topicConnection, propertyName,
                jsonString -> updateEditorsInJson(jsonString,
                        editors -> Stream.concat(
                                editors.filter(
                                        focusedEditor -> !focusedEditor.user
                                                .equals(user)),
                                Stream.of(
                                        new FocusedEditor(user, fieldIndex)))));
    }

    /**
     * Signals that the user is not editing the field that is bound to the
     * property.
     * <p>
     * If the user is currently not an editor of the field, this method does
     * nothing.
     * <p>
     * The collaboration binder already takes care of updating the active
     * editors when users focus the fields. This method is needed only if you
     * need to explicitly control the users who are displayed as the active
     * editors.
     *
     * @param topicConnection
     *            the topic connection, not {@code null}
     * @param propertyName
     *            the name of the property bound to the edited field, not
     *            {@code null}
     * @param user
     *            information of the user to remove from the editors of the
     *            field, not {@code null}
     * @see #addEditor(TopicConnection, String, UserInfo)
     */
    public static void removeEditor(TopicConnection topicConnection,
            String propertyName, UserInfo user) {

        Objects.requireNonNull(topicConnection,
                "Topic connection can't be null.");
        Objects.requireNonNull(propertyName, "Property name can't be null.");
        Objects.requireNonNull(user, "User can't be null.");

        updateMapValue(topicConnection, propertyName,
                jsonString -> updateEditorsInJson(jsonString, editors -> editors
                        .filter(editor -> !editor.user.equals(user))));
    }

    private static void updateMapValue(TopicConnection topicConnection,
            String propertyName, Function<String, String> updater) {
        CollaborationMap map = getMap(topicConnection);
        while (true) {
            String oldValue = (String) map.get(propertyName);
            String newValue = updater.apply(oldValue);
            if (map.replace(propertyName, oldValue, newValue)) {
                return;
            }
        }
    }

    static FieldState getFieldState(TopicConnection topic, String propertyName,
            Class<?> valueType) {
        String json = (String) getMap(topic).get(propertyName);
        return jsonToFieldState(json, valueType);
    }

    static CollaborationMap getMap(TopicConnection topic) {
        return topic.getNamedMap(COLLABORATION_BINDER_MAP_NAME);
    }

    private static String updateFieldValueInJson(String fieldStateJson,
            Object newValue) {

        throwIfValueTypeNotSupported(newValue);

        if (fieldStateJson == null) {
            fieldStateJson = EMPTY_FIELD_STATE_JSON;
        }

        ObjectMapper objectMapper = new CustomMapper();
        try {
            ObjectNode jsonNode = (ObjectNode) objectMapper
                    .readTree(fieldStateJson);

            jsonNode.set("value", objectMapper.valueToTree(newValue));

            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update the field value.", e);
        }
    }

    private static String updateEditorsInJson(String fieldStateJson,
            Function<Stream<FocusedEditor>, Stream<FocusedEditor>> updater) {

        if (fieldStateJson == null) {
            fieldStateJson = EMPTY_FIELD_STATE_JSON;
        }

        ObjectMapper objectMapper = new CustomMapper();
        try {
            ObjectNode jsonNode = (ObjectNode) objectMapper
                    .readTree(fieldStateJson);

            List<FocusedEditor> editors = getEditors(objectMapper, jsonNode);

            List<FocusedEditor> newEditors = updater.apply(editors.stream())
                    .collect(Collectors.toList());

            jsonNode.set("editors", objectMapper.valueToTree(newEditors));

            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update the field editors.",
                    e);
        }
    }

    private static FieldState jsonToFieldState(String json,
            Class<?> valueType) {
        if (json == null) {
            return EMPTY_FIELD_STATE;
        }

        ObjectMapper objectMapper = new CustomMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            Object value = objectMapper.treeToValue(jsonNode.get("value"),
                    valueType);
            List<FocusedEditor> editors = getEditors(objectMapper, jsonNode);
            return new FieldState(value, editors);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse the field state from a JSON string.", e);
        }
    }

    private static List<FocusedEditor> getEditors(ObjectMapper objectMapper,
            JsonNode jsonNode) throws IOException {
        return objectMapper.readerFor(EDITORS_TYPE_REF)
                .readValue(jsonNode.get("editors"));
    }

    private static void throwIfValueTypeNotSupported(Object value) {
        if (value == null) {
            return;
        }

        if (!isInstanceOfAny(value, SUPPORTED_TYPES)) {
            throw new IllegalStateException(
                    "CollaborationBinder doesn't support the provided value type: "
                            + value.getClass().getSimpleName()
                            + ". The supported types are: "
                            + SUPPORTED_TYPES.stream().map(Class::getSimpleName)
                                    .collect(Collectors.toList()));
        }

        if ((value instanceof Collection) && ((Collection) value).stream()
                .anyMatch(entry -> !isInstanceOfAny(entry,
                        SUPPORTED_TYPES_INSIDE_COLLECTION))) {
            throw new IllegalStateException(
                    "CollaborationBinder doesn't support the provided value type inside a Collection: "
                            + value.getClass().getSimpleName()
                            + ". The supported types are: "
                            + SUPPORTED_TYPES_INSIDE_COLLECTION.stream()
                                    .map(Class::getSimpleName)
                                    .collect(Collectors.toList())
                            + " If you're using a multi select component, you need to change "
                            + "the component's value type to String and convert the values.");
        }
    }

    private static boolean isInstanceOfAny(Object object,
            List<Class<?>> types) {
        return types.stream().anyMatch(type -> type.isInstance(object));
    }

}
