/*
 * Copyright 2000-2021 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.collaborationengine;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;

public class CollaborationListTest {

    private static final TypeReference<String> STRING_REF = new TypeReference<String>() {
    };

    private static class ListSubscriberSpy implements ListSubscriber {

        private final LinkedList<String> expectedItems = new LinkedList<>();

        @Override
        public void onListChange(ListChangeEvent event) {
            String item = event.getAddedItem(String.class).get();
            String expected = expectedItems.removeFirst();
            Assert.assertTrue(item.equals(expected));
        }

        private void addExpectedEvent(String item) {
            expectedItems.add(item);
        }

        private void assertNoExpectedEvents() {
            Assert.assertTrue(expectedItems.isEmpty());
        }
    }

    private MockConnectionContext context;
    private CollaborationEngine ce;
    private TopicConnection connection;
    private CollaborationList list;
    private ListSubscriberSpy spy;
    private TopicConnectionRegistration registration;

    @Before
    public void init() {
        context = MockConnectionContext.createEager();
        ce = TestUtil.createTestCollaborationEngine();
        registration = ce.openTopicConnection(context, "topic",
                SystemUserInfo.getInstance(), topicConnection -> {
                    connection = topicConnection;
                    list = connection.getNamedList("foo");
                    return null;
                });
        spy = new ListSubscriberSpy();
    }

    @Test
    public void listWithItem_getItemsByClass() {
        list.append("foo");
        Assert.assertEquals("foo", list.getItems(String.class).get(0));
    }

    @Test
    public void listWithItem_getItemsByTypeReference() {
        list.append("foo");
        Assert.assertEquals("foo", list.getItems(STRING_REF).get(0));
    }

    @Test
    public void listChangeEvent_getAddedItemByClass()
            throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        list.subscribe(event -> {
            String item = event.getAddedItem(String.class).get();
            future.complete(item);
        });
        list.append("foo");
        Assert.assertEquals("foo", future.get());
    }

    @Test
    public void listChangeEvent_getAddedItemByTypeReference()
            throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        list.subscribe(event -> {
            String item = event.getAddedItem(STRING_REF).get();
            future.complete(item);
        });
        list.append("foo");
        Assert.assertEquals("foo", future.get());
    }

    @Test
    public void listWithItems_subscribe_eventsForItems() {
        list.append("foo");
        list.append("bar");

        spy.addExpectedEvent("foo");
        spy.addExpectedEvent("bar");

        list.subscribe(spy);

        spy.assertNoExpectedEvents();
    }

    @Test
    public void listWithRemovedSubscriber_appendItem_subscriberNotNotified() {
        list.subscribe(spy).remove();

        list.append("foo"); // Spy would throw if notified of unexpected "foo"
    }

    @Test
    public void listWithSubscriber_addAnotherSubscriber_noEventForOldSubscriber() {
        list.append("foo");

        spy.addExpectedEvent("foo");
        list.subscribe(spy);
        spy.assertNoExpectedEvents();

        list.subscribe(event -> {
        }); // Spy would throw if notified again
    }

    @Test
    public void listWithSubscriber_actionIsDispatchedInContext() {
        list.subscribe(ignore -> {
        });
        context.resetActionDispatchCount();
        list.append("foo");
        // One for the event, one for addToInbox and one for completing the
        // future
        Assert.assertEquals(3, context.getDispathActionCount());
    }

    @Test
    public void listWithMultipleSubscriber_actionsAreDispatchedInOwnContext() {
        MockConnectionContext ctx1 = MockConnectionContext.createEager();
        MockConnectionContext ctx2 = MockConnectionContext.createEager();
        AtomicReference<CollaborationList> list1 = new AtomicReference<>();
        AtomicReference<CollaborationList> list2 = new AtomicReference<>();
        ce.openTopicConnection(ctx1, "topic", SystemUserInfo.getInstance(),
                topicConnection -> {
                    list1.set(topicConnection.getNamedList("foo"));
                    return null;
                });
        ce.openTopicConnection(ctx2, "topic", SystemUserInfo.getInstance(),
                topicConnection -> {
                    list2.set(topicConnection.getNamedList("foo"));
                    return null;
                });
        list1.get().subscribe(ignore -> {
        });
        list2.get().subscribe(ignore -> {
        });
        ctx1.resetActionDispatchCount();
        ctx2.resetActionDispatchCount();
        list1.get().append("foo");

        // One dispatch for the event, ond for addToInbox and one for completing
        // the future
        Assert.assertEquals(3, ctx1.getDispathActionCount());
        // One dispatch for the event and one for addToInbox
        Assert.assertEquals(2, ctx2.getDispathActionCount());
    }

    @Test
    public void multipleListsWithSubscribers_onlyOwnSubscriberIsNotified() {
        MockConnectionContext ctx1 = MockConnectionContext.createEager();
        MockConnectionContext ctx2 = MockConnectionContext.createEager();
        AtomicReference<CollaborationList> list1 = new AtomicReference<>();
        AtomicReference<CollaborationList> list2 = new AtomicReference<>();
        ce.openTopicConnection(ctx1, "topic", SystemUserInfo.getInstance(),
                topicConnection -> {
                    list1.set(topicConnection.getNamedList("foo"));
                    return null;
                });
        ce.openTopicConnection(ctx2, "topic", SystemUserInfo.getInstance(),
                topicConnection -> {
                    list2.set(topicConnection.getNamedList("bar"));
                    return null;
                });

        ListSubscriberSpy spy1 = new ListSubscriberSpy();
        ListSubscriberSpy spy2 = new ListSubscriberSpy();

        list1.get().subscribe(spy1);
        list2.get().subscribe(spy2);

        spy1.addExpectedEvent("foo");
        spy2.addExpectedEvent("bar");

        list1.get().append("foo");
        list2.get().append("bar");

        spy1.assertNoExpectedEvents();
        spy2.assertNoExpectedEvents();
    }

    @Test
    public void append_contextCanDispatch_resolved() {
        CompletableFuture<Void> append = list.append("foo");
        Assert.assertTrue(append.isDone());
    }

    @Test
    public void append_contextCannotDispatch_unresolved() {
        context.init(ignore -> {
        }, ignore -> {
        });
        CompletableFuture<Void> append = list.append("foo");
        Assert.assertFalse(append.isDone());
    }

    @Test(expected = IllegalStateException.class)
    public void subscribe_throwsIfInactiveConnection() {
        registration.remove();
        list.subscribe(event -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void getItems_throwsIfInactiveConnection() {
        registration.remove();
        list.getItems(String.class);
    }

    @Test(expected = IllegalStateException.class)
    public void append_throwsIfInactiveConnection() {
        registration.remove();
        list.append("foo");
    }

    @Test
    public void expirationTimeout_listClearedAfterTimeout() {
        Duration timeout = Duration.ofMinutes(15);
        list.setExpirationTimeout(timeout);
        list.append("foo");
        registration.remove();
        ce.setClock(Clock.offset(ce.getClock(), timeout.plusMinutes(1)));
        AtomicReference<CollaborationList> newList = new AtomicReference<>();
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    newList.set(connection.getNamedList("foo"));
                    return null;
                });
        int listSize = newList.get().getItems(String.class).size();
        Assert.assertEquals(0, listSize);
    }

    @Test
    public void expirationTimeout_listNotClearedBeforeTimeout() {
        Duration timeout = Duration.ofMinutes(15);
        list.setExpirationTimeout(timeout);
        list.append("foo");
        registration.remove();
        ce.setClock(Clock.offset(ce.getClock(), timeout.minusMinutes(1)));
        AtomicReference<CollaborationList> newList = new AtomicReference<>();
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    newList.set(connection.getNamedList("foo"));
                    return null;
                });
        String foo = newList.get().getItems(String.class).get(0);
        Assert.assertEquals("foo", foo);
    }

    @Test
    public void expirationTimeout_timeoutRemovedWhenSetToNull() {
        Duration timeout = Duration.ofMinutes(15);
        list.setExpirationTimeout(timeout);
        list.append("foo");
        registration.remove();
        ce.setClock(Clock.offset(ce.getClock(), timeout.plusMinutes(1)));
        list.setExpirationTimeout(null);
        AtomicReference<CollaborationList> newList = new AtomicReference<>();
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    newList.set(connection.getNamedList("foo"));
                    return null;
                });
        String foo = newList.get().getItems(String.class).get(0);
        Assert.assertEquals("foo", foo);
    }

    @Test
    public void expirationTimeout_connectionOpened_listClearCancelled() {
        Duration timeout = Duration.ofMinutes(15);
        list.setExpirationTimeout(timeout);
        list.append("foo");
        registration.remove();
        AtomicReference<CollaborationList> newList = new AtomicReference<>();
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    newList.set(connection.getNamedList("foo"));
                    return null;
                });
        ce.setClock(Clock.offset(ce.getClock(), timeout.plusMinutes(1)));
        String foo = newList.get().getItems(String.class).get(0);
        Assert.assertEquals("foo", foo);
    }

    @Test
    public void expirationTimeout_multipleConnectionsOpened_closeOne_listClearNotScheduled() {
        Duration timeout = Duration.ofMinutes(15);
        list.setExpirationTimeout(timeout);
        list.append("foo");
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                connection -> null).remove();
        ce.setClock(Clock.offset(ce.getClock(), timeout.plusMinutes(1)));
        String foo = list.getItems(String.class).get(0);
        Assert.assertEquals("foo", foo);
    }
}
