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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JsonUtil {

    static final String CHANGE_TYPE = "type";

    static final String CHANGE_NAME = "name";

    static final String CHANGE_KEY = "key";

    static final String CHANGE_VALUE = "value";

    static final String CHANGE_OLD_VALUE = "old-value";

    static final String CHANGE_EXPECTED_VALUE = "expected-value";

    static final String CHANGE_TYPE_PUT = "m-put";

    static final String CHANGE_TYPE_APPEND = "l-append";

    static final String CHANGE_ITEM = "item";

    public static final TypeReference<List<UserInfo>> LIST_USER_TYPE_REF = new TypeReference<List<UserInfo>>() {
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

    public static ObjectNode createPutChange(String name, String key,
            Object expectedValue, Object value) {
        ObjectMapper objectMapper = createCustomMapper();
        ObjectNode change = objectMapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_PUT);
        change.put(CHANGE_NAME, name);
        change.put(CHANGE_KEY, key);
        change.set(CHANGE_VALUE, toJsonNode(value));
        if (expectedValue != null) {
            change.set(CHANGE_EXPECTED_VALUE, toJsonNode(expectedValue));
        }
        return change;
    }

    public static ObjectNode createAppendChange(String name, Object item) {
        ObjectMapper objectMapper = createCustomMapper();
        ObjectNode change = objectMapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_APPEND);
        change.put(CHANGE_NAME, name);
        change.set(CHANGE_ITEM, toJsonNode(item));
        return change;
    }
}
