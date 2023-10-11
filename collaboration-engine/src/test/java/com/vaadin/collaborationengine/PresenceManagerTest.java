package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

public class PresenceManagerTest {

    private static final String TOPIC_ID = "presence";

    private VaadinService service;

    private SerializableSupplier<CollaborationEngine> ceSupplier;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        ceSupplier = () -> ce;
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void markAsPresentTrue_connectionDeactivated_userRemoved() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceManager manager = createActiveManager(foo);
        PresenceManager spyManager = createActiveManager(bar);
        AtomicBoolean userRemoved = new AtomicBoolean();
        spyManager.setPresenceHandler(context -> {
            UserInfo user = context.getUser();
            return () -> {
                if (user.getId().equals("foo")) {
                    userRemoved.set(true);
                }
            };
        });
        manager.markAsPresent(true);
        manager.close();
        Assert.assertTrue(userRemoved.get());
    }

    @Test
    public void markAsPresentTrue_setHandler_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.markAsPresent(true);
        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_markAsPresentTrue_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });
        manager.markAsPresent(true);

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_markAsPresentTrue_handlerInvokedOnOtherManager() {
        UserInfo user = new UserInfo("foo");
        PresenceManager foo = createActiveManager(user);
        PresenceManager bar = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        bar.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });
        foo.markAsPresent(true);

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_markAsPresentFalse_handlerRegistrationRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });
        manager.markAsPresent(false);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void markAsPresentTwice_handlerInvokedOnce() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> {
            };
        });
        manager.markAsPresent(true);
        manager.markAsPresent(true);

        Assert.assertEquals(1, users.size());
    }

    @Test
    public void markAsPresentOnMultipleManagers_oneRemoved_correctHandlerUnregistered() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceManager fooManager = createActiveManager(foo);
        PresenceManager barManager = createActiveManager(bar);
        List<UserInfo> users = new ArrayList<>();

        fooManager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });
        fooManager.markAsPresent(true);
        barManager.markAsPresent(true);
        barManager.markAsPresent(false);

        Assert.assertTrue(users.contains(foo));
        Assert.assertFalse(users.contains(bar));
    }

    @Test
    public void setHandler_handlerReceivesCurrentUser() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.markAsPresent(true);
        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void replaceHandler_oldRegistrationsRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setPresenceHandler(context -> {
            users.add(context.getUser());
            return () -> users.remove(context.getUser());
        });
        manager.markAsPresent(true);
        manager.setPresenceHandler(null);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void collaborationMapValueEncodedAsJsonNode() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        manager.markAsPresent(true);
        AtomicBoolean done = new AtomicBoolean(false);
        TestUtils.openEagerConnection(ceSupplier.get(), TOPIC_ID,
                topicConnection -> {
                    List<String> ids = topicConnection
                            .getNamedList(PresenceManager.LIST_NAME)
                            .getItems(UserInfo.class).stream()
                            .map(UserInfo::getId).collect(Collectors.toList());
                    Assert.assertTrue(ids.contains("foo"));
                    done.set(true);
                });
        Assert.assertTrue("Topic connection callback has not run", done.get());
    }

    @Test
    public void serializePresenceManager() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);

        PresenceManager deserializedManager = TestUtils.serialize(manager);
    }

    private PresenceManager createActiveManager(UserInfo user) {
        return createActiveManager(user, TOPIC_ID);
    }

    private PresenceManager createActiveManager(UserInfo user, String topicId) {
        return new PresenceManager(MockConnectionContext.createEager(), user,
                topicId, ceSupplier);
    }
}
