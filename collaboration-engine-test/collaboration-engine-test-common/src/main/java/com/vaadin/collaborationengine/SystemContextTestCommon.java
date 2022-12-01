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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.messages.testbench.MessageElement;
import com.vaadin.flow.component.messages.testbench.MessageListElement;

public class SystemContextTestCommon extends AbstractCollaborativeViewTest {

    @Override
    public String getRoute() {
        return "system";
    }

    @Test
    public void messageSubmittedToServlet_visibleInView() throws IOException {

        MessageListElement messageList = $(MessageListElement.class).first();

        Assert.assertEquals("Sanity check", Collections.emptyList(),
                messageList.getMessageElements());

        submitToServlet("Hello world");

        waitUntil(driver -> !messageList.getMessageElements().isEmpty(), 3);
        List<MessageElement> messages = messageList.getMessageElements();
        Assert.assertEquals(1, messages.size());

        MessageElement messageElement = messages.get(0);
        Assert.assertEquals("Hello world", messageElement.getText());
    }

    private void submitToServlet(String message) throws IOException {
        URL url = new URL(getURL("submit"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200) {
                throw new IOException(connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }
}
