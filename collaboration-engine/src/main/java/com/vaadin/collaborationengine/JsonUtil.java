/*
 * Copyright (C) 2021 Vaadin Ltd
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility methods to create JSON nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JsonUtil {

    static final String CHANGE_TYPE = "type";

    static final String CHANGE_NAME = "name";

    static final String CHANGE_KEY = "key";

    static final String CHANGE_VALUE = "value";

    static final String CHANGE_OLD_VALUE = "old-value";

    static final String CHANGE_EXPECTED_ID = "expected-id";

    static final String CHANGE_EXPECTED_VALUE = "expected-value";

    static final String CHANGE_TYPE_PUT = "m-put";

    static final String CHANGE_TYPE_APPEND = "l-append";

    static final String CHANGE_TYPE_LIST_SET = "l-set";

    static final String CHANGE_ITEM = "item";

    public static final String CHANGE_NODE_ID = "node-id";

    public static final String CHANGE_NODE_JOIN = "node-join";

    public static final String CHANGE_NODE_LEAVE = "node-leave";

    public static final String CHANGE_SCOPE_OWNER = "scope-owner";

    public static final TypeReference<List<UserInfo>> LIST_USER_TYPE_REF = new TypeReference<List<UserInfo>>() {
    };
    public static final TypeReference<List<CollaborationBinder.FocusedEditor>> EDITORS_TYPE_REF = new TypeReference<List<CollaborationBinder.FocusedEditor>>() {
    };

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    private JsonUtil() {
    }

    static ObjectMapper getObjectMapper() {
        return mapper;
    }

    static JsonNode toJsonNode(Object value) {
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
        try {
            return mapper.treeToValue(jsonNode, type);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException(
                    "Failed to parse the JSON node to " + type.getName(), e);
        }
    }

    static <T> Function<JsonNode, T> fromJsonConverter(Class<T> type) {
        Objects.requireNonNull(type, "The type can't be null");
        return jsonNode -> toInstance(jsonNode, type);
    }

    static <T> T toInstance(JsonNode jsonNode, TypeReference<T> type) {
        Objects.requireNonNull(type, "The type reference cannot be null");
        return (T) toInstance(jsonNode, type.getType());
    }

    static <T> Function<JsonNode, T> fromJsonConverter(TypeReference<T> type) {
        return jsonNode -> toInstance(jsonNode, type);
    }

    static Object toInstance(JsonNode jsonNode, Type type) {
        if (jsonNode == null) {
            return null;
        }
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

    static UUID toUUID(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        } else {
            return UUID.fromString(jsonNode.asText());
        }
    }

    static ObjectNode createPutChange(String name, String key,
            Object expectedValue, Object value, UUID scopeOwnerId) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_PUT);
        change.put(CHANGE_NAME, name);
        change.put(CHANGE_KEY, key);
        change.set(CHANGE_VALUE, toJsonNode(value));
        if (scopeOwnerId != null) {
            change.put(CHANGE_SCOPE_OWNER, scopeOwnerId.toString());
        }
        if (expectedValue != null) {
            change.set(CHANGE_EXPECTED_VALUE, toJsonNode(expectedValue));
        }
        return change;
    }

    static ObjectNode createAppendChange(String name, Object item,
            UUID scopeOwnerId) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_APPEND);
        change.put(CHANGE_NAME, name);
        change.set(CHANGE_ITEM, toJsonNode(item));
        if (scopeOwnerId != null) {
            change.put(CHANGE_SCOPE_OWNER, scopeOwnerId.toString());
        }
        return change;
    }

    static ObjectNode createListSetChange(String name, String id, Object value,
            UUID scopeOwnerId) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_LIST_SET);
        change.put(CHANGE_NAME, name);
        change.put(CHANGE_KEY, id);
        change.set(CHANGE_VALUE, toJsonNode(value));
        if (scopeOwnerId != null) {
            change.put(CHANGE_SCOPE_OWNER, scopeOwnerId.toString());
        }
        return change;
    }

    /**
     * Creates a JSON payload of a node join event.
     *
     * @param id
     *            the node id, not <code>null</code>
     * @return the payload
     */
    public static ObjectNode createNodeJoin(UUID id) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put(JsonUtil.CHANGE_TYPE, JsonUtil.CHANGE_NODE_JOIN);
        payload.put(JsonUtil.CHANGE_NODE_ID, id.toString());
        return payload;
    }

    /**
     * Creates a JSON payload of a node leave event.
     *
     * @param id
     *            the node id, not <code>null</code>
     * @return the payload
     */
    public static ObjectNode createNodeLeave(UUID id) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put(JsonUtil.CHANGE_TYPE, JsonUtil.CHANGE_NODE_LEAVE);
        payload.put(JsonUtil.CHANGE_NODE_ID, id.toString());
        return payload;
    }
}
