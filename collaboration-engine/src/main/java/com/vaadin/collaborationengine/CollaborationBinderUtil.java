/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.vaadin.collaborationengine.FormManager.FocusedEditor;

/**
 * Utility methods for {@link CollaborationBinder}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class CollaborationBinderUtil {

    static final String COLLABORATION_BINDER_MAP_NAME = FormManager.class
            .getName();

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
     *
     * @since 1.0
     */
    public static void setFieldValue(TopicConnection topicConnection,
            String propertyName, Object value) {

        Objects.requireNonNull(topicConnection,
                "Topic connection can't be null.");
        Objects.requireNonNull(propertyName, "Property name can't be null.");

        getMap(topicConnection).put(propertyName, value);
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
     *
     * @since 1.0
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
     *
     * @since 1.0
     */
    public static void addEditor(TopicConnection topicConnection,
            String propertyName, UserInfo user, int fieldIndex) {

        Objects.requireNonNull(topicConnection,
                "Topic connection can't be null.");
        Objects.requireNonNull(propertyName, "Property name can't be null.");
        Objects.requireNonNull(user, "User can't be null.");

        CollaborationList list = topicConnection
                .getNamedList(COLLABORATION_BINDER_MAP_NAME);
        ListOperation operation = ListOperation
                .insertLast(new FocusedEditor(user, fieldIndex, propertyName))
                .withScope(EntryScope.CONNECTION);
        list.apply(operation);
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
        CollaborationList list = getList(topicConnection);
        list.getKeys().forEach(key -> {
            FocusedEditor editor = list.getItem(key, FocusedEditor.class);
            if (editor.propertyName.equals(propertyName)
                    && editor.user.equals(user)) {
                list.set(key, null);
            }
        });
    }

    static JsonNode getFieldValue(TopicConnection topic, String propertyName) {
        JsonNode result = getMap(topic).get(propertyName, JsonNode.class);
        return result != null ? result : NullNode.getInstance();
    }

    static CollaborationMap getMap(TopicConnection topic) {
        return topic.getNamedMap(COLLABORATION_BINDER_MAP_NAME);
    }

    static CollaborationList getList(TopicConnection topic) {
        return topic.getNamedList(COLLABORATION_BINDER_MAP_NAME);
    }

}
