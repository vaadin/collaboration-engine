package com.vaadin.collaborationengine.util;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.TopicConnection;

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
        CollaborationEngine.getInstance().openTopicConnection(
                new EagerConnectionContext(), topicId, topic -> {
                    handler.accept(topic);
                    return null;
                });
    }

}
