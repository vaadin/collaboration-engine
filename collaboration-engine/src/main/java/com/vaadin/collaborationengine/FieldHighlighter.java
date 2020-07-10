package com.vaadin.collaborationengine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.internal.JsonUtils;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

class FieldHighlighter {

    private FieldHighlighter() {
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
        editorJson.put("name", Objects.toString(editor.getUserName(), ""));
        editorJson.put("colorIndex", editor.getColorIndex());
        return editorJson;
    }
}
