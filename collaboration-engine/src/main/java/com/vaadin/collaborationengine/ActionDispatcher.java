/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.vaadin.flow.server.Command;

/**
 * Allows dispatching actions to be executed in background. The ActionDispatcher
 * is created by the ConnectionContext and passed to the
 * {@link ActivationHandler} in the
 * {@link ConnectionContext#init(ActivationHandler, Executor)} method.
 *
 * @author Vaadin Ltd
 */
public interface ActionDispatcher extends Serializable {

    /**
     * Dispatches the given action.
     *
     * @param action
     *            the action to be executed in background, not <code>null</code>
     *
     */
    void dispatchAction(Command action);

    /**
     * Gets a completable future that needs to be resolved by the caller.
     *
     * @return the {@link CompletableFuture} to be resolved
     *
     */
    <T> CompletableFuture<T> createCompletableFuture();
}
