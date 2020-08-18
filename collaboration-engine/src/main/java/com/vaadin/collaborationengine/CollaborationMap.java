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

import java.util.stream.Stream;

import com.vaadin.flow.shared.Registration;

/**
 * A map that is shared between multiple users. Map instances can be retrieved
 * through a {@link TopicConnection}. Changes performed by one user will be
 * delivered as events to subscribers defined by other users.
 *
 * @author Vaadin Ltd
 */
public interface CollaborationMap {
    /**
     * Gets the current map value for the given key.
     *
     * @param key
     *            the string key for which to get a value, not <code>null</code>
     * @return the value associated with the key, or <code>null</code> if no
     *         value is present
     */
    Object get(String key);

    /**
     * Associates the given value with the given key. This method can also be
     * used to remove an association by passing <code>null</code> as the value.
     * Subscribers are notified if the new value isn't <code>equals()</code>
     * with the old value.
     *
     * @param key
     *            the string key for which to make an association, not
     *            <code>null</code>
     * @param value
     *            the value to set, or <code>null</code> to remove the
     *            association
     */
    void put(String key, Object value);

    /**
     * Atomically replaces the value for a key if and only if the current value
     * is as expected. Subscribers are notified if the new value isn't
     * <code>equals()</code> with the old value. <code>equals()</code> is also
     * used to compare the current value with the expected value.
     *
     * @param key
     *            the string key for which to make an association, not
     *            <code>null</code>
     * @param expectedValue
     *            the value to compare with the current value to determine
     *            whether to make an update, or <code>null</code> to expect that
     *            no value is present
     * @param newValue
     *            the new value to set, or <code>null</code> to remove the
     *            association
     * @return <code>true</code> if the expected value was present so that the
     *         operation could proceed; <code>false</code> if the expected value
     *         was not present
     */
    boolean replace(String key, Object expectedValue, Object newValue);

    /**
     * Gets a stream of the currently available keys. The stream is backed by a
     * current snapshot of the available keys and will thus not update even if
     * keys are added or removed before the stream is processed.
     *
     * @return the stream of keys, not <code>null</code>
     */
    Stream<String> getKeys();

    /**
     * Subscribes to changes to this map. When subscribing, the subscriber will
     * receive an event for each current value association.
     *
     * @param subscriber
     *            the subscriber to use, not <code>null</code>
     * @return a handle that can be used for removing the subscription, not
     *         <code>null</code>
     */
    Registration subscribe(MapSubscriber subscriber);

    /**
     * Gets the topic connection which is used to propagate changes to this map.
     *
     * @return the topic connection used by this map, not <code>null</code>
     */
    TopicConnection getConnection();
}
