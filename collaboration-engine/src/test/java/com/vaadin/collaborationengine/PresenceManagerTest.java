package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

public class PresenceManagerTest {

    private static final String TOPIC_ID = "presence";

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
    public void autoPresenceTrue_localUserPresent() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);

        manager.setAutoPresence(true);

        Assert.assertTrue(manager.getUsers().anyMatch(user::equals));
    }

    @Test
    public void autoPresenceTrue_setTopic_localUserPresent() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user, "topic");

        manager.setAutoPresence(true);

        Assert.assertTrue(manager.getUsers().anyMatch(user::equals));
    }

    @Test
    public void autoPresenceTrue_setHandler_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setAutoPresence(true);
        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_autoPresenceTrue_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        manager.setAutoPresence(true);

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_autoPresenceTrue_handlerInvokedOnOtherManager() {
        UserInfo user = new UserInfo("foo");
        PresenceManager foo = createActiveManager(user);
        PresenceManager bar = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        bar.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        foo.setAutoPresence(true);

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_autoPresenceFalse_handlerRegistrationRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        manager.setAutoPresence(false);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void autoPresenceSetTwice_handlerInvokedOnce() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> {
            };
        });
        manager.setAutoPresence(true);
        manager.setAutoPresence(true);

        Assert.assertEquals(1, users.size());
    }

    @Test
    public void autoPresenceSetOnMultipleManagers_oneRemoved_correctHandlerUnregistered() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceManager fooManager = createActiveManager(foo);
        PresenceManager barManager = createActiveManager(bar);
        List<UserInfo> users = new ArrayList<>();

        fooManager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        fooManager.setAutoPresence(true);
        barManager.setAutoPresence(true);
        barManager.setAutoPresence(false);

        Assert.assertTrue(users.contains(foo));
        Assert.assertFalse(users.contains(bar));
    }

    @Test
    public void setHandler_handlerReceivesCurrentUser() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setAutoPresence(true);
        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void replaceHandler_oldRegistrationsRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        List<UserInfo> users = new ArrayList<>();

        manager.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        manager.setAutoPresence(true);
        manager.setNewUserHandler(null);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void getUsers_doesNotContainDuplicates() {
        UserInfo foo = new UserInfo("foo");
        PresenceManager first = createActiveManager(foo);
        PresenceManager second = createActiveManager(foo);

        first.setAutoPresence(true);
        second.setAutoPresence(true);

        Assert.assertEquals(1, first.getUsers().count());
    }

    @Test
    public void getUsers_orderOfPresenceIsPreserved() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceManager fooManager = createActiveManager(foo);
        PresenceManager barManager = createActiveManager(bar);

        fooManager.setAutoPresence(true);
        barManager.setAutoPresence(true);

        List<UserInfo> users = fooManager.getUsers()
                .collect(Collectors.toList());

        Assert.assertEquals(foo, users.get(0));
        Assert.assertEquals(bar, users.get(1));

        fooManager.setAutoPresence(false);
        fooManager.setAutoPresence(true);

        users = barManager.getUsers().collect(Collectors.toList());

        Assert.assertEquals(bar, users.get(0));
        Assert.assertEquals(foo, users.get(1));
    }

    @Test
    public void collaborationMapValueEncodedAsJsonNode() {
        UserInfo user = new UserInfo("foo");
        PresenceManager manager = createActiveManager(user);
        manager.setAutoPresence(true);
        AtomicBoolean done = new AtomicBoolean(false);
        TestUtils.openEagerConnection(ce, TOPIC_ID, topicConnection -> {
            List<UserInfo> mapValue = topicConnection
                    .getNamedMap(PresenceManager.MAP_NAME)
                    .get(PresenceManager.MAP_KEY, JsonUtil.LIST_USER_TYPE_REF);
            List<String> ids = mapValue.stream().map(UserInfo::getId)
                    .collect(Collectors.toList());
            Assert.assertTrue(ids.contains("foo"));
            done.set(true);
        });
        Assert.assertTrue("Topic connection callback has not run", done.get());
    }

    private PresenceManager createActiveManager(UserInfo user) {
        return createActiveManager(user, TOPIC_ID);
    }

    private PresenceManager createActiveManager(UserInfo user, String topicId) {
        return new PresenceManager(new EagerConnectionContext(), user, topicId,
                ce);
    }
}
