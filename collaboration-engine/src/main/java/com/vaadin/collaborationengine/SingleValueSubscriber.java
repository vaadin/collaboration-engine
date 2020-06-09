/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

/**
 * Subscriber that gets notified when the value of a collaboration topic
 * changes.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface SingleValueSubscriber {

    /**
     * Called when the value of a collaboration topic changes.
     *
     * @param value
     *            the new value of the collaboration topic
     */
    void onValueChange(Object value);
}
