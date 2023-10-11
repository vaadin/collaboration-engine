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

import com.vaadin.flow.function.SerializableConsumer;

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
public interface ActivationHandler
        extends SerializableConsumer<ActionDispatcher> {

}
