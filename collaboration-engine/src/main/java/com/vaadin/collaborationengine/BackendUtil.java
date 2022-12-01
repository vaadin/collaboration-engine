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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class BackendUtil {
    @FunctionalInterface
    public interface Initializer {
        CompletableFuture<UUID> initialize();
    }

    public static CompletableFuture<UUID> initializeFromSnapshot(
            CollaborationEngine ce, Initializer initializer) {
        CollaborationEngine.LOGGER.debug(
                "Attempting to initialize event log " + "from snapshot.");

        int maxAttempts = ce.getConfiguration()
                .getEventLogSubscribeRetryAttempts();
        CompletableFuture<UUID> future = new CompletableFuture<>();
        attemptInitialization(0, maxAttempts, initializer, future);
        return future;
    }

    private static void attemptInitialization(int attempt, int maxAttempts,
            Initializer initializer, CompletableFuture<UUID> future) {
        if (attempt < maxAttempts) {
            CompletableFuture<UUID> initFuture = initializer.initialize();
            initFuture.whenComplete((uuid, e) -> {
                if (e != null) {
                    CollaborationEngine.LOGGER.warn(
                            "Initialize event " + "log failed - retry attempt "
                                    + (attempt + 1) + "/" + maxAttempts + ".");
                    attemptInitialization(attempt + 1, maxAttempts, initializer,
                            future);
                } else {
                    future.complete(uuid);
                }
            });
        } else {
            CollaborationEngine.LOGGER.warn("Initialize event log abandoned "
                    + "after " + maxAttempts + " retries.");
            future.complete(null);
        }
    }
}
