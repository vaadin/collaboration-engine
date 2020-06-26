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

import com.vaadin.flow.server.Command;

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
     */
    void setActivationHandler(ActivationHandler handler);

    /**
     * Dispatches the given action.
     *
     * @param action
     *            the action to be executed in the context
     */
    void dispatchAction(Command action);
}
