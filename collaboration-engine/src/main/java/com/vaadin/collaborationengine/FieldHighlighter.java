package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationBinder.FocusedEditor;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

class FieldHighlighter {

    private FieldHighlighter() {
    }

    static Registration setupForField(HasValue<?, ?> field, String propertyName,
            CollaborationBinder binder) {

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

    private static Registration init(Element field) {
        Command initWithJS = () -> field.executeJs(
                "customElements.get('vaadin-field-highlighter').init(this)");
        if (field.getNode().isAttached()) {
            initWithJS.execute();
        }
        return field.addAttachListener(e -> initWithJS.execute());
    }

    static void setEditors(HasValue<?, ?> field, List<FocusedEditor> editors,
            UserInfo localUser) {
        if (field instanceof HasElement) {
            ((HasElement) field).getElement().executeJs(
                    "customElements.get('vaadin-field-highlighter').setUsers(this, $0)",
                    serialize(editors.stream()
                            .filter(editor -> !editor.user.equals(localUser))));
        }
    }

    static void removeEditors(HasValue<?, ?> field) {
        setEditors(field, Collections.emptyList(), null);
    }

    private static JsonArray serialize(Stream<FocusedEditor> editors) {
        return editors.map(FieldHighlighter::serialize)
                .collect(JsonUtils.asArray());
    }

    private static JsonObject serialize(FocusedEditor focusedEditor) {
        JsonObject editorJson = Json.createObject();
        editorJson.put("id", focusedEditor.user.getId());
        editorJson.put("name",
                Objects.toString(focusedEditor.user.getName(), ""));
        editorJson.put("colorIndex", focusedEditor.user.getColorIndex());
        editorJson.put("fieldIndex", focusedEditor.fieldIndex);
        return editorJson;
    }
}
