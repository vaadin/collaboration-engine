/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 * This program is available under Vaadin Commercial License and Service Terms.
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

/**
 * A connection context that is always active. This context is intended to be
 * used in cases when Collaboration Engine is used in situations that aren't
 * directly associated with a UI, such as from a background thread or when
 * integrating with external services.
 * <p>
 * An instance can be acquired using {@link #getInstance()} in any situation
 * where {@link CollaborationEngine#getInstance()} is available. Other
 * situations can use {@link CollaborationEngine#getSystemContext()} or create a
 * new context instance using the constructor.
 *
 * @author Vaadin Ltd
 */
public class SystemConnectionContext implements ConnectionContext {

    private final class ActionDispatcherImplementation
            implements ActionDispatcher {
        private final Executor executor;
        private final ExecutionQueue inbox = new ExecutionQueue();
        private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
        private boolean shuttingDown;

        private ActionDispatcherImplementation(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void dispatchAction(Command action) {
            inbox.add(() -> {
                Map<Class<?>, CurrentInstance> oldInstances = CurrentInstance
                        .getInstances();
                try {
                    VaadinService.setCurrent(
                            getCollaborationEngine().getVaadinService());
                    action.execute();
                } finally {
                    CurrentInstance.restoreInstances(oldInstances);
                }
            });
            executor.execute(() -> {
                synchronized (this) {
                    inbox.runPendingCommands();
                    if (shuttingDown) {
                        shutdownFuture.complete(null);
                        shuttingDown = false;
                    }
                }
            });
        }

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            return new CompletableFuture<>();
        }

        synchronized private void shutdown() {
            if (inbox.isEmpty()) {
                shutdownFuture.complete(null);
            } else {
                shuttingDown = true;
            }
        }
    }

    private final SerializableSupplier<CollaborationEngine> ceSupplier;

    // Active handlers to deactivate if the service is destroyed
    private final Set<ActivationHandler> activeHandlers = new HashSet<>();

    private Registration serviceDestroyRegistration;

    /**
     * Creates a new system connection context instance for the given
     * Collaboration Engine instance. It is typically recommended reusing an
     * existing instance through {@link #getInstance()} or
     * {@link CollaborationEngine#getSystemContext()} rather than creating new
     * instances.
     *
     * @param ceSupplier
     *            the collaboration engine instance to use, not
     *            <code>null</code>
     */
    public SystemConnectionContext(
            SerializableSupplier<CollaborationEngine> ceSupplier) {
        this.ceSupplier = Objects.requireNonNull(ceSupplier);
    }

    /**
     * Gets the system connection context associated with the current
     * Collaboration Engine instance. This method can be used only when
     * {@link CollaborationEngine#getInstance()} is available.
     *
     * @return a system connection context instance, not <code>null</code>
     * @throws IllegalStateException
     *             in case no current collaboration engine instance is available
     */
    public static SystemConnectionContext getInstance() {
        CollaborationEngine ce = CollaborationEngine.getInstance();
        if (ce == null) {
            throw new IllegalStateException(
                    "This method cannot be used when CollaborationEngine has not been configured for the current VaadinService.");
        }
        return ce.getSystemContext();
    }

    @Override
    public Registration init(ActivationHandler activationHandler,
            Executor executor) {
        Objects.requireNonNull(activationHandler);
        Objects.requireNonNull(executor);

        synchronized (activeHandlers) {
            if (activeHandlers.isEmpty()) {
                serviceDestroyRegistration = ceSupplier.get().getVaadinService()
                        .addServiceDestroyListener(e -> {
                            synchronized (activeHandlers) {
                                activeHandlers.forEach(
                                        handler -> handler.accept(null));
                                activeHandlers.clear();
                            }
                        });
            }

            if (!activeHandlers.add(activationHandler)) {
                throw new IllegalStateException(
                        "The provided activation handler was already active");
            }

            ActionDispatcherImplementation actionDispatcher = new ActionDispatcherImplementation(
                    executor);
            activationHandler.accept(actionDispatcher);

            return new AsyncRegistration(actionDispatcher.shutdownFuture,
                    () -> {
                        synchronized (activeHandlers) {
                            if (activeHandlers.remove(activationHandler)) {
                                activationHandler.accept(null);
                                actionDispatcher.shutdown();

                                if (activeHandlers.isEmpty()) {
                                    serviceDestroyRegistration.remove();
                                    serviceDestroyRegistration = null;
                                }
                            }
                        }
                    });
        }
    }

    // For testing
    CollaborationEngine getCollaborationEngine() {
        return ceSupplier.get();
    }
}
