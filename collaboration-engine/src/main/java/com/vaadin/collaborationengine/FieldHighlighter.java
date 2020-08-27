package com.vaadin.collaborationengine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    static Registration init(Element field) {
        Command initWithJS = () -> field.executeJs(
                "customElements.get('vaadin-field-highlighter').init(this)");
        if (field.getNode().isAttached()) {
            initWithJS.execute();
        }
        return field.addAttachListener(e -> initWithJS.execute());
    }

    static void setEditors(HasValue<?, ?> field, List<UserInfo> editors,
            UserInfo localUser) {
        if (field instanceof HasElement) {
            ((HasElement) field).getElement().executeJs(
                    "customElements.get('vaadin-field-highlighter').setUsers(this, $0)",
                    serialize(editors.stream()
                            .filter(user -> !user.equals(localUser))));
        }
    }

    private static JsonArray serialize(Stream<UserInfo> editors) {
        return editors.map(FieldHighlighter::serialize)
                .collect(JsonUtils.asArray());
    }

    private static JsonObject serialize(UserInfo editor) {
        JsonObject editorJson = Json.createObject();
        editorJson.put("id", editor.getId());
        editorJson.put("name", Objects.toString(editor.getName(), ""));
        editorJson.put("colorIndex", editor.getColorIndex());
        return editorJson;
    }
}
