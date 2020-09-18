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
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

    private final Consumer<Boolean> topicActivationHandler;
    private final Map<String, List<ChangeNotifier>> subscribersPerMap = new HashMap<>();

    TopicConnection(ConnectionContext context, Topic topic,
            Consumer<Boolean> topicActivationHandler,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        this.topic = topic;
        this.context = context;
        this.topicActivationHandler = topicActivationHandler;

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
            topicActivationHandler.accept(active);
        });
    }

    private void handleChange(MapChange change) {
        try {
            EventUtil.fireEvents(subscribersPerMap.get(change.getMapName()),
                    notifier -> notifier.onEntryChange(change), false);
        } catch (RuntimeException e) {
            deactivateAndClose();
            throw e;
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
                    return topic.applyReplace(new ReplaceChange(name, key,
                            expectedValue, newValue));
                }
            }

            @Override
            public void put(String key, Object value) {
                Objects.requireNonNull(key, "Key cannot be null");
                synchronized (topic) {
                    topic.applyChange(new PutChange(name, key, value));
                }
            }

            @Override
            public Stream<String> getKeys() {
                synchronized (topic) {
                    List<String> snapshot = topic.getMapData(name)
                            .map(MapChange::getKey)
                            .collect(Collectors.toList());
                    return snapshot.stream();
                }
            }

            @Override
            public Object get(String key) {
                Objects.requireNonNull(key, "Key cannot be null");
                synchronized (topic) {
                    return topic.getMapValue(name, key);
                }
            }

            @Override
            public TopicConnection getConnection() {
                return TopicConnection.this;
            }
        };
    }

    private void deactivate() {
        try {
            EventUtil.fireEvents(deactivateRegistrations, Registration::remove,
                    false);
            deactivateRegistrations.clear();
        } catch (RuntimeException e) {
            closeWithoutDeactivating();
            throw e;
        }
    }

    void deactivateAndClose() {
        try {
            deactivate();
        } finally {
            closeWithoutDeactivating();
        }
    }

    void closeWithoutDeactivating() {
        try {
            if (closeRegistration != null) {
                closeRegistration.remove();
                closeRegistration = null;
            }
        } finally {
            topicActivationHandler.accept(false);
        }
    }

}
