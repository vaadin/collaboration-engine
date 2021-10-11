/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
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
