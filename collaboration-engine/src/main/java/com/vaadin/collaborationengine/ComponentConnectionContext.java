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

import com.vaadin.flow.shared.Registration;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;

/**
 * A built-in implementation of {@link ConnectionContext} to dispatch an action
 * in the context of a component.
 *
 * @author Vaadin Ltd
 */
public class ComponentConnectionContext implements ConnectionContext {

    private volatile UI ui;
    private final Component component;

    /**
     * Creates a new {@link ConnectionContext} from a component.
     *
     * @param component
     *            the component which holds the context to execute an action,
     *            not {@code null}
     */
    public ComponentConnectionContext(Component component) {
        Objects.requireNonNull(component, "Component can't be null.");
        this.component = component;
    }

    @Override
    public Registration setActivationHandler(ActivationHandler handler) {
        Registration attachReg = component.addAttachListener(event -> {
            ui = event.getUI();
            handler.setActive(true);
        });
        Registration detachReg = component.addDetachListener(event -> {
            handler.setActive(false);
            ui = null;
        });

        component.getUI().ifPresent(componentUI -> {
            this.ui = componentUI;
            handler.setActive(true);
        });

        return Registration.combine(attachReg, detachReg);
    }

    @Override
    public void dispatchAction(Command action) {
        UI localUI = this.ui;
        if (localUI != null) {
            localUI.access(action);
        }
    }
}
