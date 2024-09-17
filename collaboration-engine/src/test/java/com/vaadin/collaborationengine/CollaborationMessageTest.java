/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

import com.vaadin.collaborationengine.util.TestUtils;

public class CollaborationMessageTest {
    @Test
    public void serializeMessage() {
        CollaborationMessage message = new CollaborationMessage(
                new UserInfo("local"), "foo", Instant.now());

        CollaborationMessage deserializedMessage = TestUtils.serialize(message);

        assertEquals(message.getUser(), deserializedMessage.getUser());
        assertEquals(message.getText(), deserializedMessage.getText());
        assertEquals(message.getTime(), deserializedMessage.getTime());
    }
}
