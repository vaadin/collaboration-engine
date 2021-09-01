package com.vaadin.collaborationengine.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class MockConnectionContext implements ConnectionContext {

    private ActivationHandler activationHandler;

    boolean closed = false;

    boolean throwOnClose = false;

    private boolean eager;

    private Consumer<Command> actionDispatcher = Command::execute;

    private AtomicInteger actionDispatchCount = new AtomicInteger();

    public ActivationHandler getActivationHandler() {
        return activationHandler;
    }

    public void setEager(boolean eager) {
        this.eager = eager;
    }

    public void setActionDispatcher(Consumer<Command> actionDispatcher) {
        this.actionDispatcher = actionDispatcher;
    }

    @Override
    public Registration setActivationHandler(ActivationHandler handler) {
        this.activationHandler = handler;
        if (eager) {
            activate();
        }
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
        actionDispatchCount.incrementAndGet();
        actionDispatcher.accept(action);
    }

    public int getDispathActionCount() {
        return actionDispatchCount.get();
    }

    public void resetActionDispatchCount() {
        actionDispatchCount.set(0);
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

    public static MockConnectionContext createEager() {
        MockConnectionContext context = new MockConnectionContext();
        context.setEager(true);
        return context;
    }
}
