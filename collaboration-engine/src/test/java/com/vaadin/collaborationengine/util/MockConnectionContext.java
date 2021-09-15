package com.vaadin.collaborationengine.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.collaborationengine.ActionDispatcher;
import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class MockConnectionContext implements ConnectionContext {

    private ActivationHandler activationHandler;

    boolean closed = false;

    boolean throwOnClose = false;

    private boolean eager;

    private ActionDispatcher actionDispatcher = new MockActionDispatcher();

    private Executor executor;

    private boolean executorExplicitlySet;

    private AtomicInteger actionDispatchCount = new AtomicInteger();

    public ActivationHandler getActivationHandler() {
        return activationHandler;
    }

    public void setEager(boolean eager) {
        this.eager = eager;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
        executorExplicitlySet = true;
    }

    @Override
    public Registration init(ActivationHandler activationHandler,
            Executor executor) {

        this.activationHandler = activationHandler;
        if (!executorExplicitlySet) {
            this.executor = executor;
        }
        if (eager) {
            activate();
        }
        return () -> {
            closed = true;
            if (throwOnClose) {
                throw new FailOnPurposeException();
            }
        };
    }

    public int getDispathActionCount() {
        return actionDispatchCount.get();
    }

    public void resetActionDispatchCount() {
        actionDispatchCount.set(0);
    }

    public void activate() {
        activationHandler.accept(actionDispatcher);
    }

    public void deactivate() {
        activationHandler.accept(null);
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

    public ActionDispatcher getActionDispatcher() {
        return actionDispatcher;
    }

    public static class FailOnPurposeException extends RuntimeException {
    }

    public static MockConnectionContext createEager() {
        MockConnectionContext context = new MockConnectionContext();
        context.setEager(true);
        return context;
    }

    public class MockActionDispatcher implements ActionDispatcher {

        @Override
        public <T> CompletableFuture<T> createCompletableFuture() {
            return new CompletableFuture<>();
        }

        @Override
        public void dispatchAction(Command action) {
            actionDispatchCount.incrementAndGet();
            executor.execute(action::execute);
        }

    }
}
