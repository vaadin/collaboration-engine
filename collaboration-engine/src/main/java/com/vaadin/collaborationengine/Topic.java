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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.shared.Registration;

class Topic {

    @FunctionalInterface
    interface MapChangeNotifier {
        void onMapChange(String key, Object oldValue, Object newValue);
    }

    private final Object lock = new Object();

    private Object singleValue;

    private final Map<String, Object> map = new HashMap<>();

    private final List<SingleValueSubscriber> subscribers = new LinkedList<>();
    private final List<MapChangeNotifier> mapSubscribers = new LinkedList<>();

    Registration subscribe(SingleValueSubscriber subscriber) {
        synchronized (lock) {
            subscribers.add(subscriber);
            subscriber.onValueChange(singleValue);
        }

        return () -> unsubscribe(subscriber);
    }

    Registration subscribeMap(MapChangeNotifier subscriber) {
        synchronized (lock) {
            mapSubscribers.add(subscriber);

            map.forEach(
                    (key, value) -> subscriber.onMapChange(key, null, value));
        }

        return () -> unsubscribeMap(subscriber);

    }

    void fireMapChangeEvent(String key, Object oldValue, Object newValue) {
        for (MapChangeNotifier subscriber : new ArrayList<>(mapSubscribers)) {
            subscriber.onMapChange(key, oldValue, newValue);
        }
    }

    private void unsubscribe(SingleValueSubscriber subscriber) {
        synchronized (lock) {
            subscribers.remove(subscriber);
        }
    }

    private void unsubscribeMap(MapChangeNotifier subscriber) {
        synchronized (lock) {
            mapSubscribers.remove(subscriber);
        }
    }

    void setValue(Object value) {
        synchronized (lock) {
            this.singleValue = value;
            for (SingleValueSubscriber subscriber : new ArrayList<>(
                    subscribers)) {
                subscriber.onValueChange(value);
            }
        }
    }

    Object getValue() {
        return singleValue;
    }

    boolean compareAndSet(Object expected, Object update) {
        synchronized (lock) {
            if (Objects.equals(expected, singleValue)) {
                setValue(update);
                return true;
            } else {
                return false;
            }
        }
    }

    <T> T withMap(
            SerializableBiFunction<Map<String, Object>, MapChangeNotifier, T> mapHandler) {
        synchronized (lock) {
            return mapHandler.apply(map, this::fireMapChangeEvent);
        }
    }
}
