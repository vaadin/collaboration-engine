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

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility methods to create JSON nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class JsonUtil {

    static final String CHANGE_TYPE = "type";

    static final String CHANGE_NAME = "name";

    static final String CHANGE_KEY = "key";

    static final String CHANGE_POSITION_KEY = "position-key";

    static final String CHANGE_VALUE = "value";

    static final String CHANGE_EXPECTED_ID = "expected-id";

    static final String CHANGE_EXPECTED_VALUE = "expected-value";

    static final String CHANGE_CONDITIONS = "conditions";

    static final String CHANGE_VALUE_CONDITIONS = "value-conditions";

    static final String CHANGE_EMPTY = "empty";

    static final String CHANGE_TYPE_PUT = "m-put";

    static final String CHANGE_TYPE_REPLACE = "m-replace";

    static final String CHANGE_TYPE_INSERT_BEFORE = "l-insert-before";

    static final String CHANGE_TYPE_INSERT_AFTER = "l-insert-after";

    static final String CHANGE_TYPE_MOVE_BEFORE = "l-move-before";

    static final String CHANGE_TYPE_MOVE_AFTER = "l-move-after";

    static final String CHANGE_TYPE_LIST_SET = "l-set";

    static final String CHANGE_TYPE_MAP_TIMEOUT = "m-timeout";

    static final String CHANGE_TYPE_LIST_TIMEOUT = "l-timeout";

    static final String CHANGE_TYPE_LICENSE_USER = "license-user";

    static final String CHANGE_TYPE_LICENSE_EVENT = "license-event";

    static final String CHANGE_LICENSE_KEY = "license-key";

    static final String CHANGE_YEAR_MONTH = "year-month";

    static final String CHANGE_USER_ID = "user-id";

    static final String CHANGE_EVENT_NAME = "event-name";

    static final String CHANGE_EVENT_OCCURRENCE = "event-occurrence";

    public static final String CHANGE_NODE_ID = "node-id";

    public static final String CHANGE_NODE_ACTIVATE = "node-activate";

    public static final String CHANGE_NODE_DEACTIVATE = "node-deactivate";

    public static final String CHANGE_NODE_JOIN = "node-join";

    public static final String CHANGE_NODE_LEAVE = "node-leave";

    public static final String CHANGE_SCOPE_OWNER = "scope-owner";

    static final UUID TOPIC_SCOPE_ID = UUID
            .nameUUIDFromBytes(Topic.class.getName().getBytes());

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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

    static ObjectNode createReplaceChange(String name, String key,
            Object expectedValue, Object value) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_REPLACE);
        change.put(CHANGE_NAME, name);
        change.put(CHANGE_KEY, key);
        change.set(CHANGE_VALUE, toJsonNode(value));
        if (expectedValue != null) {
            change.set(CHANGE_EXPECTED_VALUE, toJsonNode(expectedValue));
        }
        return change;
    }

    static ObjectNode createListChange(ListOperation.OperationType type,
            String listName, String changeKey, String positionKey, Object item,
            UUID scopeOwnerId, Map<ListKey, ListKey> conditions,
            Map<ListKey, Object> valueConditions, Boolean empty) {
        ObjectNode change = mapper.createObjectNode();
        if (type == ListOperation.OperationType.INSERT_BEFORE) {
            change.put(CHANGE_TYPE, CHANGE_TYPE_INSERT_BEFORE);
        } else if (type == ListOperation.OperationType.INSERT_AFTER) {
            change.put(CHANGE_TYPE, CHANGE_TYPE_INSERT_AFTER);
        } else if (type == ListOperation.OperationType.MOVE_BEFORE) {
            change.put(CHANGE_TYPE, CHANGE_TYPE_MOVE_BEFORE);
        } else if (type == ListOperation.OperationType.MOVE_AFTER) {
            change.put(CHANGE_TYPE, CHANGE_TYPE_MOVE_AFTER);
        } else if (type == ListOperation.OperationType.SET) {
            change.put(CHANGE_TYPE, CHANGE_TYPE_LIST_SET);
        }
        change.put(CHANGE_NAME, listName);
        change.set(CHANGE_VALUE, toJsonNode(item));
        change.put(CHANGE_KEY, changeKey);
        change.put(CHANGE_POSITION_KEY, positionKey);
        conditions.forEach((refKey, otherKey) -> {
            ObjectNode condition = mapper.createObjectNode();
            condition.put(CHANGE_KEY,
                    refKey != null ? refKey.getKey().toString() : null);
            condition.put(CHANGE_POSITION_KEY,
                    otherKey != null ? otherKey.getKey().toString() : null);
            change.withArray(CHANGE_CONDITIONS).add(condition);
        });
        valueConditions.forEach((key, value) -> {
            ObjectNode valueCondition = mapper.createObjectNode();
            valueCondition.put(CHANGE_KEY,
                    key != null ? key.getKey().toString() : null);
            valueCondition.set(CHANGE_EXPECTED_VALUE, toJsonNode(value));
            change.withArray(CHANGE_VALUE_CONDITIONS).add(valueCondition);
        });
        if (empty != null) {
            change.put(CHANGE_EMPTY, empty);
        }
        if (scopeOwnerId != null) {
            change.put(CHANGE_SCOPE_OWNER, scopeOwnerId.toString());
        }
        return change;
    }

    static ObjectNode createMapTimeoutChange(String name, Duration timeout) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_MAP_TIMEOUT);
        change.put(CHANGE_NAME, name);
        change.set(CHANGE_VALUE, toJsonNode(timeout));
        return change;
    }

    static ObjectNode createListTimeoutChange(String name, Duration timeout) {
        ObjectNode change = mapper.createObjectNode();
        change.put(CHANGE_TYPE, CHANGE_TYPE_LIST_TIMEOUT);
        change.put(CHANGE_NAME, name);
        change.set(CHANGE_VALUE, toJsonNode(timeout));
        return change;
    }

    static ObjectNode createNodeActivate(UUID id) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put(JsonUtil.CHANGE_TYPE, JsonUtil.CHANGE_NODE_ACTIVATE);
        payload.put(JsonUtil.CHANGE_NODE_ID, id.toString());
        return payload;
    }

    static ObjectNode createNodeDeactivate(UUID id) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put(JsonUtil.CHANGE_TYPE, JsonUtil.CHANGE_NODE_DEACTIVATE);
        payload.put(JsonUtil.CHANGE_NODE_ID, id.toString());
        return payload;
    }

    static ObjectNode createUserEntry(String key, YearMonth month,
            String userId) {
        ObjectNode entry = mapper.createObjectNode();
        entry.put(CHANGE_TYPE, CHANGE_TYPE_LICENSE_USER);
        entry.put(CHANGE_LICENSE_KEY, key);
        entry.put(CHANGE_YEAR_MONTH, month.toString());
        entry.put(CHANGE_USER_ID, userId);
        return entry;
    }

    static ObjectNode createLicenseEvent(String key, String name,
            LocalDate latestOccurrence) {
        ObjectNode event = mapper.createObjectNode();
        event.put(CHANGE_TYPE, CHANGE_TYPE_LICENSE_EVENT);
        event.put(CHANGE_LICENSE_KEY, key);
        event.put(CHANGE_EVENT_NAME, name);
        event.put(CHANGE_EVENT_OCCURRENCE, latestOccurrence.toString());
        return event;
    }

    /**
     * Creates a JSON payload of a node join event.
     *
     * @param id
     *            the node id, not <code>null</code>
     * @return the payload
     */
    static ObjectNode createNodeJoin(UUID id) {
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
    static ObjectNode createNodeLeave(UUID id) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put(JsonUtil.CHANGE_TYPE, JsonUtil.CHANGE_NODE_LEAVE);
        payload.put(JsonUtil.CHANGE_NODE_ID, id.toString());
        return payload;
    }

    /**
     * Serializes a value to JSON string.
     *
     * @param value
     *            the value
     * @return the JSON string, or <code>null</code> if value is
     *         <code>null</code>
     */
    static String toString(Object value) {
        if (value == null) {
            return null;
        } else {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new JsonConversionException(
                        "Failed to serialize the object to string.", e);
            }
        }
    }

    /**
     * Deserializes a JSON string to {@link ObjectNode}.
     *
     * @param value
     *            the JSON string
     * @return the node, or <code>null</code> if value is <code>null</code>
     */
    static ObjectNode fromString(String value) {
        if (value == null) {
            return null;
        } else {
            try {
                return (ObjectNode) mapper.readTree(value);
            } catch (JsonProcessingException e) {
                throw new JsonConversionException(
                        "Failed to read the object from string.", e);
            }
        }
    }
}
