/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
