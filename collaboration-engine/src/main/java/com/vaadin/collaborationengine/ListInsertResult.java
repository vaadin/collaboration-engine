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

/**
 * The result of a list operation in a {@link CollaborationList}.
 * <p>
 * It provides access to the key of the affected item and to the
 * {@link CompletableFuture} of the operation.
 *
 * @deprecated Replaced with {@link ListOperationResult}.
 *
 * @author Vaadin Ltd
 */
@Deprecated(since = "5.1", forRemoval = true)
public interface ListInsertResult<T> {
    /**
     * Gets the key of the item.
     *
     * @return the item key, not <code>null</code>
     */
    ListKey getKey();

    /**
     * The result of the asynchronous operation.
     *
     * @return the result of the operation, not <code>null</code>
     */
    CompletableFuture<T> getCompletableFuture();
}
