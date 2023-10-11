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
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinService;

public class MessageManagerTest {

    private static final String TOPIC_ID = "messages";

    private VaadinService service;

    private SerializableSupplier<CollaborationEngine> ceSupplier;

    private Map<String, List<CollaborationMessage>> backend;

    private CollaborationMessagePersister persister;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        ceSupplier = () -> ce;
        backend = new HashMap<>();
        persister = CollaborationMessagePersister.fromCallbacks(
                query -> backend
                        .computeIfAbsent(
                                query.getTopicId(), t -> new ArrayList<>())
                        .stream()
                        .filter(message -> message.getTime()
                                .compareTo(query.getSince()) >= 0),
                event -> backend
                        .computeIfAbsent(event.getTopicId(),
                                t -> new ArrayList<>())
                        .add(event.getMessage()));
    }

    @After
    public void cleanUp() {
        backend = null;
        persister = null;
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    private CollaborationEngine getCollaborationEngine() {
        return ceSupplier.get();
    }

    @Test
    public void submitMessage_getMessagesContainsMessageWithCorrectValues() {
        Instant fixedTimestamp = Instant.now();
        getCollaborationEngine()
                .setClock(Clock.fixed(fixedTimestamp, ZoneOffset.UTC));

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
                new UserInfo("foo"), TOPIC_ID, null, ceSupplier);

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
                new UserInfo("foo"), TOPIC_ID, null, ceSupplier);

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
        MessageManager manager = new MessageManager(connectionContext,
                new UserInfo("foo"), TOPIC_ID, persister, ceSupplier);
        connectionContext.deactivate();

        CompletableFuture<Void> future = manager.submit("text");
        Assert.assertFalse(future.isDone());

        connectionContext.activate();
        Assert.assertTrue(future.isDone());
    }

    @Test
    public void withPersister_insertConditionFails_futureNotCompleted() {
        UserInfo userInfo = new UserInfo("foo");

        MessageManager manager = new MessageManager(
                MockConnectionContext.createEager(), userInfo, TOPIC_ID,
                persister, ceSupplier);

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), TOPIC_ID, userInfo,
                conn -> {
                    CollaborationList list = conn
                            .getNamedList(MessageManager.LIST_NAME);
                    list.subscribe(change -> {
                        CollaborationMessage message = change
                                .getValue(CollaborationMessage.class);
                        if (message.getText().equals("foo")) {
                            list.insertLast(new CollaborationMessage(userInfo,
                                    "boom", getCollaborationEngine().getClock()
                                            .instant()));
                        }
                    });
                    return null;
                });

        backend.computeIfAbsent(TOPIC_ID, t -> new ArrayList<>())
                .add(new CollaborationMessage(userInfo, "foo",
                        getCollaborationEngine().getClock().instant()));

        CompletableFuture<Void> future = manager.submit("text");
        Assert.assertFalse(future.isDone());
    }

    @Test
    public void serializeMessageManager() {
        MessageManager manager = createActiveManager();

        MessageManager deserializedManager = TestUtils.serialize(manager);
    }

    private MessageManager createActiveManager() {
        return createActiveManager(new UserInfo("foo"));
    }

    private MessageManager createActiveManager(UserInfo user) {
        return createActiveAdapter(user, TOPIC_ID);
    }

    private MessageManager createActiveAdapter(UserInfo user, String topicId) {
        return new MessageManager(MockConnectionContext.createEager(), user,
                topicId, null, ceSupplier);
    }
}
