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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.Topic.ChangeNotifier;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

/**
 * API for sending and subscribing to updates between clients collaborating on
 * the same collaboration topic.
 *
 * @author Vaadin Ltd
 */
public class TopicConnection {

    private final Topic topic;
    private final ConnectionContext context;
    private Registration closeRegistration;
    private final List<Registration> deactivateRegistrations = new ArrayList<>();
    private final Map<String, List<ChangeNotifier>> subscribersPerMap = new HashMap<>();

    TopicConnection(ConnectionContext context, Topic topic,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        this.topic = topic;
        this.context = context;

        closeRegistration = context.setActivationHandler(active -> {
            if (active) {
                synchronized (this.topic) {
                    Registration callbackRegistration = connectionActivationCallback
                            .apply(this);
                    addRegistration(callbackRegistration);

                    Registration changeRegistration = this.topic
                            .subscribe(this::handleChange);
                    addRegistration(changeRegistration);
                }
            } else {
                deactivate();
            }
        });
    }

    private void handleChange(MapChange change) {
        if (!subscribersPerMap.containsKey(change.getMapName())) {
            return;
        }
        for (ChangeNotifier notifier : new ArrayList<>(
                subscribersPerMap.get(change.getMapName()))) {
            notifier.onEntryChange(change);
        }
    }

    Topic getTopic() {
        return topic;
    }

    private void addRegistration(Registration registration) {
        if (registration != null) {
            deactivateRegistrations.add(registration);
        }
    }

    private Registration subscribeToMap(String mapName,
            ChangeNotifier changeNotifier) {
        subscribersPerMap.computeIfAbsent(mapName, key -> new ArrayList<>())
                .add(changeNotifier);
        return () -> unsubscribeFromMap(mapName, changeNotifier);
    }

    private void unsubscribeFromMap(String mapName,
            ChangeNotifier changeNotifier) {
        List<ChangeNotifier> notifiers = subscribersPerMap.get(mapName);
        if (notifiers == null) {
            return;
        }
        notifiers.remove(changeNotifier);
        if (notifiers.isEmpty()) {
            subscribersPerMap.remove(mapName);
        }
    }

    /**
     * Gets a collaboration map that can be used to track multiple values in a
     * single topic.
     *
     * @return the collaboration map, not <code>null</code>
     */
    public CollaborationMap getNamedMap(String name) {
        return new CollaborationMap() {
            @Override
            public Registration subscribe(MapSubscriber subscriber) {
                Objects.requireNonNull(subscriber, "Subscriber cannot be null");
                synchronized (topic) {
                    ChangeNotifier changeNotifier = mapChange -> {
                        MapChangeEvent event = new MapChangeEvent(this,
                                mapChange);
                        context.dispatchAction(
                                () -> subscriber.onMapChange(event));
                    };
                    topic.getMapData(name)
                            .forEach(changeNotifier::onEntryChange);

                    Registration registration = subscribeToMap(name,
                            changeNotifier);
                    addRegistration(registration);
                    return registration;
                }
            }

            @Override
            public boolean replace(String key, Object expectedValue,
                    Object newValue) {
                Objects.requireNonNull(key, "Key cannot be null");
                synchronized (topic) {
                    return topic.withMap(name, (map, changeListener) -> {

                        Object oldValue = map.get(key);
                        if (!Objects.equals(oldValue, expectedValue)) {
                            return Boolean.FALSE;
                        }

                        if (Objects.equals(oldValue, newValue)) {
                            return Boolean.TRUE;
                        }

                        updateMapValue(map, changeListener, key, newValue,
                                oldValue);

                        return Boolean.TRUE;
                    }).booleanValue();
                }
            }

            @Override
            public void put(String key, Object value) {
                Objects.requireNonNull(key, "Key cannot be null");
                synchronized (topic) {
                    topic.withMap(name, (map, changeListener) -> {

                        Object oldValue = map.get(key);
                        if (Objects.equals(oldValue, value)) {
                            return null;
                        }

                        updateMapValue(map, changeListener, key, value,
                                oldValue);

                        return null;
                    });
                }
            }

            private void updateMapValue(Map<String, Object> map,
                    ChangeNotifier changeNotifier, String key, Object newValue,
                    Object oldValue) {
                if (newValue == null) {
                    map.remove(key);
                } else {
                    map.put(key, newValue);
                }

                changeNotifier.onEntryChange(
                        new MapChange(name, key, oldValue, newValue));
            }

            @Override
            public Stream<String> getKeys() {
                synchronized (topic) {
                    return topic.withMap(name, (map, changeListener) -> {
                        ArrayList<String> snapshot = new ArrayList<>(
                                map.keySet());
                        return snapshot.stream();
                    });
                }
            }

            @Override
            public Object get(String key) {
                Objects.requireNonNull(key, "Key cannot be null");
                synchronized (topic) {
                    return topic.withMap(name,
                            (map, changeListener) -> map.get(key));
                }
            }

            @Override
            public TopicConnection getConnection() {
                return TopicConnection.this;
            }
        };
    }

    private void deactivate() {
        deactivateRegistrations.forEach(Registration::remove);
        deactivateRegistrations.clear();
    }

    void close() {
        deactivate();
        if (closeRegistration != null) {
            closeRegistration.remove();
            closeRegistration = null;
        }
    }

}
