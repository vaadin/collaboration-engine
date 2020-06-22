package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Command;

public class ConnectionContextTest {

    private CollaborationEngine collaborationEngine;
    private TopicConnection topicConnection;
    private SimpleConnectionContext simpleContext;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        simpleContext = new SimpleConnectionContext();
        topicConnection = collaborationEngine.openTopicConnection(simpleContext,
                "foo");
    }

    @Test
    public void subscribe_actionDispatchedThroughContext() {
        topicConnection.subscribe(newValue -> {
            /* no impl */});
        Assert.assertTrue("Context should be passed through.",
                simpleContext.isCalled);
    }

    @Test
    public void setTopicValue_actionDispatchedThroughContext() {
        topicConnection.subscribe(newValue -> {
            /* no impl */});
        simpleContext.reset();
        topicConnection.setValue("bar");
        Assert.assertTrue("Context should be passed through.",
                simpleContext.isCalled);
    }

    static class SimpleConnectionContext implements ConnectionContext {
        boolean isCalled = false;

        @Override
        public void dispatchAction(Command action) {
            isCalled = true;
            action.execute();
        }

        void reset() {
            isCalled = false;
        }
    }

}
