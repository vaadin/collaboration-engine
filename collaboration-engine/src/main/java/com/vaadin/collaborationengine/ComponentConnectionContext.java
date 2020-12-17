/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DeadlockDetectingCompletableFuture;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * A connection context based on the attach state of a set of component
 * instances. The context is considered active whenever at least one tracked
 * component is attached. All attached components must belong to the same UI
 * instance, and this UI instance is used to dispatch actions using
 * {@link UI#access(Command)}.
 *
 * @author Vaadin Ltd
 */
public class ComponentConnectionContext implements ConnectionContext {

    private final Map<Component, Registration> componentListeners = new HashMap<>();
    private final Set<Component> attachedComponents = new HashSet<>();

    private volatile UI ui;

    private List<Command> pendingActions = new ArrayList<>();

    private ActivationHandler activationHandler;
    private Registration beaconListener;
    private Registration destroyListener;

    private static AtomicBoolean pushCheckDone = new AtomicBoolean(false);

    /**
     * Creates an empty component connection context.
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
                    activationHandler.setActive(true);
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
                ui = null;
            }
        }
    }

    @Override
    public Registration setActivationHandler(
            ActivationHandler activationHandler) {
        if (this.activationHandler != null) {
            throw new IllegalStateException(
                    "An activation handler has already been set for this context");
        }
        this.activationHandler = Objects.requireNonNull(activationHandler,
                "Activation handler cannot be null");

        if (this.ui != null) {
            activationHandler.setActive(true);
        }

        return () -> {
            // This instance won't be used again, release all references
            componentListeners.values().forEach(Registration::remove);
            componentListeners.clear();
            attachedComponents.clear();
            ui = null;
        };
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
        if (activationHandler != null) {
            activationHandler.setActive(false);
        }
    }

    /**
     * Executes the given action by holding the session lock. This is done by
     * using {@link UI#access(Command)} on the UI that the component(s)
     * associated with this context belong to. This ensures that any UI changes
     * are pushed to the client in real-time if {@link Push} is enabled.
     * <p>
     * If this context is not active (none of the components are attached to a
     * UI), the action is postponed until the connection becomes active.
     *
     * @param action
     *            the action to dispatch
     */
    @Override
    public void dispatchAction(Command action) {
        synchronized (pendingActions) {
            pendingActions.add(action);
        }
        flushPendingActionsIfActive();
    }

    @Override
    public <T> CompletableFuture<T> createCompletableFuture() {
        UI localUI = this.ui;
        if (localUI == null) {
            throw new IllegalStateException(
                    "The topic connection within this context maybe deactivated."
                            + "Make sure the context has at least one component attached to the UI.");
        }
        return new DeadlockDetectingCompletableFuture<>(localUI.getSession());
    }

    private void flushPendingActionsIfActive() {
        UI localUI = this.ui;
        if (localUI != null) {
            localUI.access(() -> {
                List<Command> pendingActionsCopy;
                synchronized (pendingActions) {
                    pendingActionsCopy = new ArrayList<>(pendingActions);
                    pendingActions.clear();
                }
                pendingActionsCopy.forEach(Command::execute);
            });
        }
    }

    private void checkForPush(UI ui) {
        boolean checkedBefore = pushCheckDone.getAndSet(true);
        if (!checkedBefore
                && !ui.getSession().getConfiguration().isProductionMode()
                && !ui.getPushConfiguration().getPushMode().isEnabled()) {
            LoggerFactory.getLogger(ComponentConnectionContext.class).warn(
                    "Collaboration Engine is used without server push, so updates can't be propagated in real time. "
                            + "Add @Push annotation on your root layout or individual views to fix this.");
        }
    }
}
