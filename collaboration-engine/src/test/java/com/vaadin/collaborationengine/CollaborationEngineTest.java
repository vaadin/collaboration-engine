package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CollaborationEngineTest {

    private CollaborationEngine collaborationEngine;

    @Before
    public void init() {
        collaborationEngine = CollaborationEngine.getInstance();
    }

    @Test
    public void getInstance_notNull() {
        Assert.assertNotNull(collaborationEngine);
    }

    @Test
    public void getInstance_returnsAlwaysSameInstance() {
        Assert.assertSame(collaborationEngine,
                CollaborationEngine.getInstance());
    }

    @Test
    public void openTopicConnection_notNull() {
        Assert.assertNotNull(collaborationEngine.openTopicConnection("foo"));
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullId_throws() {
        collaborationEngine.openTopicConnection(null);
    }

    @Test
    public void openTopicConnections_sameTopicId_hasSameTopic() {
        Assert.assertSame(
                collaborationEngine.openTopicConnection("foo").getTopic(),
                collaborationEngine.openTopicConnection("foo").getTopic());
    }

    @Test
    public void openTopicConnections_distinctTopicIds_hasDistinctTopics() {
        Assert.assertNotSame(
                collaborationEngine.openTopicConnection("foo").getTopic(),
                collaborationEngine.openTopicConnection("bar").getTopic());
    }

}
