package com.vaadin.collaborationengine;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.examplecomponent.ExampleComponentMessage;
import com.vaadin.collaborationengine.examplecomponent.Message;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;

public class CollaborationExampleComponentTest {

    private static final String TOPIC_ID = "topic";

    public static class ExampleComponentTestClient {
        final UI ui;
        final UserInfo user;
        CollaborationExampleComponent exampleComponent;

        ExampleComponentTestClient(int index, CollaborationEngine ce) {
            this(index, TOPIC_ID, ce);
        }

        ExampleComponentTestClient(int index, String topicId,
                CollaborationEngine ce) {
            this.ui = new MockUI();
            this.user = new UserInfo("id" + index, "name" + index,
                    "image" + index);
            user.setAbbreviation("abbreviation" + index);
            user.setColorIndex(index);
            exampleComponent = new CollaborationExampleComponent(user, topicId,
                    ce);

        }

        private List<Message> getMessages() {
            Stream<Component> components = exampleComponent.getContent()
                    .getMessages();
            return components
                    .map(component -> ((ExampleComponentMessage) component)
                            .getMessage())
                    .collect(Collectors.toList());
        }

        protected TextField getCommentField() {
            return exampleComponent.getContent().getCommentField();
        }

        private Button getSubmitMessageButton() {
            return exampleComponent.getContent().getSubmitMessageButton();
        }

        void attach() {
            ui.add(exampleComponent);
        }

        void setTopic(String topicId) {
            exampleComponent.setTopic(topicId);
        }

        public void sendMessage(String content) {
            getCommentField().setValue(content);
            getSubmitMessageButton().click();
        }
    }

    private ExampleComponentTestClient client1;
    private ExampleComponentTestClient client2;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        client1 = new ExampleComponentTestClient(1, ce);
        client2 = new ExampleComponentTestClient(2, ce);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void beforeTopic_noMessages_controlsDisabled() {
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        Assert.assertFalse(
                "Comment field should be disabled before it is attached to an topic",
                client1.getCommentField().isEnabled());
        Assert.assertFalse(
                "Submit button should be disabled before it is attached to an topic",
                client1.getSubmitMessageButton().isEnabled());
    }

    @Test
    public void setTopic_noMessages_controlsEnabled() {
        client1.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        Assert.assertTrue("Comment field should be enabled",
                client1.getCommentField().isEnabled());
        Assert.assertTrue("submit button should be enabled",
                client1.getSubmitMessageButton().isEnabled());
    }

    @Test
    public void sendMessage_messageAppearsInMessageList() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        client1.sendMessage("new message");
        List<Message> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        Message message = messages.get(0);
        Assert.assertEquals("new message", message.getContent());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void sendMessage_messagePropagatesToOtherUser() {
        client1.attach();
        client2.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        Assert.assertEquals(Collections.emptyList(), client2.getMessages());
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        Assert.assertEquals(1, client2.getMessages().size());
        Message message = client2.getMessages().get(0);
        Assert.assertEquals("new message", message.getContent());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void joinTopic_existingMessagesAreDisplayed() {
        client1.attach();
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        Assert.assertEquals(0, client2.getMessages().size());
        client2.attach();
        Assert.assertEquals(1, client2.getMessages().size());
        Message message = client2.getMessages().get(0);
        Assert.assertEquals("new message", message.getContent());
        Assert.assertEquals("name1", message.getUserName());
    }

    @Test
    public void topicSetToNull_contentIsCleared() {
        client1.attach();
        client1.sendMessage("new message");
        Assert.assertEquals(1, client1.getMessages().size());
        client1.setTopic(null);
        Assert.assertEquals(0, client1.getMessages().size());
        Assert.assertFalse(
                "Comment field should be disabled because topic is not set",
                client1.getCommentField().isEnabled());
        Assert.assertFalse(
                "Submit button should be disabled because topic is not set",
                client1.getSubmitMessageButton().isEnabled());
    }
}
