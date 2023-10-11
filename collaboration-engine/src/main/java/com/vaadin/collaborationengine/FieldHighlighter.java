/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.FormManager.FocusedEditor;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.fieldhighlighter.FieldHighlighterInitializer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 * @since 1.0
 */
class FieldHighlighter extends FieldHighlighterInitializer
        implements Serializable {

    static Registration setupForField(HasValue<?, ?> field, String propertyName,
            CollaborationBinder<?> binder) {

        List<Registration> registrations = new ArrayList<>();

        if (field instanceof HasElement) {
            Element element = ((HasElement) field).getElement();
            registrations.add(init(element));

            registrations.add(
                    element.addEventListener("vaadin-highlight-show", e -> {

                        JsonObject eventDetail = e.getEventData()
                                .getObject("event.detail");
                        int fieldIndex = eventDetail != null
                                ? (int) eventDetail.getNumber("fieldIndex")
                                : 0;

                        binder.addEditor(propertyName, fieldIndex);

                    }).addEventData("event.detail"));

            registrations.add(element.addEventListener("vaadin-highlight-hide",
                    e -> binder.removeEditor(propertyName)));

            registrations.add(() -> binder.removeEditor(propertyName));
        }
        return () -> registrations.forEach(Registration::remove);
    }

    private final SerializableFunction<UserInfo, Integer> colorIndexProvider;

    FieldHighlighter(
            SerializableFunction<UserInfo, Integer> colorIndexProvider) {
        this.colorIndexProvider = colorIndexProvider;
    }

    void setEditors(HasValue<?, ?> field, List<FocusedEditor> editors,
            UserInfo localUser) {
        if (field instanceof HasElement) {
            ((HasElement) field).getElement().executeJs(
                    "customElements.get('vaadin-field-highlighter').setUsers(this, $0)",
                    serialize(editors.stream()
                            .filter(editor -> !editor.user.equals(localUser))));
        }
    }

    void removeEditors(HasValue<?, ?> field) {
        setEditors(field, Collections.emptyList(), null);
    }

    void addEditor(HasValue<?, ?> field, UserInfo user, int fieldIndex) {
        if (field instanceof HasElement) {
            ((HasElement) field).getElement().executeJs(
                    "customElements.get('vaadin-field-highlighter')"
                            + ".addUser(this, $0)",
                    serialize(user, fieldIndex));
        }
    }

    void removeEditor(HasValue<?, ?> field, UserInfo user, int fieldIndex) {
        if (field instanceof HasElement) {
            ((HasElement) field).getElement().executeJs(
                    "customElements.get('vaadin-field-highlighter')"
                            + ".removeUser(this, $0)",
                    serialize(user, fieldIndex));
        }
    }

    private JsonArray serialize(Stream<FocusedEditor> editors) {
        return editors.map(this::serialize).collect(JsonUtils.asArray());
    }

    private JsonObject serialize(FocusedEditor focusedEditor) {
        JsonObject editorJson = Json.createObject();
        editorJson.put("id", focusedEditor.user.getId());
        editorJson.put("name",
                Objects.toString(focusedEditor.user.getName(), ""));
        editorJson.put("colorIndex",
                colorIndexProvider.apply(focusedEditor.user));
        editorJson.put("fieldIndex", focusedEditor.fieldIndex);
        return editorJson;
    }

    private JsonObject serialize(UserInfo user, int fieldIndex) {
        JsonObject editorJson = Json.createObject();
        editorJson.put("id", user.getId());
        editorJson.put("name", Objects.toString(user.getName(), ""));
        editorJson.put("colorIndex", colorIndexProvider.apply(user));
        editorJson.put("fieldIndex", fieldIndex);
        return editorJson;
    }
}
