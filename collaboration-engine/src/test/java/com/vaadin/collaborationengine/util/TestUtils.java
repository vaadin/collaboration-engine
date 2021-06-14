package com.vaadin.collaborationengine.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Assert;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;

public class TestUtils {

    public static boolean isGarbageCollected(WeakReference<?> ref)
            throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            if (ref.get() == null) {
                return true;
            }
        }
        return false;
    }

    public static void clearMap(String topicId, String mapName,
            String... keys) {
        openEagerConnection(topicId, topicConnection -> {
            CollaborationMap map = topicConnection.getNamedMap(mapName);
            for (String key : keys) {
                map.put(key, null);
            }
        });
    }

    public static void openEagerConnection(String topicId,
            Consumer<TopicConnection> handler) {
        openEagerConnection(CollaborationEngine.getInstance(), topicId,
                handler);
    }

    public static void openEagerConnection(CollaborationEngine ce,
            String topicId, Consumer<TopicConnection> handler) {
        ce.openTopicConnection(new EagerConnectionContext(), topicId,
                new UserInfo(UUID.randomUUID().toString()), topic -> {
                    handler.accept(topic);
                    return null;
                });
    }

    public static void assertNullNode(String message, Object value) {
        Assert.assertEquals(message, NullNode.class, value.getClass());
    }

    @SafeVarargs
    public static <E> Set<E> newHashSet(E... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
