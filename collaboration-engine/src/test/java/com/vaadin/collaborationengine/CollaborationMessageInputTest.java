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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.server.VaadinService;

public class CollaborationMessageInputTest {

    private static final String TOPIC_ID = "topic";

    public static class MessageInputTestClient {
        CollaborationEngine ce;
        final UI ui;
        final UserInfo user;
        CollaborationMessageList messageList;
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
            messageList = new CollaborationMessageList(this.user, null, null,
                    ce);
            messageInput = new CollaborationMessageInput(messageList);

        }

        private List<CollaborationMessage> getMessages() {
            AtomicReference<List<CollaborationMessage>> messages = new AtomicReference<>(
                    null);
            TestUtils.openEagerConnection(ce, topicId, (topicConnection) -> {
                CollaborationList messageList = topicConnection
                        .getNamedList(CollaborationMessageList.LIST_NAME);
                List<CollaborationMessage> list = messageList
                        .getItems(CollaborationMessage.class);
                messages.set(list);
            });
            return messages.get();
        }

        void attach() {
            ui.add(messageList, messageInput);
        }

        void setTopic(String topicId) {
            this.topicId = topicId;
            messageList.setTopic(topicId);
        }

        void submitMessage(String message) {
            MessageInput.SubmitEvent submitEvent = new MessageInput.SubmitEvent(
                    messageInput.getContent(), true, message);
            ComponentUtil.fireEvent(messageInput.getContent(), submitEvent);
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
        List<CollaborationMessage> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        CollaborationMessage message = messages.get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUser().getName());
        Assert.assertEquals("image1", message.getUser().getImage());
        Assert.assertEquals("abbreviation1",
                message.getUser().getAbbreviation());
        Assert.assertEquals(1, message.getUser().getColorIndex());
    }

    @Test
    public void initialState_componentDisabled() {
        client1.attach();
        Assert.assertFalse(client1.isEnabled());
    }

    @Test
    public void setTopicOnList_componentEnabled() {
        client1.attach();
        client1.setTopic("foo");
        Assert.assertTrue(client1.isEnabled());
    }

    @Test
    public void clearTopicOnList_componentDisabled() {
        client1.attach();
        client1.setTopic("foo");
        client1.setTopic(null);
        Assert.assertFalse(client1.isEnabled());
    }
}
