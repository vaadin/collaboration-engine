package com.vaadin.collaborationengine;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.SpyConnectionContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

public class AbstractCollaborationManagerTest {

    private static final String TOPIC_ID = "manager";

    private VaadinService service;

    private CollaborationEngine ce;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        ce = TestUtil.createTestCollaborationEngine(service);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void openTopicConnection_activationCallbackExectuted() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean activation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            activation.set(true);
            return null;
        });

        Assert.assertFalse("Callback invoked before activation",
                activation.get());
        context.activate();
        Assert.assertTrue("Callback not invoked upon activation",
                activation.get());
    }

    @Test
    public void setActivationHandler_activateContext_handlerExecuted() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean activation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            return null;
        });
        manager.setActivationHandler(() -> {
            activation.set(true);
            return null;
        });

        Assert.assertFalse("Handler invoked before activation",
                activation.get());
        context.activate();
        Assert.assertTrue("Handler not invoked upon activation",
                activation.get());
    }

    @Test
    public void activateContext_setActivationHandler_handlerExecuted() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean activation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            return null;
        });
        context.activate();
        manager.setActivationHandler(() -> {
            activation.set(true);
            return null;
        });
        Assert.assertTrue("Handler not invoked with already active context",
                activation.get());
    }

    @Test
    public void setDeactivationHandler_deactivateContext_handlerExecuted() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            return null;
        });
        context.activate();
        manager.setActivationHandler(() -> {
            return () -> deactivation.set(true);
        });
        context.deactivate();
        Assert.assertTrue("Handler not invoked upon deactivate",
                deactivation.get());
    }

    @Test
    public void setDeactivationHandler_closeManager_handlerExecuted() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            return null;
        });
        context.activate();
        manager.setActivationHandler(() -> {
            return () -> deactivation.set(true);
        });
        manager.close();
        Assert.assertTrue("Handler not invoked upon close", deactivation.get());
    }

    @Test
    public void replaceActivationHandler_existingRegistrationRemoved() {
        Manager manager = createManager();
        SpyConnectionContext context = new SpyConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> {
            return null;
        });
        context.activate();
        manager.setActivationHandler(() -> {
            return () -> deactivation.set(true);
        });
        manager.setActivationHandler(() -> null);
        Assert.assertTrue(
                "Existing egistration not removed when the handler is replaced",
                deactivation.get());
    }

    private Manager createManager() {
        return new Manager(new UserInfo("local"), TOPIC_ID, ce);
    }

    static class Manager extends AbstractCollaborationManager {

        protected Manager(UserInfo localUser, String topicId,
                CollaborationEngine collaborationEngine) {
            super(localUser, topicId, collaborationEngine);
        }
    }
}
