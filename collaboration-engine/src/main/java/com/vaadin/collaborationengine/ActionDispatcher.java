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

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.server.Command;

/**
 * Allows dispatching actions to be executed in background. The ActionDispatcher
 * is created by the ConnectionContext and passed to the
 * {@link ActivationHandler} in the
 * {@link ConnectionContext#init(ActivationHandler, SerializableExecutor)}
 * method.
 *
 * @author Vaadin Ltd
 */
public interface ActionDispatcher extends Serializable {

    /**
     * Dispatches the given action.
     *
     * @param action
     *            the action to be executed in background, not <code>null</code>
     *
     */
    void dispatchAction(Command action);

    /**
     * Gets a completable future that needs to be resolved by the caller.
     *
     * @return the {@link CompletableFuture} to be resolved
     *
     */
    <T> CompletableFuture<T> createCompletableFuture();
}
