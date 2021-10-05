package com.vaadin.collaborationengine;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

public class MessageManagerTest {

    private static final String TOPIC_ID = "messages";

    private VaadinService service;

    private CollaborationEngine ce;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        ce = TestUtil.createTestCollaborationEngine(service);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void submitMessage_getMessagesContainsMessageWithCorrectValues() {
        Instant fixedTimestamp = Instant.now();
        ce.setClock(Clock.fixed(fixedTimestamp, ZoneOffset.UTC));

        MessageManager manager = createActiveManager();
        manager.submit("text");

        CollaborationMessage message = manager.getMessages().findFirst().get();

        Assert.assertEquals(manager.getLocalUser(), message.getUser());
        Assert.assertEquals(fixedTimestamp, message.getTime());
        Assert.assertEquals("text", message.getText());
    }

    @Test
    public void setHandler_submitMessage_handlerInvokedWithMessage() {
        List<CollaborationMessage> list = new ArrayList<>();
        MessageManager manager = createActiveManager();

        manager.setMessageHandler(context -> {
            CollaborationMessage message = context.getMessage();
            list.add(message);
        });
        manager.submit("text");

        CollaborationMessage message = list.get(0);
        Assert.assertEquals("text", message.getText());
    }

    @Test
    public void submitMessage_setHandler_handlerInvokedWithMessage() {
        List<CollaborationMessage> list = new ArrayList<>();
        MessageManager manager = createActiveManager();

        manager.submit("text");
        manager.setMessageHandler(context -> {
            CollaborationMessage message = context.getMessage();
            list.add(message);
        });

        CollaborationMessage message = list.get(0);
        Assert.assertEquals("text", message.getText());
    }

    @Test
    public void setHandler_submitMessage_connectionReactivates_handlerNotInvokedForOldMessage() {
        MockConnectionContext connectionContext = MockConnectionContext
                .createEager();
        MessageManager manager = new MessageManager(connectionContext,
                new UserInfo("foo"), TOPIC_ID, null, ce);

        List<CollaborationMessage> list = new ArrayList<>();
        manager.setMessageHandler(context -> {
            CollaborationMessage message = context.getMessage();
            list.add(message);
        });
        manager.submit("old");

        connectionContext.deactivate();
        connectionContext.activate();

        manager.submit("new");

        Assert.assertEquals("old", list.get(0).getText());
        Assert.assertEquals("new", list.get(1).getText());
    }

    @Test
    public void deactivateConnection_submitMessage_activateConnection_messageFutureCompletes() {
        MockConnectionContext connectionContext = MockConnectionContext
                .createEager();
        MessageManager manager = new MessageManager(connectionContext,
                new UserInfo("foo"), TOPIC_ID, null, ce);

        connectionContext.deactivate();
        CompletableFuture<Void> future = manager.submit("text");

        Assert.assertFalse(future.isDone());

        connectionContext.activate();

        Assert.assertTrue(future.isDone());
    }

    @Test
    public void withPersister_submitMessage_messageFutureCompletes() {
        MockConnectionContext connectionContext = MockConnectionContext
                .createEager();
        Map<String, List<CollaborationMessage>> backend = new HashMap<>();
        CollaborationMessagePersister persister = CollaborationMessagePersister
                .fromCallbacks(
                        query -> backend
                                .computeIfAbsent(query
                                        .getTopicId(), t -> new ArrayList<>())
                                .stream()
                                .filter(message -> message.getTime()
                                        .compareTo(query.getSince()) >= 0),
                        event -> backend
                                .computeIfAbsent(event.getTopicId(),
                                        t -> new ArrayList<>())
                                .add(event.getMessage()));
        MessageManager manager = new MessageManager(connectionContext,
                new UserInfo("foo"), TOPIC_ID, persister, ce);

        connectionContext.deactivate();

        CompletableFuture<Void> future = manager.submit("text");

        Assert.assertFalse(future.isDone());

        connectionContext.activate();

        Assert.assertTrue(future.isDone());
    }

    private MessageManager createActiveManager() {
        return createActiveManager(new UserInfo("foo"));
    }

    private MessageManager createActiveManager(UserInfo user) {
        return createActiveAdapter(user, TOPIC_ID);
    }

    private MessageManager createActiveAdapter(UserInfo user, String topicId) {
        return new MessageManager(MockConnectionContext.createEager(), user,
                topicId, null, ce);
    }
}
