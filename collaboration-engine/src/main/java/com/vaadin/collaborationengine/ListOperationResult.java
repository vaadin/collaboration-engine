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

import java.util.concurrent.CompletableFuture;

/**
 * The result of a list operation in a {@link CollaborationList}.
 * <p>
 * It provides access to the key of the affected item and to the
 * {@link CompletableFuture} of the operation.
 *
 * @author Vaadin Ltd
 */
public class ListOperationResult<T> {
    private final ListKey key;
    private final CompletableFuture<T> completableFuture;

    ListOperationResult(ListKey key, CompletableFuture<T> completableFuture) {
        this.key = key;
        this.completableFuture = completableFuture;
    }

    /**
     * Gets the key of the item.
     *
     * @return the item key, not <code>null</code>
     */
    public ListKey getKey() {
        return key;
    }

    /**
     * The result of the asynchronous operation.
     *
     * @return the result of the operation, not <code>null</code>
     */
    public CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }

    /* Map to a void parameterized type for existing list operations */
    ListOperationResult<Void> mapToVoid() {
        return new ListOperationResult<>(key,
                completableFuture.thenApply(t -> null));
    }
}
