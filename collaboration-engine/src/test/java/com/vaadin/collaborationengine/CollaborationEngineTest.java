package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class CollaborationEngineTest {

    private CollaborationEngine collaborationEngine;
    private ConnectionContext context;
    private SerializableFunction<TopicConnection, Registration> connectionCallback;

    @Before
    public void init() {
        collaborationEngine = new CollaborationEngine();
        context = new ConnectionContext() {
            @Override
            public void setActivationHandler(ActivationHandler handler) {
                handler.setActive(true);
            }

            @Override
            public void dispatchAction(Command action) {
                // no impl
            }
        };

        connectionCallback = topicConnection -> () -> {
            // no impl
        };
    }

    @Test
    public void getInstance_notNull() {
        collaborationEngine.openTopicConnection(context, "foo",
                topicConnection -> {
                    Assert.assertNotNull(topicConnection);
                    return () -> {
                    };
                });
    }

    @Test
    public void getInstance_returnsAlwaysSameInstance() {
        Assert.assertSame(CollaborationEngine.getInstance(),
                CollaborationEngine.getInstance());
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullComponent_throws() {
        collaborationEngine.openTopicConnection((Component) null, "foo",
                connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullContext_throws() {
        collaborationEngine.openTopicConnection((ConnectionContext) null, "foo",
                connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullId_throws() {
        collaborationEngine.openTopicConnection(context, null,
                connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullCallback_throws() {
        collaborationEngine.openTopicConnection(context, "foo", null);
    }

    @Test
    public void openTopicConnection_sameTopicId_hasSameTopic() {
        Topic[] topics = new Topic[2];
        collaborationEngine.openTopicConnection(context, "foo",
                topicConnection -> {
                    topics[0] = topicConnection.getTopic();
                    return null;
                });

        ConnectionContext otherContext = new ConnectionContext() {
            @Override
            public void setActivationHandler(ActivationHandler handler) {
                handler.setActive(true);
            }

            @Override
            public void dispatchAction(Command action) {
                // no impl
            }
        };
        collaborationEngine.openTopicConnection(otherContext, "foo",
                topicConnection -> {
                    topics[1] = topicConnection.getTopic();
                    return null;
                });
        Assert.assertSame(topics[0], topics[1]);
    }

    @Test
    public void openTopicConnections_distinctTopicIds_hasDistinctTopics() {
        Topic[] topics = new Topic[2];
        collaborationEngine.openTopicConnection(context, "foo",
                topicConnection -> {
                    topics[0] = topicConnection.getTopic();
                    return null;
                });

        collaborationEngine.openTopicConnection(context, "baz",
                topicConnection -> {
                    topics[1] = topicConnection.getTopic();
                    return null;
                });
        Assert.assertNotSame(topics[0], topics[1]);
    }

}
