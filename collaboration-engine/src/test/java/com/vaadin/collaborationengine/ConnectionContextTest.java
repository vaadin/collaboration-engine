package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Command;

public class ConnectionContextTest {

    private SimpleConnectionContext simpleContext;
    private TopicConnection topicConnection;
    private CollaborativeMap map;

    @Before
    public void init() {
        CollaborationEngine collaborationEngine = new CollaborationEngine();
        simpleContext = new SimpleConnectionContext();

        collaborationEngine.openTopicConnection(simpleContext, "foo", tc -> {
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
                simpleContext.isCalled);
    }

    @Test
    public void setTopicValue_actionDispatchedThroughContext() {
        simpleContext.reset();
        map.put("foo", "bar");
        Assert.assertTrue("Context should be passed through.",
                simpleContext.isCalled);
    }

    static class SimpleConnectionContext implements ConnectionContext {
        boolean isCalled = false;

        @Override
        public void setActivationHandler(ActivationHandler handler) {
            handler.setActive(true);
        }

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
