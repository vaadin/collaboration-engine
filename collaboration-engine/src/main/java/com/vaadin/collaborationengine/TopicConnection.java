/*
 * Copyright (C) 2021 Vaadin Ltd
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.collaborationengine.Topic.ChangeDetails;
import com.vaadin.collaborationengine.Topic.ChangeResult;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

/**
 * API for sending and subscribing to updates between clients collaborating on
 * the same collaboration topic.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TopicConnection {

    class CollaborationMapImplementation implements CollaborationMap {
        private final String name;

        private CollaborationMapImplementation(String name) {
            this.name = name;
        }

        @Override
        public Registration subscribe(MapSubscriber subscriber) {
            ensureActiveConnection();
            Objects.requireNonNull(subscriber, "Subscriber cannot be null");

            synchronized (topic) {
                Consumer<MapChange> mapChangeNotifier = mapChange -> {
                    MapChangeEvent event = new MapChangeEvent(this, mapChange);
                    actionDispatcher.dispatchAction(
                            () -> subscriber.onMapChange(event));
                };
                topic.getMapData(name).forEach(mapChangeNotifier);

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
            Objects.requireNonNull(key, MessageUtil.Required.KEY);

            CompletableFuture<Boolean> contextFuture = actionDispatcher
                    .createCompletableFuture();

            ObjectNode change = JsonUtil.createPutChange(name, key,
                    expectedValue, newValue);

            UUID id = UUID.randomUUID();
            topic.setChangeResultTracker(id, result -> {
                boolean isApplied = result != Topic.ChangeResult.REJECTED;
                actionDispatcher.dispatchAction(
                        () -> contextFuture.complete(isApplied));
            });
            distributor.accept(id, change);
            return contextFuture;
        }

        @Override
        public CompletableFuture<Void> put(String key, Object value,
                EntryScope scope) {
            ensureActiveConnection();
            Objects.requireNonNull(key, MessageUtil.Required.KEY);

            CompletableFuture<Void> contextFuture = actionDispatcher
                    .createCompletableFuture();

            ObjectNode change = JsonUtil.createPutChange(name, key, null,
                    value);

            UUID id = UUID.randomUUID();
            topic.setChangeResultTracker(id, result -> {
                if (scope == EntryScope.CONNECTION
                        && result == ChangeResult.ACCEPTED) {
                    connectionScopedMapKeys
                            .computeIfAbsent(name, k -> new HashMap<>())
                            .put(key, id);
                    if (!cleanupPending) {
                        cleanupScopedData();
                    }
                }
                actionDispatcher
                        .dispatchAction(() -> contextFuture.complete(null));
            });
            distributor.accept(id, change);
            return contextFuture;
        }

        @Override
        public Stream<String> getKeys() {
            ensureActiveConnection();
            synchronized (topic) {
                List<String> snapshot = topic.getMapData(name)
                        .map(MapChange::getKey).collect(Collectors.toList());
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
            Objects.requireNonNull(key, MessageUtil.Required.KEY);

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
            Duration expirationTimeout = topic.mapExpirationTimeouts.get(name);
            return Optional.ofNullable(expirationTimeout);
        }

        @Override
        public void setExpirationTimeout(Duration expirationTimeout) {
            if (expirationTimeout == null) {
                topic.mapExpirationTimeouts.remove(name);
            } else {
                topic.mapExpirationTimeouts.put(name, expirationTimeout);
            }
        }

        private Registration subscribeToMap(String mapName,
                Consumer<MapChange> mapChangeNotifier) {
            subscribersPerMap.computeIfAbsent(mapName, key -> new ArrayList<>())
                    .add(mapChangeNotifier);
            return () -> unsubscribeFromMap(mapName, mapChangeNotifier);
        }

        private void unsubscribeFromMap(String mapName,
                Consumer<MapChange> mapChangeNotifier) {
            List<Consumer<MapChange>> notifiers = subscribersPerMap
                    .get(mapName);
            if (notifiers == null) {
                return;
            }
            notifiers.remove(mapChangeNotifier);
            if (notifiers.isEmpty()) {
                subscribersPerMap.remove(mapName);
            }
        }
    }

    class CollaborationListImplementation implements CollaborationList {
        private final String name;

        private CollaborationListImplementation(String name) {
            this.name = name;
        }

        @Override
        public Registration subscribe(ListSubscriber subscriber) {
            ensureActiveConnection();
            Objects.requireNonNull(subscriber, "Subscriber cannot be null");

            synchronized (topic) {
                Consumer<ListChange> changeNotifier = listChange -> {
                    ListChangeEvent event = new ListChangeEvent(this,
                            listChange);
                    actionDispatcher.dispatchAction(
                            () -> subscriber.onListChange(event));
                };
                topic.getListChanges(name).forEach(changeNotifier);

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
            Objects.requireNonNull(type, "The type reference cannot be null");
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

            CompletableFuture<Void> contextFuture = actionDispatcher
                    .createCompletableFuture();

            ObjectNode change = JsonUtil.createAppendChange(name, item);

            UUID id = UUID.randomUUID();
            topic.setChangeResultTracker(id, result -> {
                actionDispatcher
                        .dispatchAction(() -> contextFuture.complete(null));
            });
            distributor.accept(id, change);
            return contextFuture;
        }

        @Override
        public TopicConnection getConnection() {
            return TopicConnection.this;
        }

        @Override
        public Optional<Duration> getExpirationTimeout() {
            Duration expirationTimeout = topic.listExpirationTimeouts.get(name);
            return Optional.ofNullable(expirationTimeout);
        }

        @Override
        public void setExpirationTimeout(Duration expirationTimeout) {
            if (expirationTimeout == null) {
                topic.listExpirationTimeouts.remove(name);
            } else {
                topic.listExpirationTimeouts.put(name, expirationTimeout);
            }
        }

        private Registration subscribeToList(String listName,
                Consumer<ListChange> changeNotifier) {
            subscribersPerList
                    .computeIfAbsent(listName, key -> new ArrayList<>())
                    .add(changeNotifier);
            return () -> unsubscribeFromList(listName, changeNotifier);
        }

        private void unsubscribeFromList(String listName,
                Consumer<ListChange> changeNotifier) {
            List<Consumer<ListChange>> notifiers = subscribersPerList
                    .get(listName);
            if (notifiers == null) {
                return;
            }
            notifiers.remove(changeNotifier);
            if (notifiers.isEmpty()) {
                subscribersPerList.remove(listName);
            }
        }
    }

    private final Topic topic;
    private final UserInfo localUser;
    private final List<Registration> deactivateRegistrations = new ArrayList<>();
    private final Consumer<Boolean> topicActivationHandler;
    private final Map<String, List<Consumer<MapChange>>> subscribersPerMap = new HashMap<>();
    private final Map<String, List<Consumer<ListChange>>> subscribersPerList = new HashMap<>();
    private final Map<String, Map<String, UUID>> connectionScopedMapKeys = new HashMap<>();

    private volatile boolean cleanupPending;

    private final BiConsumer<UUID, ObjectNode> distributor;
    private final SerializableFunction<TopicConnection, Registration> connectionActivationCallback;
    private Registration closeRegistration;
    private ActionDispatcher actionDispatcher;
    /**
     * Whether activation has happened, which isn't necessarily the same as
     * being active because of the asynchronous step before activation is
     * actually applied.
     */
    private boolean activated;

    TopicConnection(CollaborationEngine ce, ConnectionContext context,
            Topic topic, BiConsumer<UUID, ObjectNode> distributor,
            UserInfo localUser, Consumer<Boolean> topicActivationHandler,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        this.topic = topic;
        this.distributor = distributor;
        this.localUser = localUser;
        this.topicActivationHandler = topicActivationHandler;
        this.connectionActivationCallback = connectionActivationCallback;
        this.closeRegistration = context.init(this::acceptActionDispatcher,
                ce.getExecutorService());
    }

    private void handleChange(UUID id, ChangeDetails change) {
        try {
            if (change instanceof MapChange) {
                handlePutChange(id, (MapChange) change);
            } else if (change instanceof ListChange) {
                handleAppendChange((ListChange) change);
            } else {
                throw new UnsupportedOperationException(
                        "Type '" + change.getClass().getName()
                                + "' is not a supported change type");
            }
        } catch (RuntimeException e) {
            deactivateAndClose();
            throw e;
        }
    }

    private void handlePutChange(UUID id, MapChange mapChange) {
        String mapName = mapChange.getMapName();
        String key = mapChange.getKey();

        // If there is a connection scoped entry for the same key with a
        // different id, cleanup the existing entry
        Map<String, UUID> keys = connectionScopedMapKeys.get(mapName);
        if (keys != null && !Objects.equals(id, keys.get(key))) {
            UUID uuid = keys.get(key);
            if (!Objects.equals(mapChange.getExpectedId(), uuid)) {
                keys.remove(key);
            }
        }
        if (mapChange.hasChanges()) {
            EventUtil.fireEvents(subscribersPerMap.get(mapName),
                    notifier -> notifier.accept(mapChange), false);
        }
    }

    private void handleAppendChange(ListChange listChange) {
        EventUtil.fireEvents(subscribersPerList.get(listChange.getListName()),
                notifier -> notifier.accept(listChange), false);
    }

    Topic getTopic() {
        return topic;
    }

    /**
     * Gets the user who is related to this topic connection.
     *
     * @return the related user, not {@code null}
     *
     * @since 1.0
     */
    public UserInfo getUserInfo() {
        return localUser;
    }

    private boolean isActive() {
        return this.actionDispatcher != null;
    }

    private void addRegistration(Registration registration) {
        if (registration != null) {
            deactivateRegistrations.add(registration);
        }
    }

    /**
     * Gets a collaboration map that can be used to track multiple values in a
     * single topic.
     *
     * @param name
     *            the name of the map
     * @return the collaboration map, not <code>null</code>
     *
     * @since 1.0
     */
    public CollaborationMap getNamedMap(String name) {
        ensureActiveConnection();
        return new CollaborationMapImplementation(name);
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
        return new CollaborationListImplementation(name);
    }

    private void deactivate() {
        try {
            cleanupScopedData();
            EventUtil.fireEvents(deactivateRegistrations, Registration::remove,
                    false);
            deactivateRegistrations.clear();
        } catch (RuntimeException e) {
            closeWithoutDeactivating();
            throw e;
        }
    }

    private void cleanupScopedData() {
        synchronized (topic) {
            connectionScopedMapKeys.forEach(
                    (mapName, mapKeys) -> mapKeys.forEach((key, id) -> {
                        ObjectNode change = JsonUtil.createPutChange(mapName,
                                key, null, null);
                        change.put(JsonUtil.CHANGE_EXPECTED_ID, id.toString());
                        distributor.accept(UUID.randomUUID(), change);
                    }));
            connectionScopedMapKeys.clear();
            cleanupPending = false;
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
        if (closeRegistration != null) {
            try {
                closeRegistration.remove();
            } finally {
                closeRegistration = null;
                if (actionDispatcher != null) {
                    this.topicActivationHandler.accept(false);
                    this.actionDispatcher = null;
                }
            }
        }
    }

    private void ensureActiveConnection() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot perform this "
                    + "operation on a deactivated connection.");
        }
    }

    private void acceptActionDispatcher(ActionDispatcher actionDispatcher) {
        if (actionDispatcher != null) {
            if (activated) {
                throw new IllegalStateException(
                        "The topic connection is already active.");
            }
            activated = true;

            if (this.actionDispatcher != null) {
                /*
                 * Activation while already active based on non-null dispatcher
                 * means that deactivation has been triggered but its dispatch
                 * has not yet run. The dispatch will be ignored based on the
                 * flag which means that there's also no need to activate things
                 * again.
                 */
                return;
            }

            actionDispatcher.dispatchAction(() -> {
                if (!activated) {
                    /*
                     * Activation canceled while waiting for dispatch.
                     */
                    return;
                }
                if (this.actionDispatcher != null) {
                    throw new IllegalStateException(
                            "Activation dispatch is run out-of-order.");
                }

                this.actionDispatcher = actionDispatcher;
                cleanupPending = true;
                topicActivationHandler.accept(true);
                Registration changeRegistration = subscribeToChange();
                Registration callbackRegistration = connectionActivationCallback
                        .apply(this);
                addRegistration(callbackRegistration);
                addRegistration(() -> {
                    synchronized (topic) {
                        changeRegistration.remove();
                    }
                });
            });
        } else {
            if (!activated) {
                throw new IllegalStateException(
                        "The topic connection is already inactive.");
            }
            activated = false;
            if (this.actionDispatcher == null) {
                /*
                 * Deactivation while already inactive based on null dispatcher
                 * means that activation has been triggered but its dispatch has
                 * not yet run. The dispatch will be ignored based on the flag
                 * which means that there's also no need to deactivate things
                 * again.
                 */
                return;
            }
            this.actionDispatcher.dispatchAction(() -> {
                if (activated) {
                    /*
                     * Deactivation canceled while waiting for dispatch.
                     */
                    return;
                }
                if (this.actionDispatcher == null) {
                    throw new IllegalStateException(
                            "Deactivation dispatch is run out-of-order.");
                }

                try {
                    this.deactivate();
                } finally {
                    topicActivationHandler.accept(false);
                    this.actionDispatcher = null;
                }
            });
        }
    }

    private Registration subscribeToChange() {
        synchronized (topic) {
            return topic.subscribeToChange((id, change) -> actionDispatcher
                    .dispatchAction(() -> handleChange(id, change)));
        }
    }
}
