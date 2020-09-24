package com.vaadin.collaborationengine;

import com.vaadin.collaborationengine.util.EagerConnectionContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MultipleCollaborationMapTest {

    private TopicConnection connection;
    private CollaborationMap namedMapData;

    @Before
    public void init() {
        ConnectionContext context = new EagerConnectionContext();
        new CollaborationEngine().openTopicConnection(context, "form",
                topic -> {
                    this.connection = topic;
                    namedMapData = topic.getNamedMap("values");
                    return null;
                });
    }

    @Test
    public void namedMap_put_newElementAdded() {
        namedMapData.put("firstName", "foo");
        Assert.assertEquals("foo",
                connection.getNamedMap("values").get("firstName"));
    }

    @Test
    public void namedMap_subscribe_mapChangeEventFired() {
        AtomicReference<MapChangeEvent> mapChangeEvent = new AtomicReference<>();
        namedMapData.subscribe(mapChangeEvent::set);

        connection.getNamedMap("values").put("foo", "bar");
        Assert.assertEquals("foo", mapChangeEvent.get().getKey());
        Assert.assertEquals("bar", mapChangeEvent.get().getValue());
    }

    @Test
    public void namedMap_concurrentPut_newElementsAdded()
            throws InterruptedException {
        List<Integer> resultList = parallelPut(namedMapData, 100);
        Assert.assertEquals(1, resultList.stream().distinct().count());
        long wrongResultCount = resultList.stream().filter(num -> num != 1000)
                .count();
        Assert.assertEquals(wrongResultCount, 0);
    }

    private List<Integer> parallelPut(CollaborationMap map, int executionTimes)
            throws InterruptedException {
        int[] count = { 0 };
        map.subscribe(e -> count[0]++);
        List<Integer> resultList = new ArrayList<>();

        AtomicInteger tempVal = new AtomicInteger(0);
        for (int i = 0; i < executionTimes; i++) {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            for (int j = 0; j < 10; j++) {
                executor.execute(() -> {
                    for (int k = 0; k < 100; k++) {
                        map.put("foo", tempVal.incrementAndGet());
                    }
                });
            }
            tempVal.set(0);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            resultList.add(count[0]);
            count[0] = 0;
        }
        return resultList;
    }

}
