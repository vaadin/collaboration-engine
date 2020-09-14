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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonUtil {

    private JsonUtil() {
    }

    static String usersToJson(List<UserInfo> users) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(users);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to encode the list of users as a JSON string.", e);
        }
    }

    static List<UserInfo> jsonToUsers(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<UserInfo>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to parse the list of users from a JSON string.", e);
        }
    }
}
