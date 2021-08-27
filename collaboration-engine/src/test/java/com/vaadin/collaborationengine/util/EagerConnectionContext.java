package com.vaadin.collaborationengine.util;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.flow.shared.Registration;

public class EagerConnectionContext extends SpyConnectionContext {

    @Override
    public Registration setActivationHandler(ActivationHandler handler) {
        Registration registration = super.setActivationHandler(handler);
        activate();
        return registration;
    }
}
