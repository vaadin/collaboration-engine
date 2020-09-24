/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * Defining how a topic connection should handle incoming changes.
 *
 * @author Vaadin Ltd
 */
public interface ConnectionContext {

    /**
     * Sets an instance of {@link ActivationHandler} to the current context.
     * This method should include logic for updating the activation status.
     *
     * @param handler
     *            the handler for activation changes
     * @return the registration for any logic that needs to be cleaned up if the
     *         connection is closed permanently, or <code>null</code> if there
     *         is nothing to clean up
     */
    Registration setActivationHandler(ActivationHandler handler);

    /**
     * Dispatches the given action.
     *
     * @param action
     *            the action to be executed in the context
     */
    void dispatchAction(Command action);

    /**
     * Gets a completable future that needs to be resolved within the current
     * context.
     *
     * @return the {@link CompletableFuture} to be resolved
     */
    <T> CompletableFuture<T> createCompletableFuture();
}
