/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;

/**
 * Adapter to handle user presence in topics. It allows to set the user presence
 * in the set topic and get a list of users currently set as present in the same
 * topic. A handler can be set to react when the presence of a user changes.
 *
 * @author Vaadin Ltd
 */
public class PresenceAdapter {

    static final String MAP_NAME = PresenceAdapter.class.getName();

    static final String MAP_KEY = "users";

    private final CollaborationEngine collaborationEngine;

    private final Map<String, Registration> handlerRegistrations = new ConcurrentHashMap<>();

    private final SerializableSupplier<ConnectionContext> contextSupplier;

    private final UserInfo localUser;

    private CollaborationMap map;

    private NewUserHandler newUserHandler;

    private boolean autoPresence = false;

    private TopicConnectionRegistration topicRegistration;

    /**
     * Creates a new adapter for the provided component, with the provided local
     * user and topic id.
     * <p>
     * The provided user information is used to set the presence of the local
     * user with {@link #setAutoPresence(boolean)} (the default is
     * {@code false}).
     * <p>
     * If a {@code null} topic id is provided, {@link #getUsers()} will return
     * an empty stream until connecting to a non-null topic with
     * {@link #setTopic(String)}.
     *
     * @param component
     *            the component which holds UI access, not {@code null}
     * @param localUser
     *            the information of the local user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, or {@code null} to not
     *            connect the component to any topic
     */
    public PresenceAdapter(Component component, UserInfo localUser,
            String topicId) {
        this(() -> new ComponentConnectionContext(component), localUser,
                topicId);
    }

    /**
     * Creates a new adapter for the context obtained from the provided
     * supplier, with the provided local user and topic id.
     * <p>
     * The context supplier will be used when the topic connection is opened.
     * <p>
     * The provided user information is used to set the presence of the local
     * user with {@link #setAutoPresence(boolean)} (the default is
     * {@code false}).
     * <p>
     * If a {@code null} topic id is provided, {@link #getUsers()} will return
     * an empty stream until connecting to a non-null topic with
     * {@link #setTopic(String)}.
     *
     * @param contextSupplier
     *            the connection context supplier, not {@code null}
     * @param localUser
     *            the information of the local user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, or {@code null} to not
     *            connect the component to any topic
     */
    PresenceAdapter(SerializableSupplier<ConnectionContext> contextSupplier,
            UserInfo localUser, String topicId) {
        this(contextSupplier, localUser, topicId,
                CollaborationEngine.getInstance());
    }

    PresenceAdapter(SerializableSupplier<ConnectionContext> contextSupplier,
            UserInfo localUser, String topicId,
            CollaborationEngine collaborationEngine) {
        this.contextSupplier = Objects.requireNonNull(contextSupplier);
        this.localUser = Objects.requireNonNull(localUser);
        this.collaborationEngine = Objects.requireNonNull(collaborationEngine);
        setTopic(topicId);
    }

    /**
     * Sets the topic to use with this adapter. The connection to the previous
     * topic (if any) is closed and the local user is removed from the topic (if
     * previously set with this adapter). Connection to the new topic is opened
     * and the local user is added to the topic if previously set using
     * {@link #setAutoPresence(boolean)}.
     * <p>
     * If the topic id is {@code null}, the connection is closed.
     *
     * @param topicId
     *            the topic id to connect to, or {@code null} to not connect to
     *            any topic
     */
    public void setTopic(String topicId) {
        closeTopicConnection();
        if (topicId != null) {
            topicRegistration = collaborationEngine.openTopicConnection(
                    contextSupplier.get(), topicId, localUser,
                    this::onConnectionActivate);
        }
    }

    private void closeTopicConnection() {
        if (topicRegistration != null) {
            // This will also trigger onConnectionDeactivate which will remove
            // all handler registrations
            topicRegistration.remove();
            topicRegistration = null;
        }
    }

    /**
     * Checks if this adapter is configured to set the local user automatically
     * present when a topic is set.
     *
     * @return {@code true} if this adapter is configured to set the local user
     *         automatically present
     */
    public boolean isAutoPresence() {
        return autoPresence;
    }

    /**
     * Configures the adapter to set the local user automatically present when a
     * topic is set.
     * <p>
     * If the user wasn't already present in the topic, all adapters connected
     * to the same topic will be notified of the change and their handlers will
     * be applied to the user instance.
     *
     * @param autoPresence
     *            {@code true} to set the user as present when a topic is set,
     *            {@code false} to set as not present
     */
    public void setAutoPresence(boolean autoPresence) {
        if (this.autoPresence != autoPresence && map != null) {
            if (autoPresence) {
                addLocalUserToTopic();
            } else {
                removeLocalUserFromTopic();
            }
        }
        this.autoPresence = autoPresence;
    }

    private void addLocalUserToTopic() {
        updateUsers(map,
                oldValue -> Stream.concat(oldValue, Stream.of(localUser)));
    }

    private void removeLocalUserFromTopic() {
        updateUsers(map, oldValue -> {
            List<UserInfo> users = oldValue.collect(Collectors.toList());
            users.remove(localUser);
            return users.stream();
        });
    }

    /**
     * Sets a handler which will be invoked when a user becomes present.
     * <p>
     * The handler accepts a {@link UserInfo} instance as a parameter and should
     * return a {@link Registration} which will be removed when the user stops
     * being present.
     * <p>
     * Replacing an existing handler will remove all registrations from the
     * previous one.
     *
     * @param handler
     *            the user presence handler, or {@code null} to remove an
     *            existing handler
     */
    public void setNewUserHandler(NewUserHandler handler) {
        removeAllRegistrations();
        this.newUserHandler = handler;
        if (handler != null) {
            getUsers().forEach(this::applyHandler);
        }
    }

    /**
     * Gets the stream of users whose are currently marked as present. If the
     * topic is currently set to {@code null} the stream will be empty.
     *
     * @return the stream of users, not {@code null}
     */
    public Stream<UserInfo> getUsers() {
        if (map != null) {
            List<UserInfo> list = map.get(MAP_KEY, JsonUtil.LIST_USER_TYPE_REF);
            return list != null ? list.stream().distinct() : Stream.empty();
        } else {
            return Stream.empty();
        }
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        map = topicConnection.getNamedMap(MAP_NAME);
        map.subscribe(this::onMapChange);
        if (autoPresence) {
            addLocalUserToTopic();
        }
        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        if (autoPresence) {
            removeLocalUserFromTopic();
        }
        map = null;
        removeAllRegistrations();
    }

    private void onMapChange(MapChangeEvent event) {
        if (event.getKey().equals(MAP_KEY)) {
            List<UserInfo> oldUsersList = event
                    .getOldValue(JsonUtil.LIST_USER_TYPE_REF);
            List<UserInfo> newUsersList = event
                    .getValue(JsonUtil.LIST_USER_TYPE_REF);
            if (oldUsersList != null) {
                diff(oldUsersList, newUsersList).map(UserInfo::getId)
                        .forEach(this::removeRegistration);
            }
            if (newUsersList != null && newUserHandler != null) {
                diff(newUsersList, oldUsersList).forEach(this::applyHandler);
            }
        }
    }

    private Stream<UserInfo> diff(List<UserInfo> x, List<UserInfo> y) {
        Set<UserInfo> set = new LinkedHashSet<>(x);
        if (y != null) {
            set.removeAll(y);
        }
        return set.stream();
    }

    private void applyHandler(UserInfo user) {
        if (newUserHandler != null) {
            handlerRegistrations.put(user.getId(),
                    newUserHandler.handleNewUser(user));
        }
    }

    private void removeRegistration(String key) {
        Registration registration = handlerRegistrations.remove(key);
        if (registration != null) {
            registration.remove();
        }
    }

    private void removeAllRegistrations() {
        List<String> keys = new ArrayList<>(handlerRegistrations.keySet());
        keys.forEach(this::removeRegistration);
    }

    private void updateUsers(CollaborationMap map,
            SerializableFunction<Stream<UserInfo>, Stream<UserInfo>> updater) {
        List<UserInfo> oldUsers = map.get(MAP_KEY, JsonUtil.LIST_USER_TYPE_REF);

        Stream<UserInfo> oldUsersStream = oldUsers == null ? Stream.empty()
                : oldUsers.stream();
        List<UserInfo> newUsers = updater.apply(oldUsersStream)
                .collect(Collectors.toList());

        map.replace(MAP_KEY, oldUsers, newUsers).thenAccept(success -> {
            if (!success) {
                updateUsers(map, updater);
            }
        });
    }
}
