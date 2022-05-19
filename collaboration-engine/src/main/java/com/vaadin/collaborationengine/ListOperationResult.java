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
