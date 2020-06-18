package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CollaborationEngineTest {

    private CollaborationEngine collaborationEngine;
    private ConnectionContext context;

    @Before
    public void init() {
        collaborationEngine = CollaborationEngine.getInstance();
        context = action -> {
            /* no implementation */};
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
        Assert.assertNotNull(
                collaborationEngine.openTopicConnection(context, "foo"));
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullId_throws() {
        collaborationEngine.openTopicConnection(context, null);
    }

    @Test
    public void openTopicConnections_sameTopicId_hasSameTopic() {
        Assert.assertSame(
                collaborationEngine.openTopicConnection(context, "foo")
                        .getTopic(),
                collaborationEngine.openTopicConnection(context, "foo")
                        .getTopic());
    }

    @Test
    public void openTopicConnections_distinctTopicIds_hasDistinctTopics() {
        Assert.assertNotSame(
                collaborationEngine.openTopicConnection(context, "foo")
                        .getTopic(),
                collaborationEngine.openTopicConnection(context, "bar")
                        .getTopic());
    }

}
