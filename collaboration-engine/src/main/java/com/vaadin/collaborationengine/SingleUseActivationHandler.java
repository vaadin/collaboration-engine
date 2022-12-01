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

class SingleUseActivationHandler implements ActivationHandler {
    private ActivationHandler activationHandler;

    SingleUseActivationHandler(ActivationHandler activationHandler) {
        this.activationHandler = activationHandler;
    }

    @Override
    public void accept(ActionDispatcher actionDispatcher) {
        if (this.activationHandler == null) {
            return;
        }
        this.activationHandler.accept(actionDispatcher);
        this.activationHandler = null;
    }
}
