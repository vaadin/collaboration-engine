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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Vaadin Ltd
 * @since 1.0
 */
class EventUtil {
    private EventUtil() {
        // Only static helpers
    }

    /**
     * Runs the given action for each listener in a list and collects any
     * runtime exceptions. If any listener throws an exception, then the
     * exception is rethrown after iteration completes. If multiple exceptions
     * are thrown, then subsequent exceptions are set as suppressed exceptions
     * of the first exception which is thrown at the end.
     * <p>
     * Listeners for which an exception is thrown can optionally be removed from
     * the list of listeners.
     *
     * @param <T>
     *            the listener type
     * @param listeners
     *            the list of listeners, or <code>null</code> to gracefully not
     *            invoke any listeners
     * @param action
     *            the action to used to invoke the listener, not
     *            <code>null</code>
     * @param removeFailingListeners
     *            <code>true</code> to remove failing listeners from the list,
     *            <code>false</code> to not modify the list of listeners
     *
     * @since 1.0
     */
    static <T> void fireEvents(List<T> listeners, Consumer<T> action,
            boolean removeFailingListeners) {
        assert action != null;

        if (listeners == null) {
            return;
        }

        RuntimeException firstException = null;
        for (T listener : new ArrayList<>(listeners)) {
            try {
                action.accept(listener);
            } catch (RuntimeException e) {
                if (removeFailingListeners) {
                    listeners.remove(listener);
                }

                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }

        if (firstException != null) {
            throw firstException;
        }
    }
}
