/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.shared.Registration;

class AsyncRegistration implements Registration {

    private final transient CompletableFuture<Void> future;
    private final Registration registration;

    AsyncRegistration(CompletableFuture<Void> future,
            Registration registration) {
        this.future = future;
        this.registration = registration;
    }

    CompletableFuture<Void> getFuture() {
        return future;
    }

    @Override
    public void remove() {
        registration.remove();
    }
}
