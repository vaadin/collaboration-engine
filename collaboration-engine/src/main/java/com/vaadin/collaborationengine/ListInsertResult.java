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
 * The result of an insertion in a {@link CollaborationList}.
 * <p>
 * It provides access to the key of the inserted item and to the
 * {@link CompletableFuture} of the insert operation.
 *
 * @author Vaadin Ltd
 */
class ListInsertResult<T> {
    private final ListKey key;
    private final CompletableFuture<T> completableFuture;

    ListInsertResult(ListKey key, CompletableFuture<T> completableFuture) {
        this.key = key;
        this.completableFuture = completableFuture;
    }

    /**
     * Gets the key of the inserted item.
     *
     * @return the inserted item key
     */
    public ListKey getKey() {
        return key;
    }

    /**
     * The result of the asynchronous insert operation.
     *
     * @return the result of the insert operation
     */
    public CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }
}
