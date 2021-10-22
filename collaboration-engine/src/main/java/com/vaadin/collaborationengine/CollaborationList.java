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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import com.vaadin.flow.shared.Registration;

/**
 * A list that is shared between multiple users. List instances can be retrieved
 * through a {@link TopicConnection}. Changes performed by one user will be
 * delivered as events to subscribers defined by other users.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public interface CollaborationList extends HasExpirationTimeout {

    /**
     * Gets the list items as instances of the given class.
     *
     * @param type
     *            the expected type of the items
     * @param <T>
     *            the type of the class given as the argument
     * @return a list of the items
     * @throws JsonConversionException
     *             if one or more values in the list cannot be converted to an
     *             instance of the given class
     */
    <T> List<T> getItems(Class<T> type);

    /**
     * Gets the list items as instances of the given type reference.
     *
     * @param type
     *            the reference of the expected type of the items
     * @param <T>
     *            the type of the reference given as the argument
     * @return a list of the items
     * @throws JsonConversionException
     *             if one or more values in the list cannot be converted to an
     *             instance of the given type reference
     */
    <T> List<T> getItems(TypeReference<T> type);

    /**
     * Appends the given item to the list.
     * <p>
     * The given item must be JSON-serializable so it can be sent over the
     * network when Collaboration Engine is hosted in a standalone server.
     *
     * @param item
     *            the item to append, not <code>null</code>
     * @return a completable future that is resolved when the item has been
     *         appended to the list
     * @throws JsonConversionException
     *             if the given item isn't serializable as JSON string
     */
    default CompletableFuture<Void> append(Object item) {
        return append(item, EntryScope.TOPIC);
    }

    /**
     * Appends the given item to the list with the given scope.
     * <p>
     * The given item must be JSON-serializable so it can be sent over the
     * network when Collaboration Engine is hosted in a standalone server.
     * <p>
     * The <code>scope</code> parameter specifies the scope of the entry, which
     * is either one of {@link EntryScope#TOPIC} to keep the entry in the list
     * until explicitly removed, or {@link EntryScope#CONNECTION} to
     * automatically remove the entry when the connection which put the entry is
     * deactivated.
     *
     * @param item
     *            the item to append, not <code>null</code>
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return a completable future that is resolved when the item has been
     *         appended to the list
     * @throws JsonConversionException
     *             if the given item isn't serializable as JSON string
     */
    CompletableFuture<Void> append(Object item, EntryScope scope);

    /**
     * Subscribes to changes to this list. When subscribing, the subscriber will
     * receive an event for each item already in the list.
     *
     * @param subscriber
     *            the subscriber to use, not <code>null</code>
     * @return a handle that can be used for removing the subscription, not
     *         <code>null</code>
     */
    Registration subscribe(ListSubscriber subscriber);

    /**
     * Gets the topic connection which is used to propagate changes to this
     * list.
     *
     * @return the topic connection used by this list, not <code>null</code>
     */
    TopicConnection getConnection();

    /**
     * Gets the optional expiration timeout of this list. An empty
     * {@link Optional} is returned if no timeout is set, which means the list
     * is not cleared when there are no connected users to the related topic
     * (this is the default).
     *
     * @return the expiration timeout
     */
    @Override
    Optional<Duration> getExpirationTimeout();

    /**
     * Sets the expiration timeout of this list. If set, the list content is
     * cleared when {@code expirationTimeout} has passed after the last
     * connection to the topic this list belongs to is closed. If set to
     * {@code null}, the timeout is cancelled.
     *
     * @param expirationTimeout
     *            the expiration timeout
     */
    @Override
    void setExpirationTimeout(Duration expirationTimeout);
}
