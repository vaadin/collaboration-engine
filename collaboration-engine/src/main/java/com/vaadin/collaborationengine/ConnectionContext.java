/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.concurrent.Executor;

import com.vaadin.flow.shared.Registration;

/**
 * Defining how a topic connection should handle incoming changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ConnectionContext {

    /**
     * Initializes the connection context with a {@link ActivationHandler} and
     * an {@link Executor}.
     * <p>
     * The method {@link ActivationHandler#accept(Object)} from the provided
     * {@link ActivationHandler} should be called with an
     * {@link ActionDispatcher} when this ConnectionContext is activated. When
     * this ConnectionContext is deactivated, it should call
     * {@link ActivationHandler#accept(Object)} with a null parameter.
     * <p>
     * The {@link ActionDispatcher} should ensure synchronization within the
     * context of this ConnectionContext.
     *
     * @param activationHandler
     *            the handler for activation changes
     * @param executor
     *            executor that should be used by the handler to execute
     *            background tasks. Not <code>null</code>
     * @return the registration for any logic that needs to be cleaned up if the
     *         connection is closed permanently, or <code>null</code> if there
     *         is nothing to clean up
     */
    Registration init(ActivationHandler activationHandler, Executor executor);

}
