/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinService;

public class AbstractCollaborationManagerTest {

    private static final String TOPIC_ID = "manager";

    private VaadinService service;

    private SerializableSupplier<CollaborationEngine> ceSupplier;

    @Before
    public void init() {
        service = new MockService();
        VaadinService.setCurrent(service);
        CollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        ceSupplier = () -> ce;
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void openTopicConnection_activationCallbackExectuted() {
        Manager manager = createManager();
        MockConnectionContext context = new MockConnectionContext();
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
        MockConnectionContext context = new MockConnectionContext();
        AtomicBoolean activation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> null);
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
        MockConnectionContext context = new MockConnectionContext();
        AtomicBoolean activation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> null);
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
        MockConnectionContext context = new MockConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> null);
        context.activate();
        manager.setActivationHandler(() -> () -> deactivation.set(true));
        context.deactivate();
        Assert.assertTrue("Handler not invoked upon deactivate",
                deactivation.get());
    }

    @Test
    public void setDeactivationHandler_closeManager_handlerExecuted() {
        Manager manager = createManager();
        MockConnectionContext context = new MockConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> null);
        context.activate();
        manager.setActivationHandler(() -> () -> deactivation.set(true));
        manager.close();
        Assert.assertTrue("Handler not invoked upon close", deactivation.get());
    }

    @Test
    public void replaceActivationHandler_existingRegistrationRemoved() {
        Manager manager = createManager();
        MockConnectionContext context = new MockConnectionContext();
        AtomicBoolean deactivation = new AtomicBoolean();
        manager.openTopicConnection(context, connection -> null);
        context.activate();
        manager.setActivationHandler(() -> () -> deactivation.set(true));
        manager.setActivationHandler(() -> null);
        Assert.assertTrue(
                "Existing egistration not removed when the handler is replaced",
                deactivation.get());
    }

    @Test
    public void serializeManager() {
        Manager manager = createManager();

        Manager deserializedManager = TestUtils.serialize(manager);

        assertEquals(manager.getLocalUser(),
                deserializedManager.getLocalUser());
        assertEquals(manager.getTopicId(), deserializedManager.getTopicId());
    }

    private Manager createManager() {
        return new Manager(new UserInfo("local"), TOPIC_ID, ceSupplier);
    }

    static class Manager extends AbstractCollaborationManager {

        protected Manager(UserInfo localUser, String topicId,
                SerializableSupplier<CollaborationEngine> ceSupplier) {
            super(localUser, topicId, ceSupplier);
        }
    }
}
