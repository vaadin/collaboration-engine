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

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;
import com.vaadin.flow.server.Command;

public class CollaborationListTest {

    private static final TypeReference<String> STRING_REF = new TypeReference<String>() {
    };

    private static class ConnectionContextSpy extends EagerConnectionContext {

        private boolean shouldCountDispatchedAction = false;

        private int actionsDispatched = 0;

        private boolean allowDispatch = true;

        private void setAllowDispatch(boolean allowDispatch) {
            this.allowDispatch = allowDispatch;
        }

        private void setShouldCountDispatchedAction(
                boolean shouldCountDispatchedAction) {
            this.shouldCountDispatchedAction = shouldCountDispatchedAction;
        }

        @Override
        public void dispatchAction(Command action) {
            if (allowDispatch) {
                super.dispatchAction(action);
                if (shouldCountDispatchedAction) {
                    actionsDispatched++;
                    shouldCountDispatchedAction = false;
                }
            }
        }

    }

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

    private ConnectionContextSpy context;
    private CollaborationEngine ce;
    private TopicConnection connection;
    private CollaborationList list;
    private ListSubscriberSpy spy;
    private TopicConnectionRegistration registration;

    @Before
    public void init() {
        context = new ConnectionContextSpy();
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
        list.subscribe(event -> context.setShouldCountDispatchedAction(true));
        list.append("foo");
        Assert.assertEquals(1, context.actionsDispatched);
    }

    @Test
    public void listWithMultipleSubscriber_actionsAreDispatchedInOwnContext() {
        ConnectionContextSpy ctx1 = new ConnectionContextSpy();
        ConnectionContextSpy ctx2 = new ConnectionContextSpy();
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
        list1.get()
                .subscribe(event -> ctx1.setShouldCountDispatchedAction(true));
        list2.get()
                .subscribe(event -> ctx2.setShouldCountDispatchedAction(true));
        list1.get().append("foo");

        Assert.assertEquals(1, ctx1.actionsDispatched);
        Assert.assertEquals(1, ctx2.actionsDispatched);
    }

    @Test
    public void multipleListsWithSubscribers_onlyOwnSubscriberIsNotified() {
        ConnectionContextSpy ctx1 = new ConnectionContextSpy();
        ConnectionContextSpy ctx2 = new ConnectionContextSpy();
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
        context.setAllowDispatch(false);
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
}
