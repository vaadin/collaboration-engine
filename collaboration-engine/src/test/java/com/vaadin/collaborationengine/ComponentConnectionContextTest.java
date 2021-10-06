package com.vaadin.collaborationengine;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.TestUtil.TestCollaborationEngine;
import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.SpyActivationHandler;
import com.vaadin.collaborationengine.util.TestComponent;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;

public class ComponentConnectionContextTest {
    private MockUI ui;
    private TestComponent component;
    private SpyActivationHandler activationHandler;
    private Executor executor = Runnable::run;

    @Before
    public void init() {
        ui = new MockUI();
        component = new TestComponent();

        activationHandler = new SpyActivationHandler();
    }

    @Test
    public void unattachedComponent_setActivationHandler_isActivated() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertInactive("Context should not be active");
    }

    @Test
    public void attachedComponent_setActivationHandler_isActivated() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);

        activationHandler.assertActive("Context should be active");
    }

    @Test
    public void activationHandlerAndComponentSet_attachComponent_isActivated() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertInactive("Should initially be inactive");

        ui.add(component);

        activationHandler.assertActive(
                "Context should be active after attaching component");
    }

    @Test
    public void activationHandlerSet_addDetachedComponent_isNotActivated() {
        ComponentConnectionContext context = new ComponentConnectionContext();
        context.init(activationHandler, executor);

        context.addComponent(component);

        activationHandler.assertInactive(
                "Adding detached component should not activate context");
    }

    @Test
    public void activationHandlerSet_addAttachedComponent_isActivated() {
        ComponentConnectionContext context = new ComponentConnectionContext();
        context.init(activationHandler, executor);
        ui.add(component);

        context.addComponent(component);

        activationHandler.assertActive(
                "Adding attached component should activate context");
    }

    @Test
    public void activationHandlerSet_addAttachedComponentTwice_onlyActivatedOnce() {
        ComponentConnectionContext context = new ComponentConnectionContext();
        context.init(activationHandler, executor);
        ui.add(component);

        context.addComponent(component);
        context.addComponent(component);

        // Spy would throw if activated again without intermediate assertActive
    }

    @Test
    public void activationHandlerSet_removeNonexistentComponent_nothingHappened() {
        ComponentConnectionContext context = new ComponentConnectionContext();
        final String message = "Activation handler should never be triggered"
                + " when removing nonexistent component";
        context.init(dispatcher -> Assert.fail(message), executor);

        context.removeComponent(component);
    }

    @Test
    public void activationHandlerAndAttachedComponentSet_detachComponent_isDeactivatedAndKeepListeners() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertActive("Sanity check, clear expectation");

        ui.remove(component);

        activationHandler
                .assertInactive("Should deactivate when component is detached");
        Assert.assertTrue("Attach listener should still be active after detach",
                component.hasAttachListener());
    }

    @Test
    public void activationHandlerAndAttachedComponentSet_removeDetachedComponent_listenerRemoved() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertActive("Sanity check, clear expectation");

        ui.remove(component);

        // Sanity check
        Assert.assertTrue(component.hasAttachListener());

        context.removeComponent(component);

        Assert.assertFalse(
                "Attach listener should be removed after removing component",
                component.hasAttachListener());
    }

    @Test
    public void activationHandlerAndAttachedComponentSet_removeComponent_isDeactivatedAndRemoveListeners() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertActive("Sanity check, clear expectation");

        context.removeComponent(component);

        activationHandler
                .assertInactive("Should deactivate when component is removed");
        Assert.assertFalse(
                "Attach listener should no longer be present after remove",
                component.hasAttachListener());
    }

    @Test
    public void activeContext_removeComponentTwice_onlyDeactivateOnce() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertActive("Sanity check, clear expectation");

        context.removeComponent(component);
        context.removeComponent(component);

        // Spy would throw if deactivated again without intermediate check
    }

    @Test
    public void contextWithMultipleComponents_closeConnection_noListenersInComponents() {
        TestComponent c2 = new TestComponent();

        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.addComponent(c2);

        Registration registration = context.init(
                dispatcher -> Assert.assertFalse(
                        "Should only be triggered for deactivation", false),
                executor);
        // Sanity checks
        Assert.assertTrue(component.hasAttachListener());
        Assert.assertTrue(component.hasDetachListener());
        Assert.assertTrue(c2.hasAttachListener());
        Assert.assertTrue(c2.hasDetachListener());

        registration.remove();

        Assert.assertFalse(component.hasAttachListener());
        Assert.assertFalse(component.hasDetachListener());
        Assert.assertFalse(c2.hasAttachListener());
        Assert.assertFalse(c2.hasDetachListener());
    }

    @Test(expected = NullPointerException.class)
    public void emptyContext_addNullComponent_throws() {
        new ComponentConnectionContext().addComponent(null);
    }

    @Test(expected = NullPointerException.class)
    public void emptyContext_removeNullComponent_throws() {
        new ComponentConnectionContext().removeComponent(null);
    }

    @Test
    public void activeContext_addDetachedComponent_contextStillActive() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        ui.add(component);
        context.init(activationHandler, executor);

        context.addComponent(new TestComponent());

        activationHandler.assertActive(
                "Active context should remain active after adding detached component");
    }

    @Test
    public void activeContextWithTwoComponents_removeComponents_activeAfterFirstRemovalInactiveAfterSecond() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        ui.add(component);
        context.init(activationHandler, executor);

        TestComponent c2 = new TestComponent();
        ui.add(c2);

        // Spy would throw if activated again without intermediate assertActive
        context.addComponent(c2);

        context.removeComponent(component);

        activationHandler.assertActive(
                "Should be active after removing only one component");

        context.removeComponent(c2);

        activationHandler.assertInactive(
                "Should be inactive after removing all components");
    }

    @Test
    public void activeContextWithTwoComponents_detachComponents_activeAfterFirstDetachInactiveAfterSecond() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        ui.add(component);
        context.init(activationHandler, executor);

        TestComponent c2 = new TestComponent();
        ui.add(c2);

        // Spy would throw if activated again without intermediate assertActive
        context.addComponent(c2);

        ui.remove(component);

        activationHandler.assertActive(
                "Should be active after detaching only one component");

        ui.remove(c2);

        activationHandler.assertInactive(
                "Should be inactive after detaching all components");
    }

    @Test
    public void activeContext_dispatchAction_owningUiAccessed() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);

        ArrayList<String> executed = new ArrayList<>();
        Command command = () -> executed.add("command");
        context.init(activationHandler, Runnable::run);
        ui.setExecuteAccessTasks(false);

        activationHandler.getActionDispatcher().dispatchAction(command);
        Assert.assertEquals(Collections.emptyList(), executed);

        ui.runAccessTasks();

        Assert.assertEquals("Command should have been passed to UI.access",
                Arrays.asList("command"), executed);
    }

    @Test
    public void deactivatedContext_dispatchAction_actionsEnqueued() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        activationHandler.assertActive("Sanity check");
        ActionDispatcher dispatcher = activationHandler.getActionDispatcher();
        ui.remove(component);
        activationHandler.assertInactive("Sanity check");

        ArrayList<String> executed = new ArrayList<>();
        dispatcher.dispatchAction(() -> executed.add("foo"));
        dispatcher.dispatchAction(() -> executed.add("bar"));

        Assert.assertEquals("Dispatched action should not have been invoked",
                Collections.emptyList(), executed);

        ui.add(component);
        activationHandler.assertActive("Sanity check");

        Assert.assertEquals(
                "Dispatched action should have been invoked after activating",
                Arrays.asList("foo", "bar"), executed);
    }

    @Test
    public void dispatchAction_activateContext_dispatchedActionsRunInOrder() {
        ArrayList<String> actions = new ArrayList<>();

        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(dispatcher -> {
            activationHandler.accept(dispatcher);
        }, executor);
        activationHandler.assertActive("Should be active");
        ActionDispatcher dispatcher = activationHandler.getActionDispatcher();
        ui.remove(component);
        activationHandler.assertInactive("Should be inactive");
        dispatcher.dispatchAction(() -> actions.add("1"));
        dispatcher.dispatchAction(() -> actions.add("2"));

        Assert.assertTrue(actions.isEmpty());
        ui.add(component);
        dispatcher.dispatchAction(() -> actions.add("3"));
        Assert.assertEquals(Arrays.asList("1", "2", "3"), actions);
    }

    @Test
    public void deactivatedContext_dispatchActionInAction_activate_actionsExecuted() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        Assert.assertNull(activationHandler.getActionDispatcher());
        ArrayList<String> executed = new ArrayList<>();

        activationHandler.assertInactive("Should be inactive before add");
        ui.add(component);
        activationHandler.assertActive("Should be active after add");
        ActionDispatcher dispatcher = activationHandler.getActionDispatcher();
        Assert.assertNotNull(dispatcher);
        ui.remove(component);
        activationHandler.assertInactive("Should be inactive after remove");
        Command innerCommand = () -> executed.add("inner");
        Command outerCommand = () -> {
            executed.add("outer");
            dispatcher.dispatchAction(innerCommand);
        };
        dispatcher.dispatchAction(outerCommand);
        Assert.assertTrue(executed.isEmpty());
        ui.add(component);
        Assert.assertEquals(Arrays.asList("outer", "inner"), executed);
    }

    @Test(expected = IllegalStateException.class)
    public void activeContext_addComponentFromAnotherUi_throws() {
        ui.add(component);
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);

        UI ui2 = new UI();
        ui2.getInternals().setSession(ui.getSession());
        TestComponent c2 = new TestComponent();
        ui2.add(c2);

        context.addComponent(c2);
    }

    @Test
    public void activateTwoContexts_hasOneBeaconHandler() {
        ComponentConnectionContext context1 = new ComponentConnectionContext(
                component);
        Component component2 = new TestComponent();
        ComponentConnectionContext context2 = new ComponentConnectionContext(
                component2);
        ui.add(component);
        ui.add(component2);

        Assert.assertEquals(1, ui.getSession().getRequestHandlers().stream()
                .filter(BeaconHandler.class::isInstance).count());
    }

    @Test
    public void activateContext_triggerBeaconRequestHandling_deactivateConnectionAndRemoveBeaconListener() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        ui.add(component);
        activationHandler.assertActive(
                "Should be activated when the component is attached.");

        BeaconHandler beaconHandler = getBeaconHandler(ui);
        beaconHandler.synchronizedHandleRequest(null, null, null);

        Assert.assertTrue(beaconHandler.getListeners().isEmpty());
        activationHandler.assertInactive(
                "Should be deactivated when Beacon handler is triggered.");
    }

    @Test
    public void activateContext_deactivateContext_removeBeaconListener() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        ui.add(component);
        activationHandler.assertActive(
                "Should be activated when the component is attached.");
        ui.remove(component);
        Assert.assertTrue(getBeaconHandler(ui).getListeners().isEmpty());
    }

    @Test
    public void activateContext_moveComponentsToAnotherUI_triggerBeaconRequestHandling_shouldNotDeactivate() {
        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        ui.add(component);
        activationHandler.assertActive(
                "Should be activated when the component is attached.");

        ui.remove(component);
        component.getElement().getNode().removeFromTree();
        activationHandler.assertInactive(
                "Should be inactive when all components are detached.");
        new MockUI().add(component);

        getBeaconHandler(ui).synchronizedHandleRequest(null, null, null);
        activationHandler.assertActive(
                "BeaconHandler should not deactivate connection.");
    }

    @Test
    public void serviceDestroy_deactivatesConnection() throws ServiceException {
        VaadinService service = ui.getSession().getService();

        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        context.init(activationHandler, executor);
        ui.add(component);

        activationHandler.assertActive("Just clearing some internal flags :)");

        // Allow destroy event to be handled through session.access
        ui.getSession().unlock();

        service.destroy();

        activationHandler.assertInactive(
                "Connection should become inactive when service is destroyed");
    }

    @Test
    public void expiredSession_noLeaks() throws InterruptedException {
        // Must keep service as a local to prevent GCing the whole system
        VaadinService service = ui.getSession().getService();

        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        WeakReference<ComponentConnectionContext> contextRef = new WeakReference<>(
                context);
        context = null;

        ui.add(component);

        // Allow destroy event to be handled through session.access
        ui.getSession().unlock();

        service.fireSessionDestroy(ui.getSession());

        ui = null;
        component = null;

        Assert.assertTrue(TestUtils.isGarbageCollected(contextRef));
    }

    @Test
    public void noPushActive_actiavteThenAttach_pushActivated() {
        Assert.assertFalse("Sanity check",
                ui.getPushConfiguration().getPushMode().isEnabled());
        new ComponentConnectionContext(component).init(activationHandler,
                executor);
        ui.add(component);

        Assert.assertTrue("Push should be enabled",
                ui.getPushConfiguration().getPushMode().isEnabled());

    }

    @Test
    public void noPushActive_attachThenActivate_pushActivated() {
        Assert.assertFalse("Sanity check",
                ui.getPushConfiguration().getPushMode().isEnabled());

        ui.add(component);
        new ComponentConnectionContext(component).init(activationHandler,
                executor);

        Assert.assertTrue("Push should be enabled",
                ui.getPushConfiguration().getPushMode().isEnabled());

    }

    @Test
    public void specialPushActive_activateContext_pushNotChanged() {
        // Manual mode counts as enabled push
        ui.getPushConfiguration().setPushMode(PushMode.MANUAL);

        ComponentConnectionContext context = new ComponentConnectionContext(
                component);
        ui.add(component);
        context.init(activationHandler, executor);

        Assert.assertEquals("Push mode should remain same", PushMode.MANUAL,
                ui.getPushConfiguration().getPushMode());
    }

    @Test
    public void pollingActive_activateContext_pushNotActivated() {
        ui.setPollInterval(1);

        ui.add(component);
        new ComponentConnectionContext(component).init(activationHandler,
                executor);

        Assert.assertEquals("Push mode should remain disabled",
                PushMode.DISABLED, ui.getPushConfiguration().getPushMode());
    }

    @Test
    public void defaultConfig_activateContext_pushActivated() {
        CollaborationEngineConfiguration config = new CollaborationEngineConfiguration(
                licenceEvent -> {
                });

        // Stores itself for CollaborationEngine.getInstance(service)
        CollaborationEngine.configure(ui.getSession().getService(), config);

        ui.add(component);
        new ComponentConnectionContext(component).init(activationHandler,
                executor);

        Assert.assertEquals("Push mode should be activated", PushMode.AUTOMATIC,
                ui.getPushConfiguration().getPushMode());
    }

    @Test
    public void pushActivationDisabled_activateContext_pushNotActivated() {
        CollaborationEngineConfiguration config = new CollaborationEngineConfiguration(
                licenceEvent -> {
                });

        // Stores itself for CollaborationEngine.getInstance(service)
        CollaborationEngine.configure(ui.getSession().getService(), config);

        config.setAutomaticallyActivatePush(false);

        ui.add(component);
        new ComponentConnectionContext(component).init(activationHandler,
                executor);

        Assert.assertEquals("Push mode should remain disabled",
                PushMode.DISABLED, ui.getPushConfiguration().getPushMode());
    }

    @Test
    public void activeContext_deactivateByRegistration_cleanupListeners() {
        ui.add(component);
        Registration registration = new ComponentConnectionContext(component)
                .init(activationHandler, executor);
        activationHandler.assertActive("Context should be active");
        BeaconHandler beaconHandler = getBeaconHandler(ui);
        Assert.assertFalse(beaconHandler.getListeners().isEmpty());
        registration.remove();
        Assert.assertTrue("Should have been removed by registration.remove",
                beaconHandler.getListeners().isEmpty());
        activationHandler.assertInactive(
                "Should have been deactivated by registration.remove");
    }

    @Test
    public void connectionWithModifyingUnregistration_detachThenAttach_unregisterBeforeRegister() {
        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(ui.getSession().getService());

        List<String> log = createEagerMapEventCollector(ce, "topic", "map");

        ui.setExecuteAccessTasks(false);
        ui.add(component);

        ce.openTopicConnection(component, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    CollaborationMap map = connection.getNamedMap("map");
                    map.put("key", "active");
                    return () -> map.put("key", null);
                });

        ui.runAccessTasks();

        Assert.assertEquals("Santiy check", Arrays.asList("active"), log);

        ui.remove(component);
        ui.runAccessTasks();

        ui.add(component);
        ui.runAccessTasks();

        Assert.assertEquals(Arrays.asList("active", null, "active"), log);
    }

    @Test
    public void connectionWithModifyingUnregistration_immediateReattach_unregisterBeforeRegister() {
        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(ui.getSession().getService());

        List<String> log = createEagerMapEventCollector(ce, "topic", "map");

        ui.setExecuteAccessTasks(false);
        ui.add(component);

        ce.openTopicConnection(component, "topic", SystemUserInfo.getInstance(),
                connection -> {
                    CollaborationMap map = connection.getNamedMap("map");
                    map.put("key", "active");
                    return () -> map.put("key", null);
                });

        ui.runAccessTasks();

        Assert.assertEquals("Santiy check", Arrays.asList("active"), log);

        ui.remove(component);
        ui.add(component);
        ui.runAccessTasks();

        /*
         * There's only one activation entry in the list on the latest version
         * that includes the fix that skips deactivation is activation happens
         * quickly enough. For older versions, there would instead be three
         * entries: "active", null, "active"
         */
        Assert.assertEquals(Arrays.asList("active"), log);
    }

    private List<String> createEagerMapEventCollector(
            TestCollaborationEngine ce, String topicId, String mapName) {
        List<String> log = new ArrayList<>();
        MockConnectionContext spyContext = MockConnectionContext.createEager();
        spyContext.setExecutor(Runnable::run);
        ce.openTopicConnection(spyContext, topicId,
                SystemUserInfo.getInstance(), connection -> {
                    return connection.getNamedMap(mapName).subscribe(
                            event -> log.add(event.getValue(String.class)));
                });
        return log;
    }

    private static void ensureSingleThreadExecutorTasksAreRun(
            ExecutorService executorService)
            throws InterruptedException, ExecutionException {
        /*
         * The submitted task will be the last one in the executor's queue.
         * Assuming it's a single-threaded executor, that means that anything
         * else in that queue has already been run when our submitted task is
         * run. We do thus only have to block until our own task is run.
         */
        executorService.submit(() -> {
        }).get();
    }

    public BeaconHandler getBeaconHandler(MockUI mockUI) {
        return mockUI.getSession().getRequestHandlers().stream()
                .filter(BeaconHandler.class::isInstance)
                .map(BeaconHandler.class::cast).findFirst().get();
    }

}
