package com.vaadin.collaborationengine;

import java.time.Instant;

import org.junit.Test;

import com.vaadin.collaborationengine.util.TestUtils;

import static org.junit.Assert.assertEquals;

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
