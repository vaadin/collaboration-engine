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

    private final Map<String, Map<String, Object>> namedMapData = new HashMap<>();
    private final Map<String, List<MapChangeNotifier>> namedMapSubscribers = new HashMap<>();

    private final List<SingleValueSubscriber> subscribers = new LinkedList<>();

    Registration subscribe(SingleValueSubscriber subscriber) {
        synchronized (lock) {
            subscribers.add(subscriber);
            subscriber.onValueChange(singleValue);
        }

        return () -> unsubscribe(subscriber);
    }

    Registration subscribeMap(String name, MapChangeNotifier subscriber) {
        synchronized (lock) {
            namedMapSubscribers.computeIfAbsent(name, key -> new LinkedList<>())
                    .add(subscriber);

            if (namedMapData.containsKey(name)) {
                namedMapData.get(name).forEach((key, value) -> subscriber
                        .onMapChange(key, null, value));
            }
        }

        return () -> unsubscribeMap(name, subscriber);
    }

    void fireMapChangeEvent(String name, String key, Object oldValue,
            Object newValue) {
        if (!namedMapSubscribers.containsKey(name)) {
            return;
        }
        for (MapChangeNotifier subscriber : new ArrayList<>(
                namedMapSubscribers.get(name))) {
            subscriber.onMapChange(key, oldValue, newValue);
        }
    }

    private void unsubscribe(SingleValueSubscriber subscriber) {
        synchronized (lock) {
            subscribers.remove(subscriber);
        }
    }

    private void unsubscribeMap(String name, MapChangeNotifier subscriber) {
        synchronized (lock) {
            List<MapChangeNotifier> subscribers = namedMapSubscribers.get(name);
            if (subscribers == null) {
                return;
            }

            subscribers.remove(subscriber);
            if (subscribers.isEmpty()) {
                namedMapSubscribers.remove(name);
            }
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

    <T> T withMap(String name,
            SerializableBiFunction<Map<String, Object>, MapChangeNotifier, T> mapHandler) {
        synchronized (lock) {
            return mapHandler.apply(
                    namedMapData.computeIfAbsent(name, key -> new HashMap<>()),
                    (key, oldValue, newValue) -> fireMapChangeEvent(name, key,
                            oldValue, newValue));
        }
    }
}
