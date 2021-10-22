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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.TopicConnection.CollaborationListImplementation;
import com.vaadin.collaborationengine.util.MockConnectionContext;

public class CollaborationListTest {

    private static final TypeReference<String> STRING_REF = new TypeReference<String>() {
    };

    private static class ListAppendSpy implements ListSubscriber {

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

    private static class EventCollector extends ArrayList<ListChangeEvent>
            implements ListSubscriber {
        @Override
        public void onListChange(ListChangeEvent event) {
            add(event);
        }
    }

    private MockConnectionContext context;
    private CollaborationEngine ce;
    private TopicConnection connection;
    private CollaborationList list;
    // This field is here only to test API that is not yet public
    @Deprecated()
    private CollaborationListImplementation privateList;
    private ListAppendSpy appendSpy;
    private EventCollector eventCollector;
    private TopicConnectionRegistration registration;

    @Before
    public void init() {
        context = MockConnectionContext.createEager();
        ce = TestUtil.createTestCollaborationEngine();
        registration = ce.openTopicConnection(context, "topic",
                SystemUserInfo.getInstance(), topicConnection -> {
                    connection = topicConnection;
                    list = connection.getNamedList("foo");
                    privateList = (CollaborationListImplementation) list;
                    return null;
                });
        appendSpy = new ListAppendSpy();
        eventCollector = new EventCollector();
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

        appendSpy.addExpectedEvent("foo");
        appendSpy.addExpectedEvent("bar");

        list.subscribe(appendSpy);

        appendSpy.assertNoExpectedEvents();
    }

    @Test
    public void listWithRemovedSubscriber_appendItem_subscriberNotNotified() {
        list.subscribe(appendSpy).remove();

        list.append("foo"); // Spy would throw if notified of unexpected "foo"
    }

    @Test
    public void listWithSubscriber_addAnotherSubscriber_noEventForOldSubscriber() {
        list.append("foo");

        appendSpy.addExpectedEvent("foo");
        list.subscribe(appendSpy);
        appendSpy.assertNoExpectedEvents();

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

        ListAppendSpy spy1 = new ListAppendSpy();
        ListAppendSpy spy2 = new ListAppendSpy();

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

    @Test
    public void threeItems_removeFirst_twoRemaining() {
        List<ListKey> keys = insertLast(privateList, "one", "two", "three");

        privateList.set(keys.get(0), null);

        assertValues(privateList, "two", "three");
    }

    @Test
    public void threeItems_removeMiddle_twoRemaining() {
        List<ListKey> keys = insertLast(privateList, "one", "two", "three");

        privateList.set(keys.get(1), null);

        assertValues(privateList, "one", "three");
    }

    @Test
    public void threeItems_removeLast_twoRemaining() {
        List<ListKey> keys = insertLast(privateList, "one", "two", "three");

        privateList.set(keys.get(2), null);

        assertValues(privateList, "one", "two");
    }

    @Test
    public void oneItem_removeIt_listEmpty() {
        ListKey key = privateList.insertLast("one").getKey();

        privateList.set(key, null);

        assertValues(privateList);
    }

    @Test
    public void listEmptied_addOne_nothingCorrupted() {
        ListKey key = privateList.insertLast("one").getKey();
        privateList.set(key, null);

        list.append("another");

        assertValues(privateList, "another");
    }

    @Test
    public void listWithItem_setValue_valueSet()
            throws InterruptedException, ExecutionException {
        ListKey key = privateList.insertLast("one").getKey();

        Boolean result = privateList.set(key, "two").get();

        assertValues(privateList, "two");
        Assert.assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void listWithRemovedItem_setValue_negativeResult()
            throws InterruptedException, ExecutionException {
        ListKey key = privateList.insertLast("one").getKey();
        privateList.set(key, null);

        Boolean result = privateList.set(key, "two").get();

        Assert.assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void emptyListWithSubscriber_appendItems_eventDetailsComplete() {
        list.subscribe(eventCollector);

        ListKey key1 = privateList.insertLast("one").getKey();
        ListKey key2 = privateList.insertLast("two").getKey();

        Assert.assertEquals(2, eventCollector.size());
        assertEvent(ListChangeType.INSERT, key1, null, "one", null, null, null,
                null, eventCollector.get(0));

        assertEvent(ListChangeType.INSERT, key2, null, "two", null, key1, null,
                null, eventCollector.get(1));
    }

    @Test
    public void populatedList_subscribe_eventDetailsComplete() {
        ListKey key1 = privateList.insertLast("one").getKey();
        ListKey key2 = privateList.insertLast("two").getKey();

        list.subscribe(eventCollector);

        Assert.assertEquals(2, eventCollector.size());
        assertEvent(ListChangeType.INSERT, key1, null, "one", null, null, null,
                null, eventCollector.get(0));

        assertEvent(ListChangeType.INSERT, key2, null, "two", null, key1, null,
                null, eventCollector.get(1));
    }

    @Test
    public void populatedListWithSubscriber_changeItems_eventDetailsComplete() {
        List<ListKey> keys = insertLast(privateList, "one", "two", "three");
        list.subscribe(eventCollector);
        eventCollector.clear();

        keys.forEach(key -> privateList.set(key,
                privateList.getItem(key, String.class).toUpperCase()));

        Assert.assertEquals(3, eventCollector.size());
        assertEvent(ListChangeType.SET, keys.get(0), "one", "ONE", null, null,
                keys.get(1), keys.get(1), eventCollector.get(0));
        assertEvent(ListChangeType.SET, keys.get(1), "two", "TWO", keys.get(0),
                keys.get(0), keys.get(2), keys.get(2), eventCollector.get(1));
        assertEvent(ListChangeType.SET, keys.get(2), "three", "THREE",
                keys.get(1), keys.get(1), null, null, eventCollector.get(2));
    }

    @Test
    public void populatedListWithSubscriber_removeMiddleItem_eventDetailsComplete() {
        List<ListKey> keys = insertLast(privateList, "one", "two", "three");
        list.subscribe(eventCollector);
        eventCollector.clear();

        privateList.set(keys.get(1), null);

        Assert.assertEquals(1, eventCollector.size());
        assertEvent(ListChangeType.SET, keys.get(1), "two", null, keys.get(0),
                null, keys.get(2), null, eventCollector.get(0));
    }

    @Test
    public void insertLastWithConnectionScope_connectiondDeactivated_entryRemoved() {
        ListKey key = privateList.insertLast("foo", EntryScope.CONNECTION)
                .getKey();
        context.deactivate();
        context.activate();
        Assert.assertNull(privateList.getItem(key, String.class));
        Assert.assertEquals(0, privateList.getKeys().count());
    }

    @Test
    public void insertLastWithTopicScope_setWithConnectionScope_connectionDeactivated_entryRemoved() {
        ListKey key = privateList.insertLast("foo").getKey();
        privateList.set(key, "foo", EntryScope.CONNECTION);
        context.deactivate();
        context.activate();
        Assert.assertNull(privateList.getItem(key, String.class));
        Assert.assertEquals(0, privateList.getKeys().count());
    }

    @Test
    public void insertLastWithConnectionScope_setWithTopicScope_connectionDeactivated_entryNotRemoved() {
        ListKey key = privateList.insertLast("foo", EntryScope.CONNECTION)
                .getKey();
        privateList.set(key, "foo");
        context.deactivate();
        context.activate();
        Assert.assertEquals("foo", privateList.getItem(key, String.class));
    }

    private static List<ListKey> insertLast(
            CollaborationListImplementation list, String... values) {
        return Stream.of(values).map(list::insertLast)
                .map(ListInsertResult::getKey).collect(Collectors.toList());
    }

    private static void assertEvent(ListChangeType type, ListKey key,
            String oldValue, String value, ListKey oldAfter, ListKey after,
            ListKey oldBefore, ListKey before, ListChangeEvent event) {
        Assert.assertEquals(type, event.getType());
        Assert.assertEquals(key, event.getKey());
        Assert.assertEquals(oldValue, event.getOldValue(String.class));
        Assert.assertEquals(oldValue, event.getOldValue(STRING_REF));
        Assert.assertEquals(value, event.getValue(String.class));
        Assert.assertEquals(value, event.getValue(STRING_REF));
        Assert.assertEquals(oldAfter, event.getOldAfter());
        Assert.assertEquals(after, event.getAfter());
        Assert.assertEquals(oldBefore, event.getOldBefore());
        Assert.assertEquals(before, event.getBefore());
    }

    private static void assertValues(CollaborationListImplementation list,
            String... values) {
        Assert.assertEquals(Arrays.asList(values), list.getItems(String.class));
        Assert.assertEquals(Arrays.asList(values), list.getItems(STRING_REF));

        Assert.assertEquals(Arrays.asList(values),
                list.getKeys().map(key -> list.getItem(key, String.class))
                        .collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(values),
                list.getKeys().map(key -> list.getItem(key, STRING_REF))
                        .collect(Collectors.toList()));
    }
}
