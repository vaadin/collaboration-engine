package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.TestUtils;

public class ConnectionContextTest {

    private MockConnectionContext context;
    private TopicConnection topicConnection;
    private CollaborationMap map;

    @Before
    public void init() {
        CollaborationEngine collaborationEngine = TestUtil
                .createTestCollaborationEngine();

        context = MockConnectionContext.createEager();

        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), tc -> {
                    topicConnection = tc;
                    map = tc.getNamedMap("map");
                    map.subscribe(event -> {
                    });
                    return () -> {
                    };
                });
    }

    @Test
    public void subscribe_actionDispatchedThroughContext() {
        map.put("foo", "bar");
        Assert.assertTrue("Context should be passed through.",
                context.getDispatchActionCount() > 0);
    }

    @Test
    public void setTopicValue_actionDispatchedThroughContext() {
        context.resetActionDispatchCount();
        map.put("foo", "bar");
        Assert.assertTrue("Context should be passed through.",
                context.getDispatchActionCount() > 0);
    }

    @Test
    public void serializedContext() {
        ConnectionContext deserializedContext = TestUtils.serialize(context);
    }
}
