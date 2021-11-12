package com.vaadin.collaborationengine;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.VaadinService;

public class SnapshotTest {

    private VaadinService service;
    private CollaborationEngine ce;
    private MockConnectionContext context;
    private TopicConnectionRegistration registration;
    private TopicConnection connection;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        context = MockConnectionContext.createEager();
        ce = TestUtil.createTestCollaborationEngine();
        registration = ce.openTopicConnection(context, "topic",
                SystemUserInfo.getInstance(), topicConnection -> {
                    connection = topicConnection;
                    return null;
                });
    }

    @After
    public void tearDown() {
        registration.remove();
        VaadinService.setCurrent(null);
    }

    @Test
    public void getMaps_expectedValues() {
        CollaborationMap map = connection.getNamedMap("foo");
        map.put("1", "a");
        map.put("2", "b");
        Topic.Snapshot snapshot = Topic.Snapshot
                .fromTopic(connection.getTopic(), UUID.randomUUID());
        Assert.assertEquals(1, snapshot.getMaps().size());
        Map<String, Topic.Entry> collaborationMapSnapshot = snapshot.getMaps()
                .get("foo");
        Assert.assertNotNull(collaborationMapSnapshot);
        Assert.assertEquals("a",
                collaborationMapSnapshot.get("1").data.asText());
        Assert.assertEquals("b",
                collaborationMapSnapshot.get("2").data.asText());
    }

    @Test
    public void getLists_expectedValues() {
        CollaborationList list = connection.getNamedList("foo");
        list.append("a");
        list.append("b");
        list.append("c");
        list.append("b");

        Topic.Snapshot snapshot = Topic.Snapshot
                .fromTopic(connection.getTopic(), UUID.randomUUID());
        Assert.assertEquals(1, snapshot.getLists().size());
        List<EntryList.ListEntrySnapshot> listSnapshot = snapshot.getLists()
                .get("foo").stream().collect(Collectors.toList());
        Iterator<EntryList.ListEntrySnapshot> iterator = listSnapshot
                .iterator();
        Assert.assertEquals("a", iterator.next().value.asText());
        Assert.assertEquals("b", iterator.next().value.asText());
        Assert.assertEquals("c", iterator.next().value.asText());
        Assert.assertEquals("b", iterator.next().value.asText());
    }

    @Test
    public void getBackendNodes_expectedValues() {
        Topic.Snapshot snapshot = Topic.Snapshot
                .fromTopic(connection.getTopic(), UUID.randomUUID());
        UUID expectedId = ce.getConfiguration().getBackend().getNodeId();
        Assert.assertEquals(Collections.singletonList(expectedId),
                snapshot.getBackendNodes());
    }
}
