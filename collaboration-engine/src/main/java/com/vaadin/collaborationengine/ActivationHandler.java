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

/**
 * Defines when to execute the connection callback
 * <p>
 * A connection is not active when it's newly created. When being activated, the
 * activation callback of the connection should be invoked (or re-invoked when
 * re-activating the connection).
 * <p>
 * When being deactivated, the topic no longer has reference to the connection.
 *
 * @author Vaadin Ltd
 */
public interface ActivationHandler {

    /**
     * Activates or deactivates a connection
     *
     * @param active
     *            activate the connection if {@code true}, deactivate otherwise
     */
    void setActive(boolean active);
}
