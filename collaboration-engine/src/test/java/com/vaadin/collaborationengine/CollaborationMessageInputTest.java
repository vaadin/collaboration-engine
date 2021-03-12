package com.vaadin.collaborationengine;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.server.VaadinService;

public class CollaborationMessageInputTest {

    private static final String TOPIC_ID = "topic";

    public static class MessageInputTestClient {
        CollaborationEngine ce;
        final UI ui;
        final UserInfo user;
        CollaborationMessageInput messageInput;
        String topicId = null;

        MessageInputTestClient(int index, CollaborationEngine ce) {
            this(index, TOPIC_ID, ce);
        }

        MessageInputTestClient(int index, String topicId,
                CollaborationEngine ce) {
            this.ce = ce;
            this.ui = new MockUI();
            this.user = new UserInfo("id" + index, "name" + index,
                    "image" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setColorIndex(index);
            messageInput = new CollaborationMessageInput(user, topicId, ce);

        }

        private List<CollaborationMessageListItem> getMessages() {
            AtomicReference<List<CollaborationMessageListItem>> messages = new AtomicReference<>(
                    null);
            TestUtils.openEagerConnection(ce, topicId, (topicConnection) -> {
                CollaborationList messageList = topicConnection
                        .getNamedList(CollaborationMessageInput.LIST_NAME);
                List<CollaborationMessageListItem> list = messageList
                        .getItems(CollaborationMessageListItem.class);
                messages.set(list);
            });
            return messages.get();
        }

        void attach() {
            ui.add(messageInput);
        }

        void setTopic(String topicId) {
            this.topicId = topicId;
            messageInput.setTopic(topicId);
        }

        void submitMessage(String message) {
            messageInput.submitMessage(new MessageInput.SubmitEvent(
                    messageInput.getContent(), true, message));
        }

        boolean isEnabled() {
            return messageInput.getContent().isEnabled();
        }
    }

    private CollaborationEngine ce;
    private MessageInputTestClient client1;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        ce = TestUtil.createTestCollaborationEngine(service);
        client1 = new MessageInputTestClient(1, ce);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void sendMessage_messageAppearsInTopic() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertTrue(client1.getMessages().isEmpty());
        client1.submitMessage("new message");
        List<CollaborationMessageListItem> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        CollaborationMessageListItem message = messages.get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUser().getName());
        Assert.assertEquals("image1", message.getUser().getImage());
        Assert.assertEquals("abbreviation1",
                message.getUser().getAbbreviation());
        Assert.assertEquals(1, message.getUser().getColorIndex());
    }

    @Test
    public void nonNullTopic_connectionNotActivated_componentDisabled() {
        Assert.assertFalse(client1.isEnabled());
    }

    @Test
    public void nonNullTopic_connectionActivated_componentEnabled() {
        client1.attach();
        Assert.assertTrue(client1.isEnabled());
    }

    @Test
    public void initialNullTopic_componentDisabled() {
        MessageInputTestClient client2 = new MessageInputTestClient(2, null,
                ce);
        client2.attach();
        Assert.assertFalse(client2.isEnabled());
    }

    @Test
    public void changeTopicToNull_componentDisabled() {
        client1.attach();
        client1.setTopic(null);
        Assert.assertFalse(client1.isEnabled());
    }

}
