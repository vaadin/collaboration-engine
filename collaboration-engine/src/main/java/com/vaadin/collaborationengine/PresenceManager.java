/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.collaborationengine.PresenceHandler.PresenceContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.Registration;

/**
 * Manager to handle user presence in topics. It allows to set the user presence
 * in the set topic and get a list of users currently set as present in the same
 * topic. A handler can be set to react when the presence of a user changes.
 *
 * @author Vaadin Ltd
 * @since 3.2
 */
public class PresenceManager {

    private static class UserEntry {
        private int count = 0;
        private Registration registration;
    }

    static final String LIST_NAME = PresenceManager.class.getName();

    private final Map<String, UserEntry> userEntries = new LinkedHashMap<>();

    private final UserInfo localUser;

    private final String topicId;

    private ListKey ownPresenceKey;

    private TopicConnection.CollaborationListImplementation list;

    private PresenceHandler presenceHandler;

    private boolean markAsPresent = false;

    private TopicConnectionRegistration topicRegistration;

    private Registration subscribeRegistration;

    /**
     * Creates a new manager for the provided component.
     * <p>
     * The provided user information is used to set the presence of the local
     * user with {@link #markAsPresent(boolean)} (the default is {@code false}).
     * <p>
     *
     * @param component
     *            the component which holds UI access, not {@code null}
     * @param localUser
     *            the information of the local user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     */
    public PresenceManager(Component component, UserInfo localUser,
            String topicId) {
        this(new ComponentConnectionContext(component), localUser, topicId,
                CollaborationEngine.getInstance());
    }

    /**
     * Creates a new manager for the provided connection context.
     * <p>
     * The provided user information is used to set the presence of the local
     * user with {@link #markAsPresent(boolean)} (the default is {@code false}).
     * <p>
     *
     * @param context
     *            the context that manages connection status, not {@code null}
     * @param localUser
     *            the information of the local user, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     * @param collaborationEngine
     *            the collaboration engine instance to use, not {@code null}
     */
    public PresenceManager(ConnectionContext context, UserInfo localUser,
            String topicId, CollaborationEngine collaborationEngine) {
        this.localUser = Objects.requireNonNull(localUser);
        this.topicId = Objects.requireNonNull(topicId);
        this.topicRegistration = collaborationEngine.openTopicConnection(
                context, topicId, localUser, this::onConnectionActivate);
    }

    /**
     * Gets the topic id.
     *
     * @return the topic id
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     * Disconnects from the topic.
     */
    public void close() {
        if (topicRegistration != null) {
            // This will also trigger onConnectionDeactivate which will remove
            // all handler registrations
            topicRegistration.remove();
            topicRegistration = null;
        }
    }

    /**
     * Configures the manager to mark the local user present in the topic.
     * <p>
     * If the user wasn't already present in the topic, all managers connected
     * to the same topic will be notified of the change and their handlers will
     * be applied to the user instance.
     *
     * @param markAsPresent
     *            {@code true} to mark the user as present in the topic,
     *            {@code false} to set as not present
     */
    public void markAsPresent(boolean markAsPresent) {
        if (this.markAsPresent != markAsPresent && list != null) {
            if (markAsPresent) {
                addLocalUserToTopic();
            } else {
                removeLocalUserFromTopic();
            }
        }
        this.markAsPresent = markAsPresent;
    }

    private void addLocalUserToTopic() {
        assert ownPresenceKey == null;
        ownPresenceKey = list.insertLast(localUser, EntryScope.CONNECTION)
                .getKey();
    }

    private void removeLocalUserFromTopic() {
        assert ownPresenceKey != null;
        list.set(ownPresenceKey, null);
        ownPresenceKey = null;
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
     * @deprecated Use {@link #setPresenceHandler(PresenceHandler)} instead
     */
    @Deprecated
    public void setNewUserHandler(NewUserHandler handler) {
        setPresenceHandler(context -> handler.handleNewUser(context.getUser()));
    }

    /**
     * Sets a handler which will be invoked when a user becomes present.
     * <p>
     * The handler accepts a {@link PresenceContext} instance as a parameter and
     * should return a {@link Registration} which will be removed when the user
     * stops being present.
     * <p>
     * Replacing an existing handler will remove all registrations from the
     * previous one.
     *
     * @param handler
     *            the user presence handler, or {@code null} to remove an
     *            existing handler
     */
    public void setPresenceHandler(PresenceHandler handler) {
        resetEntries();
        this.presenceHandler = handler;
        if (handler != null && list != null) {
            subscribeRegistration = list.subscribe(this::onListChange);
        }
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        list = (TopicConnection.CollaborationListImplementation) topicConnection
                .getNamedList(LIST_NAME);
        if (markAsPresent) {
            addLocalUserToTopic();
        }
        if (this.presenceHandler != null && subscribeRegistration == null) {
            subscribeRegistration = list.subscribe(this::onListChange);
        }
        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        ownPresenceKey = null;
        list = null;
        resetEntries();
    }

    private void onListChange(ListChangeEvent event) {
        switch (event.getType()) {
        case INSERT:
            handleNewUser(event.getValue(UserInfo.class));
            break;
        case SET:
            if (event.getValue(UserInfo.class) == null) {
                handleRemovedUser(event.getOldValue(UserInfo.class));
            } else {
                throw new UnsupportedOperationException(
                        "Cannot update an existing entry");
            }
            break;
        case MOVE:
            // Unexpected, but no problem in ignoring
            break;
        }
    }

    private void handleRemovedUser(UserInfo removedUser) {
        UserEntry userEntry = userEntries.get(removedUser.getId());
        if (--userEntry.count == 0) {
            removeRegistration(userEntry);
            userEntries.remove(removedUser.getId());
        }
    }

    private void handleNewUser(UserInfo addedUser) {
        UserEntry userEntry = userEntries.computeIfAbsent(addedUser.getId(),
                ignore -> new UserEntry());
        if (userEntry.count++ == 0) {
            if (presenceHandler != null) {
                assert userEntry.registration == null;
                userEntry.registration = presenceHandler
                        .handlePresence(new DefaultPresenceContext(addedUser));
            }
        }
    }

    private void removeRegistration(UserEntry entry) {
        Registration registration = entry.registration;
        if (registration != null) {
            registration.remove();
            entry.registration = null;
        }
    }

    private void resetEntries() {
        if (subscribeRegistration != null) {
            subscribeRegistration.remove();
            subscribeRegistration = null;
        }

        userEntries.values().forEach(this::removeRegistration);
        userEntries.clear();
    }

    static class DefaultPresenceContext implements PresenceContext {

        private final UserInfo user;

        public DefaultPresenceContext(UserInfo user) {
            this.user = user;
        }

        @Override
        public UserInfo getUser() {
            return user;
        }
    }
}
