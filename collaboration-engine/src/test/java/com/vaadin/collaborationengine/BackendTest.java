/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.TestBackendFactory;

public class BackendTest {

    private TestBackendFactory backendFactory;

    @Before
    public void setup() {
        backendFactory = new TestBackendFactory();
    }

    @After
    public void cleanup() {
    }

    @Test
    public void nodeJoins_eventLogReceivesEvent() {
        CollaborationEngine node = createNode();
        Backend backend = node.getConfiguration().getBackend();
        UUID nodeId = backend.getNodeId();
        AtomicBoolean joined = new AtomicBoolean();

        backend.getMembershipEventLog().subscribe(null, (eventId, payload) -> {
            ObjectNode event = JsonUtil.fromString(payload);
            String type = event.get(JsonUtil.CHANGE_TYPE).asText();
            String id = event.get(JsonUtil.CHANGE_NODE_ID).asText();
            joined.set(JsonUtil.CHANGE_NODE_JOIN.equals(type)
                    && nodeId.toString().equals(id));
        });
        join(node);

        Assert.assertTrue("Join event not received", joined.get());
    }

    @Test
    public void nodeLeaves_eventLogReceivesEvent() {
        CollaborationEngine node = createNode();
        Backend backend = node.getConfiguration().getBackend();
        UUID nodeId = backend.getNodeId();
        AtomicBoolean left = new AtomicBoolean();

        backend.getMembershipEventLog().subscribe(null, (eventId, payload) -> {
            ObjectNode event = JsonUtil.fromString(payload);
            String type = event.get(JsonUtil.CHANGE_TYPE).asText();
            String id = event.get(JsonUtil.CHANGE_NODE_ID).asText();
            left.set(JsonUtil.CHANGE_NODE_LEAVE.equals(type)
                    && nodeId.toString().equals(id));
        });
        leave(node);

        Assert.assertTrue("Leave event not received", left.get());
    }

    @Test
    public void nodeJoins_onlyNode_isTopicLeader() {
        CollaborationEngine node = createNode();
        AtomicBoolean isLeader = new AtomicBoolean();

        join(node);

        node.openTopicConnection(node.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    isLeader.set(conn.getTopic().isLeader());
                    return null;
                });

        Assert.assertTrue("Node should be topic leader, but it's not",
                isLeader.get());
    }

    @Test
    public void nodeJoins_otherNodePresent_isNotTopicLeader() {
        CollaborationEngine node1 = createNode();
        CollaborationEngine node2 = createNode();
        AtomicBoolean isLeader = new AtomicBoolean(true);

        join(node1);

        node1.openTopicConnection(node1.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> null);

        join(node2);

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    isLeader.set(conn.getTopic().isLeader());
                    return null;
                });

        Assert.assertFalse("Node should not be topic leader, but it is",
                isLeader.get());
    }

    @Test
    public void twoNodes_leaderLeaves_otherBecomeLeader() {
        CollaborationEngine node1 = createNode();
        CollaborationEngine node2 = createNode();
        AtomicBoolean isLeader = new AtomicBoolean();

        join(node1);

        node1.openTopicConnection(node1.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> null);

        join(node2);

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> null);

        leave(node1);

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    isLeader.set(conn.getTopic().isLeader());
                    return null;
                });

        Assert.assertTrue("Node has not become topic leader", isLeader.get());
    }

    @Test
    public void mapEntryWithConnectionScope_ownerLeaves_entryRemoved() {
        CollaborationEngine node1 = createNode();
        CollaborationEngine node2 = createNode();

        join(node1);

        node1.openTopicConnection(node1.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    conn.getNamedMap("map").put("key", "value",
                            EntryScope.CONNECTION);
                    return null;
                });

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> null);

        leave(node1);

        AtomicBoolean staleEntryRemoved = new AtomicBoolean();

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    String value = conn.getNamedMap("map").get("key",
                            String.class);
                    staleEntryRemoved.set(value == null);
                    return null;
                });

        Assert.assertTrue("Stale entry not removed", staleEntryRemoved.get());
    }

    @Test
    public void listEntryWithConnectionScope_ownerLeaves_entryRemoved() {
        CollaborationEngine node1 = createNode();
        CollaborationEngine node2 = createNode();

        join(node1);

        node1.openTopicConnection(node1.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    conn.getNamedList("list").insertLast("value",
                            EntryScope.CONNECTION);
                    return null;
                });

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> null);

        leave(node1);

        AtomicBoolean staleEntryRemoved = new AtomicBoolean();

        node2.openTopicConnection(node2.getSystemContext(), "topic",
                new UserInfo("foo"), conn -> {
                    staleEntryRemoved.set(conn.getNamedList("list")
                            .getItems(String.class).isEmpty());
                    return null;
                });

        Assert.assertTrue("Stale entry not removed", staleEntryRemoved.get());
    }

    private CollaborationEngine createNode() {
        CollaborationEngineConfiguration conf = new CollaborationEngineConfiguration(
                e -> {
                });
        conf.setBackend(backendFactory.createBackend());
        return TestUtil.createTestCollaborationEngine(new MockService(), conf);
    }

    private void join(CollaborationEngine node) {
        backendFactory.join(node.getConfiguration().getBackend());
    }

    private void leave(CollaborationEngine node) {
        backendFactory.leave(node.getConfiguration().getBackend());
    }
}
