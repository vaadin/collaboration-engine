/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.collaborationengine.Topic.ListChangeNotifier;
import com.vaadin.collaborationengine.Topic.MapChangeNotifier;
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
    private final UserInfo localUser;
    private Registration closeRegistration;
    private final List<Registration> deactivateRegistrations = new ArrayList<>();

    private final Consumer<Boolean> topicActivationHandler;
    private final Map<String, List<MapChangeNotifier>> subscribersPerMap = new HashMap<>();
    private final Map<String, List<ListChangeNotifier>> subscribersPerList = new HashMap<>();

    private boolean active;

    TopicConnection(ConnectionContext context, Topic topic, UserInfo localUser,
            Consumer<Boolean> topicActivationHandler,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        this.topic = topic;
        this.context = context;
        this.localUser = localUser;
        this.topicActivationHandler = topicActivationHandler;

        closeRegistration = context.setActivationHandler(active -> {
            if (active) {
                this.active = true;
                context.dispatchAction(() -> {
                    Registration callbackRegistration = connectionActivationCallback
                            .apply(this);
                    addRegistration(callbackRegistration);

                    Registration mapChangeRegistration;
                    Registration listChangeRegistration;
                    synchronized (this.topic) {
                        mapChangeRegistration = this.topic
                                .subscribeToMapChange(this::handleMapChange);
                        listChangeRegistration = this.topic
                                .subscribeToListChange(this::handleListChange);
                    }
                    addRegistration(() -> {
                        synchronized (this.topic) {
                            mapChangeRegistration.remove();
                            listChangeRegistration.remove();
                        }
                    });
                });
            } else {
                deactivate();
            }
            topicActivationHandler.accept(active);
        });
    }

    private void handleMapChange(MapChange change) {
        try {
            EventUtil.fireEvents(subscribersPerMap.get(change.getMapName()),
                    notifier -> notifier.onEntryChange(change), false);
        } catch (RuntimeException e) {
            deactivateAndClose();
            throw e;
        }
    }

    private void handleListChange(ListChange change) {
        try {
            EventUtil.fireEvents(subscribersPerList.get(change.getListName()),
                    notifier -> notifier.onListChange(change), false);
        } catch (RuntimeException e) {
            deactivateAndClose();
            throw e;
        }
    }

    Topic getTopic() {
        return topic;
    }

    /**
     * Gets the user who is related to this topic connection.
     *
     * @return the related user, not {@code null}
     */
    public UserInfo getUserInfo() {
        return localUser;
    }

    private void addRegistration(Registration registration) {
        if (registration != null) {
            deactivateRegistrations.add(registration);
        }
    }

    private Registration subscribeToMap(String mapName,
            MapChangeNotifier mapChangeNotifier) {
        subscribersPerMap.computeIfAbsent(mapName, key -> new ArrayList<>())
                .add(mapChangeNotifier);
        return () -> unsubscribeFromMap(mapName, mapChangeNotifier);
    }

    private void unsubscribeFromMap(String mapName,
            MapChangeNotifier mapChangeNotifier) {
        List<MapChangeNotifier> notifiers = subscribersPerMap.get(mapName);
        if (notifiers == null) {
            return;
        }
        notifiers.remove(mapChangeNotifier);
        if (notifiers.isEmpty()) {
            subscribersPerMap.remove(mapName);
        }
    }

    private Registration subscribeToList(String listName,
            ListChangeNotifier changeNotifier) {
        subscribersPerList.computeIfAbsent(listName, key -> new ArrayList<>())
                .add(changeNotifier);
        return () -> unsubscribeFromList(listName, changeNotifier);
    }

    private void unsubscribeFromList(String listName,
            ListChangeNotifier changeNotifier) {
        List<ListChangeNotifier> notifiers = subscribersPerList.get(listName);
        if (notifiers == null) {
            return;
        }
        notifiers.remove(changeNotifier);
        if (notifiers.isEmpty()) {
            subscribersPerList.remove(listName);
        }
    }

    /**
     * Gets a collaboration map that can be used to track multiple values in a
     * single topic.
     *
     * @param name
     *            the name of the map
     * @return the collaboration map, not <code>null</code>
     */
    public CollaborationMap getNamedMap(String name) {
        ensureActiveConnection();
        return new CollaborationMap() {
            @Override
            public Registration subscribe(MapSubscriber subscriber) {
                ensureActiveConnection();
                Objects.requireNonNull(subscriber, "Subscriber cannot be null");

                synchronized (topic) {
                    MapChangeNotifier mapChangeNotifier = mapChange -> {
                        MapChangeEvent event = new MapChangeEvent(this,
                                mapChange);
                        context.dispatchAction(
                                () -> subscriber.onMapChange(event));
                    };
                    topic.getMapData(name)
                            .forEach(mapChangeNotifier::onEntryChange);

                    Registration registration = subscribeToMap(name,
                            mapChangeNotifier);
                    addRegistration(registration);
                    return registration;
                }
            }

            @Override
            public CompletableFuture<Boolean> replace(String key,
                    Object expectedValue, Object newValue) {
                ensureActiveConnection();
                Objects.requireNonNull(key, "Key cannot be null");

                CompletableFuture<Boolean> contextFuture = context
                        .createCompletableFuture();

                boolean isReplaced;
                synchronized (topic) {
                    isReplaced = topic.applyMapReplace(new ReplaceChange(name,
                            key, JsonUtil.toJsonNode(expectedValue),
                            JsonUtil.toJsonNode(newValue)));

                }
                context.dispatchAction(
                        () -> contextFuture.complete(isReplaced));
                return contextFuture;
            }

            @Override
            public CompletableFuture<Void> put(String key, Object value) {
                ensureActiveConnection();
                Objects.requireNonNull(key, "Key cannot be null");

                CompletableFuture<Void> contextFuture = context
                        .createCompletableFuture();

                synchronized (topic) {
                    topic.applyMapChange(new PutChange(name, key,
                            JsonUtil.toJsonNode(value)));
                }

                context.dispatchAction(() -> contextFuture.complete(null));
                return contextFuture;
            }

            @Override
            public Stream<String> getKeys() {
                ensureActiveConnection();
                synchronized (topic) {
                    List<String> snapshot = topic.getMapData(name)
                            .map(MapChange::getKey)
                            .collect(Collectors.toList());
                    return snapshot.stream();
                }
            }

            @Override
            public <T> T get(String key, Class<T> type) {
                return JsonUtil.toInstance(get(key), type);
            }

            @Override
            public <T> T get(String key, TypeReference<T> type) {
                return JsonUtil.toInstance(get(key), type);
            }

            private JsonNode get(String key) {
                ensureActiveConnection();
                Objects.requireNonNull(key, "Key cannot be null");

                synchronized (topic) {
                    return topic.getMapValue(name, key);
                }
            }

            @Override
            public TopicConnection getConnection() {
                return TopicConnection.this;
            }

            @Override
            public Optional<Duration> getExpirationTimeout() {
                Duration expirationTimeout = topic.expirationTimeouts.get(name);
                return Optional.ofNullable(expirationTimeout);
            }

            @Override
            public void setExpirationTimeout(Duration expirationTimeout) {
                if (expirationTimeout == null) {
                    topic.expirationTimeouts.remove(name);
                } else {
                    topic.expirationTimeouts.put(name, expirationTimeout);
                }
            }
        };
    }

    /**
     * Gets a collaboration list that can be used to track a list of items in a
     * single topic.
     *
     * @param name
     *            the name of the list
     * @return the collaboration list, not <code>null</code>
     */
    public CollaborationList getNamedList(String name) {
        ensureActiveConnection();
        return new CollaborationList() {
            @Override
            public Registration subscribe(ListSubscriber subscriber) {
                ensureActiveConnection();
                Objects.requireNonNull(subscriber, "Subscriber cannot be null");

                synchronized (topic) {
                    ListChangeNotifier changeNotifier = listChange -> {
                        ListChangeEvent event = new ListChangeEvent(this,
                                listChange);
                        context.dispatchAction(
                                () -> subscriber.onListChange(event));
                    };
                    topic.getListChanges(name)
                            .forEach(changeNotifier::onListChange);

                    Registration registration = subscribeToList(name,
                            changeNotifier);
                    addRegistration(registration);
                    return registration;
                }
            }

            @Override
            public <T> List<T> getItems(Class<T> type) {
                ensureActiveConnection();
                Objects.requireNonNull(type, "The type can't be null");
                synchronized (topic) {
                    return topic.getListItems(name)
                            .map(node -> JsonUtil.toInstance(node, type))
                            .collect(Collectors.toList());
                }
            }

            @Override
            public <T> List<T> getItems(TypeReference<T> type) {
                ensureActiveConnection();
                Objects.requireNonNull(type,
                        "The type reference cannot be null");
                synchronized (topic) {
                    return topic.getListItems(name)
                            .map(node -> JsonUtil.toInstance(node, type))
                            .collect(Collectors.toList());
                }
            }

            @Override
            public CompletableFuture<Void> append(Object item) {
                ensureActiveConnection();
                Objects.requireNonNull(item, "The item cannot be null");

                CompletableFuture<Void> contextFuture = context
                        .createCompletableFuture();

                synchronized (topic) {
                    JsonNode value = JsonUtil.toJsonNode(item);
                    topic.applyListChange(new ListChange(name, value));
                }

                context.dispatchAction(() -> contextFuture.complete(null));
                return contextFuture;
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
            this.active = false;
        }
    }

    private void ensureActiveConnection() {
        if (!active) {
            throw new IllegalStateException("Cannot perform this "
                    + "operation on a deactivated connection.");
        }
    }
}
