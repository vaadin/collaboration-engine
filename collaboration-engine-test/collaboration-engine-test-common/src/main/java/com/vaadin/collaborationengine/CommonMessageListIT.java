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

public class CommonMessageListIT extends AbstractCollaborativeViewTest {

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
        $("button").id("next-topic").click();
    }

    @After
    public void reset() {
        $("button").id("reset-user-counter").click();
    }

    @Test
    public void submitMessages_messageListUpdated() {

        Assert.assertEquals("Expected the message list to be empty", 0,
                client1.getMessages().size());
        client1.submitMessage("hello users");

        Assert.assertEquals("Expected message list to contain one message", 1,
                client1.getMessages().size());
        Assert.assertEquals("Expected a message with the text 'hello users'",
                "hello users", client1.getMessages().get(0).getText());

        ClientState client2 = new ClientState(addClient());
        Assert.assertEquals(
                "Expected a second connected client to see the one message", 1,
                client2.getMessages().size());
        Assert.assertEquals(
                "Expected a a second connected client to see the text 'hello users'",
                "hello users", client2.getMessages().get(0).getText());

        client2.submitMessage("hi");
        Assert.assertEquals("Expected the sender to get the additional message",
                2, client2.getMessages().size());
        Assert.assertEquals(
                "Expected the first client to get the additional message", 2,
                client1.getMessages().size());
        Assert.assertEquals("Expected the sender to see the text 'hi'", "hi",
                client2.getMessages().get(1).getText());
        Assert.assertEquals("Expected the first client to see the text 'hi'",
                "hi", client1.getMessages().get(1).getText());
    }

    @Test
    public void disconnectAndConnectToTopic_messageListUpdated() {

        client1.submitMessage("hello users");
        Assert.assertEquals("Expected message list to contain one message", 1,
                client1.getMessages().size());
        Assert.assertEquals("Expected a message with the text 'hello users'",
                "hello users", client1.getMessages().get(0).getText());

        $("button").id("set-topic-null").click();

        Assert.assertEquals("Expected message list to be empty", 0,
                client1.getMessages().size());

        $("button").id("set-topic").click();

        Assert.assertEquals(
                "Expected message list to contain message after connecting to topic again",
                1, client1.getMessages().size());
        Assert.assertEquals("Expected a message with the text 'hello users'",
                "hello users", client1.getMessages().get(0).getText());
    }
}
