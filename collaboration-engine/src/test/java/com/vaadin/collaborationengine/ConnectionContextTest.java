package com.vaadin.collaborationengine;

import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class ConnectionContextTest {

    private SimpleConnectionContext simpleContext;
    private TopicConnection topicConnection;
    private CollaborationMap map;

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
        public Registration setActivationHandler(ActivationHandler handler) {
            handler.setActive(true);
            return null;
        }

        @Override
        public void dispatchAction(Command action) {
            isCalled = true;
            action.execute();
        }

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            return new CompletableFuture<>();
        }

        void reset() {
            isCalled = false;
        }
    }

}
