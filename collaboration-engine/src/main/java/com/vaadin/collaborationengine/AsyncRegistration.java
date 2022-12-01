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
