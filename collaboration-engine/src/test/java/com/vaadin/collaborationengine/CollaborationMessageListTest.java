package com.vaadin.collaborationengine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.server.VaadinService;

public class CollaborationMessageListTest {

    private static final String TOPIC_ID = "topic";

    public static class MessageListTestClient {
        final UI ui;
        final UserInfo user;
        CollaborationMessageList messageList;
        CollaborationEngine ce;
        String topicId = null;

        MessageListTestClient(int index, CollaborationEngine ce) {
            this(index, TOPIC_ID, ce);
        }

        MessageListTestClient(int index, String topicId,
                CollaborationEngine ce) {
            this.ce = ce;
            this.ui = new MockUI();
            this.user = new UserInfo("id" + index, "name" + index,
                    "image" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setColorIndex(index);
            messageList = new CollaborationMessageList(user, topicId, ce);
        }

        private List<MessageListItem> getMessages() {
            return messageList.getContent().getItems();
        }

        void attach() {
            ui.add(messageList);
        }

        void setTopic(String topicId) {
            this.topicId = topicId;
            messageList.setTopic(topicId);
        }

        public void sendMessage(String content) {
            TestUtils.openEagerConnection(ce, topicId, (topicConnection) -> {
                CollaborationMap messageMap = topicConnection
                        .getNamedMap(CollaborationMessageInput.MAP_NAME);
                sendMessage(messageMap, content);
            });
        }

        private void sendMessage(CollaborationMap map, String content) {
            List<MessageListItem> messages = map.get(
                    CollaborationMessageInput.MAP_KEY,
                    JsonUtil.LIST_MESSAGE_TYPE_REF);
            List<MessageListItem> newMessages = messages != null
                    ? new ArrayList<>(messages)
                    : new ArrayList<>();
            MessageListItem submittedMessage = new MessageListItem(content,
                    Instant.now(), user.getName(), user.getImage());
            newMessages.add(submittedMessage);
            map.replace(CollaborationMessageInput.MAP_KEY, messages,
                    newMessages).thenAccept(success -> {
                        if (!success) {
                            sendMessage(map, content);
                        }
                    });
        }
    }

    private MessageListTestClient client1;
    private MessageListTestClient client2;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        client1 = new MessageListTestClient(1, ce);
        client2 = new MessageListTestClient(2, ce);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void sendMessage_messageAppearsInMessageList() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        client1.sendMessage("new message");
        List<MessageListItem> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        MessageListItem message = messages.get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void sendMessage_messagePropagatesToOtherUser() {
        client1.attach();
        client2.attach();
        client1.setTopic(TOPIC_ID);
        client2.setTopic(TOPIC_ID);
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        Assert.assertEquals(Collections.emptyList(), client2.getMessages());
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        Assert.assertEquals(1, client2.getMessages().size());
        MessageListItem message = client2.getMessages().get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void joinTopic_existingMessagesAreDisplayed() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        Assert.assertEquals(0, client2.getMessages().size());
        client2.attach();
        client2.setTopic(TOPIC_ID);
        Assert.assertEquals(1, client2.getMessages().size());
        MessageListItem message = client2.getMessages().get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void topicSetToNull_contentIsCleared() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        client1.setTopic(null);
        Assert.assertEquals(0, client1.getMessages().size());
    }
}
