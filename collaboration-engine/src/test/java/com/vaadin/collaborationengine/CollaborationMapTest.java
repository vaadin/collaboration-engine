package com.vaadin.collaborationengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class CollaborationMapTest {

    private static class MapSubscriberSpy implements MapSubscriber {
        private final Map<String, String> expectedOld = new HashMap<>();
        private final Map<String, String> expectedNew = new HashMap<>();

        public void addExpectedEvent(String key, String oldValue,
                String newValue) {
            Assert.assertFalse("Should not expect same value again",
                    expectedOld.containsKey(key));

            expectedOld.put(key, oldValue);
            expectedNew.put(key, newValue);
        }

        @Override
        public void onMapChange(MapChangeEvent event) {
            String key = event.getKey();
            Assert.assertTrue("Event should be expected for key " + key,
                    expectedOld.containsKey(key));
            Assert.assertEquals(expectedOld.remove(key),
                    event.getOldValue(String.class));
            Assert.assertEquals(expectedNew.remove(key),
                    event.getValue(String.class));
        }

        public void assertNoExpectedEvents() {
            Assert.assertEquals("There are expected events",
                    Collections.emptySet(), expectedOld.keySet());
        }

    }

    private ToggleableConnectionContext context;
    private TopicConnection connection;
    private CollaborationMap map;
    private MapSubscriberSpy spy;

    @Before
    public void init() {
        context = new ToggleableConnectionContext();
        CollaborationEngine ce = new CollaborationEngine();
        TestUtil.setDummyCollaborationEngineConfig(ce);
        ce.openTopicConnection(context, "topic", SystemUserInfo.getInstance(),
                topicConnection -> {
                    this.connection = topicConnection;
                    map = connection.getNamedMap("foo");
                    return null;
                });
        spy = new MapSubscriberSpy();
    }

    @Test
    public void mapWithValues_subscribe_eventsForValues() {
        map.put("one", "first");
        map.put("two", "second");

        spy.addExpectedEvent("one", null, "first");
        spy.addExpectedEvent("two", null, "second");

        map.subscribe(spy);

        spy.assertNoExpectedEvents();
    }

    @Test
    public void mapWithPreviousValues_subscribe_noEvents() {
        map.put("one", "first");
        map.put("one", null);

        map.subscribe(spy);
    }

    @Test
    public void mapWithSubscriber_setNewValue_valueChangedAndEventFired() {
        map.subscribe(spy);

        spy.addExpectedEvent("one", null, "first");
        map.put("one", "first");

        spy.assertNoExpectedEvents();

        Assert.assertEquals("first", map.get("one", String.class));
    }

    @Test
    public void mapWithSubscriber_setSameValue_noEventFired() {
        map.put("one", "first");
        spy.addExpectedEvent("one", null, "first");
        map.subscribe(spy);
        spy.assertNoExpectedEvents();

        map.put("one", "first");
        // Spy would throw if an event is fired
    }

    @Test
    public void mapWithSubscriber_replace_valueChangedAndEventFired()
            throws ExecutionException, InterruptedException {
        map.subscribe(spy);

        spy.addExpectedEvent("one", null, "first");
        Boolean succeeded = map.replace("one", null, "first").get();
        Assert.assertTrue("Replace should be successful", succeeded);

        spy.assertNoExpectedEvents();
        Assert.assertEquals("first", map.get("one", String.class));
    }

    @Test
    public void mapWithSubscriber_replaceWrongExpectedValue_noChange()
            throws ExecutionException, InterruptedException {
        map.subscribe(spy);

        Boolean succeeded = map.replace("one", "other", "first").get();
        Assert.assertFalse("Replace should not succeed", succeeded);
        Assert.assertNull("Old value should be present",
                map.get("one", String.class));
    }

    @Test
    public void mapWithSubscriber_replaceCurrentValue_succeedNoEvent()
            throws ExecutionException, InterruptedException {
        map.subscribe(spy);

        Boolean succeeded = map.replace("one", null, null).get();
        Assert.assertTrue("Replace should succeed", succeeded);
        // spy would have throw if an event was fired
    }

    @Test
    public void mapWithRemovedValues_getKeys_containsOnlyPresentValues() {
        map.put("one", "first");
        map.put("two", "second");
        map.put("one", null);

        List<String> keys = map.getKeys().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("two"), keys);
    }

    @Test
    public void oldKeysSnapshot_newValueAdded_snapshotUnchanged() {
        map.put("one", "first");
        Stream<String> keysStream = map.getKeys();
        map.put("two", "second");

        List<String> keys = keysStream.collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("one"), keys);
    }

    @Test
    public void mapWithRemovedSubscriber_valueChange_subscriberNotNotified() {
        Registration registration = map.subscribe(spy);

        registration.remove();

        map.put("one", "first");
        // Spy would throw if there was an event
    }

    @Test
    public void mapWithSubscriber_addAnotherSubscriber_noEventForOldSubscriber() {
        map.put("one", "first");

        spy.addExpectedEvent("one", null, "first");
        map.subscribe(spy);
        spy.assertNoExpectedEvents();

        map.subscribe(ignore -> {

        });
        // Spy would throw if there was an event
    }

    @Test
    public void mapWithExistingValue_updateValue_eventHasOldValue() {
        map.put("one", "first");
        spy.addExpectedEvent("one", null, "first");
        map.subscribe(spy);

        spy.addExpectedEvent("one", "first", "updated");
        map.put("one", "updated");

        spy.assertNoExpectedEvents();
    }

    @Test
    public void mapWithExistingValue_removeValue_eventNewValueIsNull() {
        map.put("one", "first");
        spy.addExpectedEvent("one", null, "first");
        map.subscribe(spy);

        spy.addExpectedEvent("one", "first", null);
        map.put("one", null);

        spy.assertNoExpectedEvents();
    }

    @Test
    public void put_contextCannotDispatch_unresolved() {
        context.setAllowDispatch(false);
        CompletableFuture<Void> put = map.put("one", "first");
        Assert.assertFalse(put.isDone());
    }

    @Test
    public void replace_contextCannotDispatch_unresolved() {
        map.put("one", "first");
        context.setAllowDispatch(false);
        CompletableFuture<Boolean> replace = map.replace("one", "first",
                "second");
        Assert.assertFalse(replace.isDone());
    }

    @Test
    public void replace_contextCanDispatch_resolved()
            throws InterruptedException, ExecutionException {
        map.put("one", "first");
        Boolean isReplaced = map.replace("one", "first", "second").get();
        Assert.assertTrue(isReplaced);
    }

    @Test
    public void replace_mapValueImmutable() {
        List<String> data = Arrays.asList("foo", "bar");
        ArrayNode jsonNode = (ArrayNode) JsonUtil.toJsonNode(data);
        map.replace("key", null, jsonNode);

        data.set(0, "qux");
        jsonNode.add("lorem");

        List<String> mapData = map.get("key", MockJson.LIST_STRING_TYPE_REF);
        Assert.assertEquals(
                "Map data shouldn't be changed by the original instances.",
                mapData, Arrays.asList("foo", "bar"));
    }

    @Test
    public void get_mapValueImmutable() {
        List<String> data = Arrays.asList("foo", "bar");
        map.put("key", data);

        ArrayNode mapDataNode = map.get("key", ArrayNode.class);
        mapDataNode.set(0, new TextNode("baz"));

        Assert.assertTrue(
                "The returned value shouldn't affect map data in the topic.",
                data.containsAll(
                        map.get("key", MockJson.LIST_STRING_TYPE_REF)));
    }

    private class ToggleableConnectionContext extends EagerConnectionContext {

        private boolean allowDispatch = true;

        public void setAllowDispatch(boolean allowDispatch) {
            this.allowDispatch = allowDispatch;
        }

        @Override
        public void dispatchAction(Command action) {
            if (allowDispatch) {
                action.execute();
            }
        }
    }
}
