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
