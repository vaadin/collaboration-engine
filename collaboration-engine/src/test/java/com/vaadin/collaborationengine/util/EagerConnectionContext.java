package com.vaadin.collaborationengine.util;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

import java.util.concurrent.CompletableFuture;

public class EagerConnectionContext implements ConnectionContext {

    @Override
    public Registration setActivationHandler(ActivationHandler handler) {
        handler.setActive(true);
        return null;
    }

    @Override
    public void dispatchAction(Command action) {
        action.execute();
    }

    @Override
    public <T> CompletableFuture<T> createCompletableFuture() {
        return new CompletableFuture<>();
    }
}
