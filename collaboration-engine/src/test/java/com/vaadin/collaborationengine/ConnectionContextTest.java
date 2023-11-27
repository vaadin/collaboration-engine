package com.vaadin.collaborationengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.SpyActivationHandler;
import com.vaadin.collaborationengine.util.TestComponent;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.server.VaadinService;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionContextTest implements Serializable {

    private transient CollaborationEngine ce;
    private transient MockUI ui;
    private transient MockConnectionContext context;
    private transient SpyActivationHandler spy;
    private transient CollaborationMap map;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        ce = TestUtil.createTestCollaborationEngine();

        ui = new MockUI();
        context = MockConnectionContext.createEager();
        spy = new SpyActivationHandler();

        ce.openTopicConnection(context, "foo", SystemUserInfo.getInstance(),
                tc -> {
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
        assertTrue("Context should be passed through.",
                context.getDispatchActionCount() > 0);
    }

    @Test
    public void setTopicValue_actionDispatchedThroughContext() {
        context.resetActionDispatchCount();
        map.put("foo", "bar");
        assertTrue("Context should be passed through.",
                context.getDispatchActionCount() > 0);
    }

    @Test
    public void serializedContext() {
        ConnectionContext deserializedContext = TestUtils.serialize(context);
    }
}
