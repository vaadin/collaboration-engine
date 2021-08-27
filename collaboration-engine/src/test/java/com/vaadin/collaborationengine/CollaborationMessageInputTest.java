package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.ReflectionUtils;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageListItem;
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

        private List<MessageListItem> getMessages() {
            return messageList.getContent().getItems();
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
        List<MessageListItem> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        MessageListItem message = messages.get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals("name1", message.getUserName());
        Assert.assertEquals("image1", message.getUserImage());
        Assert.assertEquals("abbreviation1", message.getUserAbbreviation());
        Assert.assertEquals(1, message.getUserColorIndex().intValue());
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

    private static List<String> blackListedMethods = Arrays
            .asList("addSubmitListener", "isEnabled", "setEnabled");

    @Test
    public void messageInput_replicateRelevantAPIs() {
        List<String> messageInputMethods = ReflectionUtils
                .getMethodNames(MessageInput.class);
        List<String> collaborationMessageInputMethods = ReflectionUtils
                .getMethodNames(CollaborationMessageInput.class);

        List<String> missingMethods = messageInputMethods.stream()
                .filter(m -> !blackListedMethods.contains(m)
                        && !collaborationMessageInputMethods.contains(m))
                .collect(Collectors.toList());

        if (!missingMethods.isEmpty()) {
            Assert.fail("Missing wrapper for methods: "
                    + missingMethods.toString());
        }
    }
}
