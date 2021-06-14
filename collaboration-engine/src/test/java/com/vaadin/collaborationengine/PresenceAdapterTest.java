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

public class PresenceAdapterTest {

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
        PresenceAdapter adapter = createActiveAdapter(user);

        adapter.setAutoPresence(true);

        Assert.assertTrue(adapter.getUsers().anyMatch(user::equals));
    }

    @Test
    public void autoPresenceTrue_setTopic_localUserPresent() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user, null);

        adapter.setAutoPresence(true);
        adapter.setTopic("topic");

        Assert.assertTrue(adapter.getUsers().anyMatch(user::equals));
    }

    @Test
    public void autoPresenceTrue_nullTopic_localUserRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);

        adapter.setAutoPresence(true);
        adapter.setTopic(null);

        Assert.assertEquals(0, adapter.getUsers().count());
    }

    @Test
    public void autoPresenceTrue_changeTopic_getUsersFromNewTopic() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceAdapter fooAdapter = createActiveAdapter(foo, "fooTopic");
        PresenceAdapter barAdapter = createActiveAdapter(bar, "barTopic");

        fooAdapter.setAutoPresence(true);
        barAdapter.setAutoPresence(true);
        barAdapter.setTopic("fooTopic");

        Assert.assertTrue(barAdapter.getUsers().anyMatch(foo::equals));
    }

    @Test
    public void autoPresenceTrue_changeTopic_localUserRemovedFromOldTopic() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceAdapter fooAdapter = createActiveAdapter(foo, "fooTopic");
        PresenceAdapter barAdapter = createActiveAdapter(bar, "fooTopic");

        fooAdapter.setAutoPresence(true);
        barAdapter.setAutoPresence(true);
        barAdapter.setTopic("barTopic");

        Assert.assertTrue(fooAdapter.getUsers().noneMatch(bar::equals));
    }

    @Test
    public void autoPresenceTrue_setHandler_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setAutoPresence(true);
        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_autoPresenceTrue_handlerInvoked() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        adapter.setAutoPresence(true);

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void setHandler_autoPresenceTrue_handlerInvokedOnOtherAdapter() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter foo = createActiveAdapter(user);
        PresenceAdapter bar = createActiveAdapter(user);
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
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        adapter.setAutoPresence(false);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void autoPresenceSetTwice_handlerInvokedOnce() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> {
            };
        });
        adapter.setAutoPresence(true);
        adapter.setAutoPresence(true);

        Assert.assertEquals(1, users.size());
    }

    @Test
    public void autoPresenceSetOnMultipleAdapters_oneRemoved_correctHandlerUnregistered() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceAdapter fooAdapter = createActiveAdapter(foo);
        PresenceAdapter barAdapter = createActiveAdapter(bar);
        List<UserInfo> users = new ArrayList<>();

        fooAdapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        fooAdapter.setAutoPresence(true);
        barAdapter.setAutoPresence(true);
        barAdapter.setAutoPresence(false);

        Assert.assertTrue(users.contains(foo));
        Assert.assertFalse(users.contains(bar));
    }

    @Test
    public void setHandler_handlerReceivesCurrentUser() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setAutoPresence(true);
        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });

        Assert.assertTrue(users.contains(user));
    }

    @Test
    public void replaceHandler_oldRegistrationsRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        adapter.setAutoPresence(true);
        adapter.setNewUserHandler(null);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void nullTopic_handlerRegistrationsRemoved() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        List<UserInfo> users = new ArrayList<>();

        adapter.setNewUserHandler(newUser -> {
            users.add(newUser);
            return () -> users.remove(newUser);
        });
        adapter.setAutoPresence(true);
        adapter.setTopic(null);

        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void getUsers_doesNotContainDuplicates() {
        UserInfo foo = new UserInfo("foo");
        PresenceAdapter first = createActiveAdapter(foo);
        PresenceAdapter second = createActiveAdapter(foo);

        first.setAutoPresence(true);
        second.setAutoPresence(true);

        Assert.assertEquals(1, first.getUsers().count());
    }

    @Test
    public void getUsers_orderOfPresenceIsPreserved() {
        UserInfo foo = new UserInfo("foo");
        UserInfo bar = new UserInfo("bar");
        PresenceAdapter fooAdapter = createActiveAdapter(foo);
        PresenceAdapter barAdapter = createActiveAdapter(bar);

        fooAdapter.setAutoPresence(true);
        barAdapter.setAutoPresence(true);

        List<UserInfo> users = fooAdapter.getUsers()
                .collect(Collectors.toList());

        Assert.assertEquals(foo, users.get(0));
        Assert.assertEquals(bar, users.get(1));

        fooAdapter.setAutoPresence(false);
        fooAdapter.setAutoPresence(true);

        users = barAdapter.getUsers().collect(Collectors.toList());

        Assert.assertEquals(bar, users.get(0));
        Assert.assertEquals(foo, users.get(1));
    }

    @Test
    public void collaborationMapValueEncodedAsJsonNode() {
        UserInfo user = new UserInfo("foo");
        PresenceAdapter adapter = createActiveAdapter(user);
        adapter.setAutoPresence(true);
        AtomicBoolean done = new AtomicBoolean(false);
        TestUtils.openEagerConnection(ce, TOPIC_ID, topicConnection -> {
            List<UserInfo> mapValue = topicConnection
                    .getNamedMap(PresenceAdapter.MAP_NAME)
                    .get(PresenceAdapter.MAP_KEY, JsonUtil.LIST_USER_TYPE_REF);
            List<String> ids = mapValue.stream().map(UserInfo::getId)
                    .collect(Collectors.toList());
            Assert.assertTrue(ids.contains("foo"));
            done.set(true);
        });
        Assert.assertTrue("Topic connection callback has not run", done.get());
    }

    private PresenceAdapter createActiveAdapter(UserInfo user) {
        return createActiveAdapter(user, TOPIC_ID);
    }

    private PresenceAdapter createActiveAdapter(UserInfo user, String topicId) {
        return new PresenceAdapter(EagerConnectionContext::new, user, topicId,
                ce);
    }
}
