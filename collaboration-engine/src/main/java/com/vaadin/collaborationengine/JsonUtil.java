/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {

    public static final TypeReference<List<UserInfo>> LIST_USER_TYPE_REF = new TypeReference<List<UserInfo>>() {
    };
    public static final TypeReference<List<CollaborationMessageListItem>> LIST_MESSAGE_TYPE_REF = new TypeReference<List<CollaborationMessageListItem>>() {
    };
    public static final TypeReference<List<CollaborationBinder.FocusedEditor>> EDITORS_TYPE_REF = new TypeReference<List<CollaborationBinder.FocusedEditor>>() {
    };

    private static class CustomMapper extends ObjectMapper {
        public CustomMapper() {
            registerModule(new JavaTimeModule());
        }
    }

    private JsonUtil() {
    }

    static ObjectMapper createCustomMapper() {
        return new CustomMapper();
    }

    static JsonNode toJsonNode(Object value) {
        ObjectMapper mapper = createCustomMapper();
        try {
            return mapper.valueToTree(value);
        } catch (IllegalArgumentException e) {
            throw new JsonConversionException(
                    "Failed to encode the object to JSON node. "
                            + "Make sure the value contains a supported type.",
                    e);
        }
    }

    static <T> T toInstance(JsonNode jsonNode, Class<T> type) {
        if (jsonNode == null) {
            return null;
        }
        ObjectMapper objectMapper = createCustomMapper();
        try {
            return objectMapper.treeToValue(jsonNode, type);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException(
                    "Failed to parse the JSON node to " + type.getName(), e);
        }
    }

    static <T> T toInstance(JsonNode jsonNode, TypeReference<T> type) {
        return (T) toInstance(jsonNode, type.getType());
    }

    static Object toInstance(JsonNode jsonNode, Type type) {
        if (jsonNode == null) {
            return null;
        }
        ObjectMapper mapper = createCustomMapper();
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        try {
            return mapper.readValue(mapper.treeAsTokens(jsonNode), javaType);
        } catch (IOException e) {
            throw new JsonConversionException(
                    "Failed to parse the JSON node to "
                            + javaType.getTypeName(),
                    e);
        }
    }

}
