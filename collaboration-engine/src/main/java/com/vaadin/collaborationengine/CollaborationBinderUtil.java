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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.collaborationengine.CollaborationBinder.FieldState;
import com.vaadin.collaborationengine.CollaborationBinder.FocusedEditor;

import static com.vaadin.collaborationengine.JsonUtil.EDITORS_TYPE_REF;

/**
 * Utility methods for {@link CollaborationBinder}.
 *
 * @author Vaadin Ltd
 */
public class CollaborationBinderUtil {

    private static final String COLLABORATION_BINDER_MAP_NAME = CollaborationBinder.class
            .getName();

    private static final FieldState EMPTY_FIELD_STATE = new FieldState(
            NullNode.getInstance(), Collections.emptyList());

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
                state -> withFieldValue(state, value));
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

        updateMapValue(topicConnection, propertyName, jsonNode -> withEditors(
                jsonNode,
                editors -> Stream.concat(editors.filter(
                        focusedEditor -> !focusedEditor.user.equals(user)),
                        Stream.of(new FocusedEditor(user, fieldIndex)))));
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
                jsonNode -> withEditors(jsonNode, editors -> editors
                        .filter(editor -> !editor.user.equals(user))));
    }

    private static void updateMapValue(TopicConnection topicConnection,
            String propertyName, Function<ObjectNode, ObjectNode> updater) {
        CollaborationMap map = getMap(topicConnection);
        ObjectNode oldValue = map.get(propertyName, ObjectNode.class);
        JsonNode newValue = updater.apply(oldValue);
        map.replace(propertyName, oldValue, newValue).thenAccept(success -> {
            if (!success) {
                updateMapValue(topicConnection, propertyName, updater);
            }
        });
    }

    static FieldState getFieldState(TopicConnection topic,
            String propertyName) {
        FieldState fieldState = getMap(topic).get(propertyName,
                FieldState.class);
        return fieldState == null ? EMPTY_FIELD_STATE : fieldState;
    }

    static CollaborationMap getMap(TopicConnection topic) {
        return topic.getNamedMap(COLLABORATION_BINDER_MAP_NAME);
    }

    private static ObjectNode withFieldValue(ObjectNode fieldState,
            Object newValue) {
        if (fieldState == null) {
            fieldState = (ObjectNode) JsonUtil.toJsonNode(EMPTY_FIELD_STATE);
        }

        ObjectNode jsonNode = fieldState.deepCopy();
        jsonNode.set("value", JsonUtil.toJsonNode(newValue));
        return jsonNode;
    }

    private static ObjectNode withEditors(ObjectNode fieldState,
            Function<Stream<FocusedEditor>, Stream<FocusedEditor>> updater) {
        if (fieldState == null) {
            fieldState = (ObjectNode) JsonUtil.toJsonNode(EMPTY_FIELD_STATE);
        }

        ObjectNode fieldStateNode = fieldState.deepCopy();
        List<FocusedEditor> editors = JsonUtil
                .toInstance(fieldStateNode.get("editors"), EDITORS_TYPE_REF);

        List<FocusedEditor> newEditors = updater.apply(editors.stream())
                .collect(Collectors.toList());
        fieldStateNode.set("editors", JsonUtil.toJsonNode(newEditors));
        return fieldStateNode;
    }
}
