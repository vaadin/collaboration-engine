package com.vaadin.collaborationengine;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.messages.testbench.MessageElement;
import com.vaadin.flow.component.messages.testbench.MessageInputElement;
import com.vaadin.flow.component.messages.testbench.MessageListElement;
import com.vaadin.testbench.TestBenchTestCase;

public class MessageListTestCommon extends AbstractCollaborativeViewTest {

    private static final String HELLO_USERS = "hello users";

    @Override
    public String getRoute() {
        return "chat";
    }

    public static class ClientState {
        public MessageListElement messageList;
        public MessageInputElement messageInput;

        public ClientState(TestBenchTestCase client) {
            messageList = client.$(MessageListElement.class).first();
            messageInput = client.$(MessageInputElement.class).first();
        }

        public List<MessageElement> getMessages() {
            return messageList.getMessageElements();
        }

        public void submitMessage(String text) {
            messageInput.submit(text);
        }
    }

    protected ClientState client1;

    @Before
    public void init() {
        client1 = new ClientState(this);
        clickButton("next-topic");
    }

    @After
    public void reset() {
        clickButton("reset-user-counter");
    }

    @Test
    public void submitMessages_messageListUpdated() {

        Assert.assertEquals("Expected the message list to be empty", 0,
                client1.getMessages().size());
        client1.submitMessage(HELLO_USERS);

        Assert.assertEquals("Expected message list to contain one message", 1,
                client1.getMessages().size());
        assertMessage(HELLO_USERS, client1.getMessages().get(0));

        ClientState client2 = new ClientState(addClient());
        Assert.assertEquals(
                "Expected a second connected client to see the one message", 1,
                client2.getMessages().size());
        assertMessage(HELLO_USERS, client2.getMessages().get(0));

        final String hi = "hi";
        client2.submitMessage(hi);
        Assert.assertEquals("Expected the sender to get the additional message",
                2, client2.getMessages().size());
        Assert.assertEquals(
                "Expected the first client to get the additional message", 2,
                client1.getMessages().size());
        assertMessage(hi, client2.getMessages().get(1));
        assertMessage(hi, client1.getMessages().get(1));
    }

    @Test
    public void disconnectAndConnectToTopic_messageListUpdated() {

        client1.submitMessage(HELLO_USERS);
        waitUntil(webDriver -> !client1.getMessages().isEmpty(), 5);
        Assert.assertEquals("Expected message list to contain one message", 1,
                client1.getMessages().size());
        assertMessage(HELLO_USERS, client1.getMessages().get(0));

        clickButton("set-topic-null");

        waitUntil(webDriver -> client1.getMessages().isEmpty(), 5);
        Assert.assertEquals("Expected message list to be empty", 0,
                client1.getMessages().size());

        clickButton("set-topic");

        waitUntil(webDriver -> !client1.getMessages().isEmpty(), 5);
        Assert.assertEquals(
                "Expected message list to contain message after connecting to topic again",
                1, client1.getMessages().size());
        assertMessage(HELLO_USERS, client1.getMessages().get(0));
    }

    private void clickButton(String id) {
        $("button").id(id).click();
    }

    private void assertMessage(String expected, MessageElement actual) {
        Assert.assertEquals(String
                .format("Expected a message with the text '%s'", expected),
                expected, actual.getText());
    }
}
