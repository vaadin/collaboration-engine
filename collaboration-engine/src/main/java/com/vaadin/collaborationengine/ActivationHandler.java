/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.function.Consumer;

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
 * @since 1.0
 */
public interface ActivationHandler extends Consumer<ActionDispatcher> {

}
