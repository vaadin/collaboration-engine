/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DeadlockDetectingCompletableFuture;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * A connection context based on the attach state of a set of component
 * instances. The context is considered active whenever at least one tracked
 * component is attached. All attached components must belong to the same UI
 * instance, and this UI instance is used to dispatch actions using
 * {@link UI#access(Command)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentConnectionContext implements ConnectionContext {

    private final Map<Component, Registration> componentListeners = new HashMap<>();
    private final Set<Component> attachedComponents = new HashSet<>();

    private volatile UI ui;

    private final ConcurrentLinkedQueue<Command> inbox = new ConcurrentLinkedQueue<>();
    private final ActionDispatcher actionDispatcher = new ActionDispatcherImpl();
    private final AtomicBoolean active = new AtomicBoolean();
    private Consumer<ActionDispatcher> activationHandler;
    private Executor backgroundRunner;
    private Registration beaconListener;
    private Registration destroyListener;

    private static AtomicBoolean pushWarningShown = new AtomicBoolean(false);

    /**
     * Creates an empty component connection context.
     *
     * @since 1.0
     */
    public ComponentConnectionContext() {
        // Nothing to do here
    }

    /**
     * Creates a new component connection context which is initially using a
     * single component.
     *
     * @param component
     *            the component to use, not <code>null</code>
     *
     * @since 1.0
     */
    public ComponentConnectionContext(Component component) {
        addComponent(component);
    }

    /**
     * Adds a component instance to track for this context. Calling this method
     * again with a component that is already tracked has no effect.
     *
     * @param component
     *            the component to track, not <code>null</code>
     * @see #removeComponent(Component)
     *
     * @since 1.0
     */
    public void addComponent(Component component) {
        Objects.requireNonNull(component, "Component can't be null.");

        if (!componentListeners.containsKey(component)) {
            Registration attachRegistration = component.addAttachListener(
                    event -> markAsAttached(event.getUI(), event.getSource()));
            Registration detachRegistration = component.addDetachListener(
                    event -> markAsDetached(event.getSource()));

            componentListeners.put(component, Registration
                    .combine(attachRegistration, detachRegistration));

            component.getUI().ifPresent(
                    componentUi -> markAsAttached(componentUi, component));
        }
    }

    /**
     * Stops tracking a component for this context. Calling this method for a
     * component that isn't tracked has no effect.
     *
     * @param component
     *            the component to stop tracking, not <code>null</code>
     * @see #addComponent(Component)
     *
     * @since 1.0
     */
    public void removeComponent(Component component) {
        Objects.requireNonNull(component, "Component can't be null.");

        Registration registration = componentListeners.remove(component);
        if (registration != null) {
            registration.remove();
            markAsDetached(component);
        }
    }

    private void markAsAttached(UI componentUi, Component component) {
        if (attachedComponents.add(component)) {
            if (attachedComponents.size() == 1) {
                // First attach
                this.ui = componentUi;

                checkForPush(ui);

                BeaconHandler beaconHandler = BeaconHandler
                        .ensureInstalled(this.ui);
                beaconListener = beaconHandler
                        .addListener(this::deactivateConnection);

                ServiceDestroyDelegate destroyDelegate = ServiceDestroyDelegate
                        .ensureInstalled(this.ui);
                destroyListener = destroyDelegate
                        .addListener(event -> deactivateConnection());

                flushPendingActionsIfActive();

                if (activationHandler != null) {
                    activate();
                }

            } else if (componentUi != ui) {
                throw new IllegalStateException(
                        "All components in this connection context must be associated with the same UI.");
            }
        }
    }

    private void markAsDetached(Component component) {
        if (attachedComponents.remove(component)) {
            if (attachedComponents.isEmpty()) {
                // Last detach
                deactivateConnection();
            }
        }
    }

    @Override
    public Registration init(ActivationHandler activationHandler,
            Executor backgroundRunner) {
        if (this.activationHandler != null) {
            throw new IllegalStateException(
                    "This context has already been initialized");
        }
        this.activationHandler = Objects.requireNonNull(activationHandler,
                "Activation handler cannot be null");
        this.backgroundRunner = Objects.requireNonNull(backgroundRunner,
                "Background runner cannot be null");

        if (this.ui != null) {
            activate();
        }

        return () -> {
            // This instance won't be used again, release all references
            componentListeners.values().forEach(Registration::remove);
            componentListeners.clear();
            attachedComponents.clear();
            deactivateConnection();
        };
    }

    private void activate() {
        if (this.activationHandler != null && !this.active.getAndSet(true)) {
            this.activationHandler.accept(this.actionDispatcher);
        }
    }

    private void deactivateConnection() {
        if (beaconListener != null) {
            beaconListener.remove();
            beaconListener = null;
        }
        if (destroyListener != null) {
            destroyListener.remove();
            destroyListener = null;
        }
        if (activationHandler != null && ui != null
                && active.getAndSet(false)) {
            activationHandler.accept(null);
            actionDispatcher.dispatchAction(() -> ui = null);
        }
    }

    class ActionDispatcherImpl implements ActionDispatcher {
        /**
         * Executes the given action by holding the session lock. This is done
         * by using {@link UI#access(Command)} on the UI that the component(s)
         * associated with this context belong to. This ensures that any UI
         * changes are pushed to the client in real-time if {@link Push} is
         * enabled.
         * <p>
         * If this context is not active (none of the components are attached to
         * a UI), the action is postponed until the connection becomes active.
         *
         * @param action
         *            the action to dispatch
         */
        @Override
        public void dispatchAction(Command action) {
            inbox.add(action);
            flushPendingActionsIfActive();
        }

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            UI localUI = ComponentConnectionContext.this.ui;
            if (localUI == null) {
                throw new IllegalStateException(
                        "The topic connection within this context maybe deactivated."
                                + "Make sure the context has at least one component attached to the UI.");
            }
            return new DeadlockDetectingCompletableFuture<>(
                    localUI.getSession());
        }
    }

    private void flushPendingActionsIfActive() {
        UI localUI = this.ui;
        if (localUI == null || backgroundRunner == null) {
            return;
        }
        backgroundRunner.execute(() -> executePendingCommands(localUI));
    }

    private void executePendingCommands(UI localUI) {
        localUI.access(() -> {
            while (true) {
                Command command = inbox.poll();
                if (command == null) {
                    break;
                }
                command.execute();
            }
        });
    }

    private static void checkForPush(UI ui) {
        if (!canPushChanges(ui) && isActivationEnabled(ui)) {
            ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

            boolean warningAlreadyShown = pushWarningShown.getAndSet(true);
            if (!warningAlreadyShown) {
                int flowVersionInVaadin14 = 2;
                String annotationLocation = Version
                        .getMajorVersion() == flowVersionInVaadin14
                                ? "root layout or individual views"
                                : "AppShellConfigurator class";

                LoggerFactory.getLogger(ComponentConnectionContext.class).warn(
                        "Server push has been automatically enabled so updates can be shown immediately. "
                                + "Add @Push annotation on your "
                                + annotationLocation
                                + " to suppress this warning. "
                                + "Set automaticallyActivatePush to false in CollaborationEngineConfiguration if you want to ensure push is not automatically enabled.");
            }
        }
    }

    private static boolean isActivationEnabled(UI ui) {

        CollaborationEngine ce = CollaborationEngine
                .getInstance(ui.getSession().getService());

        return ce != null ? ce.getConfiguration().isAutomaticallyActivatePush()
                : CollaborationEngineConfiguration.DEFAULT_AUTOMATICALLY_ACTIVATE_PUSH;
    }

    private static boolean canPushChanges(UI ui) {
        return ui.getPushConfiguration().getPushMode().isEnabled()
                || ui.getPollInterval() > 0;
    }
}
