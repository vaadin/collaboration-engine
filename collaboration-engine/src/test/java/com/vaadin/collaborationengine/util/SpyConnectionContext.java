package com.vaadin.collaborationengine.util;

import java.util.concurrent.CompletableFuture;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class SpyConnectionContext implements ConnectionContext {

    private ActivationHandler activationHandler;

    boolean closed = false;

    boolean throwOnClose = false;

    public ActivationHandler getActivationHandler() {
        return activationHandler;
    }

    @Override
    public Registration setActivationHandler(ActivationHandler handler) {
        this.activationHandler = handler;
        return () -> {
            closed = true;
            activationHandler = null;
            if (throwOnClose) {
                throw new FailOnPurposeException();
            }
        };
    }

    @Override
    public void dispatchAction(Command action) {
        action.execute();
    }

    @Override
    public <T> CompletableFuture<T> createCompletableFuture() {
        return new CompletableFuture<>();
    }

    public void activate() {
        activationHandler.setActive(true);
    }

    public void deactivate() {
        activationHandler.setActive(false);
    }

    public boolean isThrowOnClose() {
        return throwOnClose;
    }

    public void setThrowOnClose(boolean throwOnClose) {
        this.throwOnClose = throwOnClose;
    }

    public boolean isClosed() {
        return closed;
    }

    public static class FailOnPurposeException extends RuntimeException {
    }
}
