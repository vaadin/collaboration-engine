package com.vaadin.collaborationengine;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestStreamResource;
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
            messageList = new CollaborationMessageList(user, null, null, ce);
        }

        private List<MessageListItem> getMessages() {
            return messageList.getContent().getItems();
        }

        void attach() {
            ui.add(messageList);
        }

        void setTopic(String topicId) {
            setTopic(topicId, null);
        }

        void setTopic(String topicId, CollaborationMessagePersister persister) {
            this.topicId = topicId;
            messageList.setTopic(this.topicId, persister);
        }

        public void sendMessage(String content) {
            messageList.appendMessage(content);
        }
    }

    private MessageListTestClient client1;
    private MessageListTestClient client2;

    private Map<String, List<CollaborationMessage>> backend;
    private CollaborationMessagePersister persister;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        client1 = new MessageListTestClient(1, ce);
        client2 = new MessageListTestClient(2, ce);

        backend = new HashMap<>();
        persister = CollaborationMessagePersister.fromCallbacks(
                query -> backend
                        .computeIfAbsent(
                                query.getTopicId(), t -> new ArrayList<>())
                        .stream()
                        .filter(message -> message.getTime()
                                .isAfter(query.getSince())),
                event -> backend
                        .computeIfAbsent(event.getTopicId(),
                                t -> new ArrayList<>())
                        .add(event.getMessage()));
    }

    @After
    public void cleanUp() {
        backend.clear();
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void sendMessage_messageAppearsInMessageList() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertEquals(Collections.emptyList(), client1.getMessages());
        client1.ce.setClock(
                Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC));
        client1.sendMessage("new message");
        List<MessageListItem> messages = client1.getMessages();
        Assert.assertEquals(1, messages.size());
        MessageListItem message = messages.get(0);
        Assert.assertEquals("new message", message.getText());
        Assert.assertEquals(Instant.ofEpochSecond(10), message.getTime());
        Assert.assertEquals("name1", message.getUserName());
        Assert.assertEquals("abbreviation1", message.getUserAbbreviation());
        Assert.assertEquals("image1", message.getUserImage());
        Assert.assertEquals(Integer.valueOf(1), message.getUserColorIndex());
    }

    @Test
    public void noExplicitColorIndex_colorIndexProvidedByCollaborationEngine() {
        client1.user.setColorIndex(-1);
        client1.attach();
        client1.setTopic(TOPIC_ID);
        client1.sendMessage("new message");
        MessageListItem message = client1.getMessages().get(0);
        Assert.assertEquals(Integer.valueOf(0), message.getUserColorIndex());
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

    @Test(expected = NullPointerException.class)
    public void setSubmitterBeforeTopic_nullRegistration_throws() {
        client1.messageList.setSubmitter(activationContext -> null);
        client1.attach();
        client1.setTopic(TOPIC_ID);
    }

    @Test(expected = NullPointerException.class)
    public void setSubmitterAfterTopic_nullRegistration_throws() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        client1.messageList.setSubmitter(activationContext -> null);
    }

    @Test
    public void setSubmitterBeforeTopic_submitterActivated() {
        AtomicBoolean submitterActivated = new AtomicBoolean();
        client1.messageList.setSubmitter(activationContext -> {
            submitterActivated.set(true);
            return () -> {
            };
        });
        client1.attach();
        client1.setTopic(TOPIC_ID);
        Assert.assertTrue(submitterActivated.get());
    }

    @Test
    public void setSubmitterAfterTopic_submitterActivated() {
        AtomicBoolean submitterActivated = new AtomicBoolean();
        client1.attach();
        client1.setTopic(TOPIC_ID);
        client1.messageList.setSubmitter(activationContext -> {
            submitterActivated.set(true);
            return () -> {
            };
        });
        Assert.assertTrue(submitterActivated.get());
    }

    @Test
    public void setSubmitterBeforeTopic_clearTopic_registrationRemoved() {
        AtomicBoolean registration = new AtomicBoolean();
        client1.messageList.setSubmitter(
                activationContext -> () -> registration.set(true));
        client1.attach();
        client1.setTopic(TOPIC_ID);
        client1.setTopic(null);
        Assert.assertTrue(registration.get());
    }

    @Test
    public void setSubmitterAfterTopic_clearTopic_registrationRemoved() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        AtomicBoolean registration = new AtomicBoolean();
        client1.messageList.setSubmitter(
                activationContext -> () -> registration.set(true));
        client1.setTopic(null);
        Assert.assertTrue(registration.get());
    }

    @Test
    public void replaceSubmitter_existingRegistrationRemoved() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        AtomicBoolean registration = new AtomicBoolean();
        client1.messageList.setSubmitter(
                activationContext -> () -> registration.set(true));
        client1.messageList.setSubmitter(activationContext -> () -> {
        });
        Assert.assertTrue(registration.get());
    }

    @Test
    public void nullSubmitter_existingRegistrationRemoved() {
        client1.attach();
        client1.setTopic(TOPIC_ID);
        AtomicBoolean registration = new AtomicBoolean();
        client1.messageList.setSubmitter(
                activationContext -> () -> registration.set(true));
        client1.messageList.setSubmitter(null);
        Assert.assertTrue(registration.get());
    }

    @Test
    public void setPersister_attach_messagesAreReadFromBackend() {
        addMessageToBackend(TOPIC_ID, client1.user, "foo", Instant.now());
        client1.attach();
        client1.setTopic(TOPIC_ID, persister);
        Assert.assertEquals(1, client1.getMessages().size());
    }

    @Test
    public void setPersister_appendMessage_messagesAreWrittenToBackend() {
        Instant time = Instant.now();
        client1.ce.setClock(Clock.fixed(time, ZoneOffset.UTC));
        client1.attach();
        client1.setTopic(TOPIC_ID, persister);
        client1.messageList.appendMessage("foo");
        CollaborationMessage message = backend.get(TOPIC_ID).get(0);

        // Assert we pass all the correct info to the persister
        Assert.assertEquals("foo", message.getText());
        Assert.assertEquals(client1.user, message.getUser());
        Assert.assertEquals(time, message.getTime());
    }

    @Test
    public void setPersister_fetchPersistedList_onlyNewMessagesAreAppended() {
        addMessageToBackend(TOPIC_ID, client1.user, "foo", Instant.now());

        client1.attach();
        client1.setTopic(TOPIC_ID, persister);

        addMessageToBackend(TOPIC_ID, client1.user, "bar", Instant.now());

        client1.messageList.fetchPersistedList();
        Assert.assertEquals(2, client1.getMessages().size());
    }

    private void addMessageToBackend(String topicId, UserInfo user, String text,
            Instant time) {
        backend.computeIfAbsent(topicId, t -> new ArrayList<>())
                .add(new CollaborationMessage(user, text, time));
    }

    @Test
    public void imageProvider_beforeAttach_streamResourceIsUsed() {
        UI.setCurrent(client1.ui);
        client1.messageList.setImageProvider(
                user -> new TestStreamResource(user.getName()));
        client1.setTopic(TOPIC_ID);
        client2.setTopic(TOPIC_ID);
        client1.attach();
        client2.attach();

        client2.sendMessage("foo");

        List<MessageListItem> items = client1.getMessages();

        Assert.assertEquals(1, items.size());
        MessageListItem item = items.get(0);

        Assert.assertThat(item.getUserImage(),
                CoreMatchers.startsWith("VAADIN/dynamic"));
        Assert.assertEquals("name2", item.getUserImageResource().getName());
    }

    @Test
    public void imageProvider_afterAttach_streamResourceIsUsed() {
        UI.setCurrent(client1.ui);
        client1.setTopic(TOPIC_ID);
        client2.setTopic(TOPIC_ID);
        client1.attach();
        client2.attach();

        client1.messageList.setImageProvider(
                user -> new TestStreamResource(user.getName()));

        client2.sendMessage("foo");

        List<MessageListItem> items = client1.getMessages();

        Assert.assertEquals(1, items.size());
        MessageListItem item = items.get(0);

        Assert.assertThat(item.getUserImage(),
                CoreMatchers.startsWith("VAADIN/dynamic"));
        Assert.assertEquals("name2", item.getUserImageResource().getName());
    }

    @Test
    public void imageProvider_nullStream_noImage() {
        UI.setCurrent(client1.ui);
        client1.setTopic(TOPIC_ID);
        client2.setTopic(TOPIC_ID);
        client1.attach();
        client2.attach();

        client1.messageList.setImageProvider(user -> null);

        client2.sendMessage("foo");

        List<MessageListItem> items = client1.getMessages();
        Assert.assertEquals(1, items.size());

        MessageListItem item = items.get(0);

        Assert.assertNull(item.getUserImage());
        Assert.assertNull(item.getUserImageResource());
    }

    @Test
    public void imageProvider_clearProvider_imageIsSetFromUserInfo() {
        UI.setCurrent(client1.ui);
        client1.messageList.setImageProvider(
                user -> new TestStreamResource(user.getName()));
        client1.setTopic(TOPIC_ID);
        client2.setTopic(TOPIC_ID);
        client1.attach();
        client2.attach();

        client2.sendMessage("foo");

        client1.messageList.setImageProvider(null);

        List<MessageListItem> items = client1.getMessages();
        Assert.assertEquals(1, items.size());
        MessageListItem item = items.get(0);

        Assert.assertNull(item.getUserImageResource());
        Assert.assertEquals("image2", item.getUserImage());
    }
}
