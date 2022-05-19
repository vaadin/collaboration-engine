/*
 * Copyright 2020-2022 Vaadin Ltd.
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
import java.util.stream.Stream;

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
     * Gets the list item identifier by the given key as instance of the given
     * class.
     *
     * @param <T>
     *            the type of the value from <code>type</code> parameter, e.g.
     *            <code>String</code>
     * @param key
     *            the key of the requested item, not <code>null</code>
     * @param type
     *            the expected type of the item, not <code>null</code>
     * @return the requested item
     * @throws JsonConversionException
     *             if the value in the list cannot be converted to an instance
     *             of the given class
     * @since 4.1
     */
    <T> T getItem(ListKey key, Class<T> type);

    /**
     * Gets the list item identifier by the given key as instance of the given
     * type reference.
     *
     * @param <T>
     *            the type reference of the value from <code>type</code>
     *            parameter, e.g. <code>List<String></code>
     * @param key
     *            the key of the requested item, not <code>null</code>
     * @param type
     *            the expected type reference of the item, not <code>null</code>
     * @return the requested item
     * @throws JsonConversionException
     *             if the value in the list cannot be converted to an instance
     *             of the given class
     * @since 4.1
     */
    <T> T getItem(ListKey key, TypeReference<T> type);

    /**
     * Gets the keys for all the items on the list.
     *
     * @return the keys
     * @since 4.1
     */
    Stream<ListKey> getKeys();

    /**
     * Performs the given list operation. The operation contains the value, its
     * position and other requirements. If any of the conditions are not met,
     * the operation may fail (see {@link ListOperationResult}).
     *
     * @param operation
     *            the list operation, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    ListOperationResult<Boolean> apply(ListOperation operation);

    /**
     * Inserts the given item as the first item of the list.
     *
     * @param item
     *            the item, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Void> insertFirst(Object item) {
        return insertFirst(item, EntryScope.TOPIC);
    }

    /**
     * Inserts the given item as the first item of the list, with the given
     * scope.
     *
     * @param item
     *            the item, not <code>null</code>
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Void> insertFirst(Object item,
            EntryScope scope) {
        ListOperation operation = ListOperation.insertFirst(item)
                .withScope(scope);
        return apply(operation).mapToVoid();
    }

    /**
     * Inserts the given item as the last item of the list.
     *
     * @param item
     *            the item, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     * @since 4.1
     */
    default ListOperationResult<Void> insertLast(Object item) {
        return insertLast(item, EntryScope.TOPIC);
    }

    /**
     * Inserts the given item as the last item of the list, with the given
     * scope.
     *
     * @param item
     *            the item, not <code>null</code>
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     * @since 4.1
     */
    default ListOperationResult<Void> insertLast(Object item,
            EntryScope scope) {
        ListOperation operation = ListOperation.insertLast(item)
                .withScope(scope);
        return apply(operation).mapToVoid();
    }

    /**
     * Inserts the given item just before the given key location.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param item
     *            the item, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Boolean> insertBefore(ListKey key,
            Object item) {
        return insertBefore(key, item, EntryScope.TOPIC);
    }

    /**
     * Inserts the given item just before the given key location, with the given
     * scope.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param item
     *            the item, not <code>null</code>
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Boolean> insertBefore(ListKey key, Object item,
            EntryScope scope) {
        ListOperation operation = ListOperation.insertBefore(key, item)
                .withScope(scope);
        return apply(operation);
    }

    /**
     * Inserts the given item just after the given key location.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param item
     *            the item, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Boolean> insertAfter(ListKey key, Object item) {
        return insertAfter(key, item, EntryScope.TOPIC);
    }

    /**
     * Inserts the given item just after the given key location, with the given
     * scope.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param item
     *            the item, not <code>null</code>
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return the result of the operation, not <code>null</code>
     */
    default ListOperationResult<Boolean> insertAfter(ListKey key, Object item,
            EntryScope scope) {
        ListOperation operation = ListOperation.insertAfter(key, item)
                .withScope(scope);
        return apply(operation);
    }

    /**
     * Moves the second given key just before the first given key location.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param keyToMove
     *            the key to move, not <code>null</code>
     * @return a completable future that is resolved when the operation has
     *         completed, not <code>null</code>
     */
    CompletableFuture<Boolean> moveBefore(ListKey key, ListKey keyToMove);

    /**
     * Moves the second given key just after the first given key location.
     *
     * @param key
     *            the position key, not <code>null</code>
     * @param keyToMove
     *            the key to move, not <code>null</code>
     * @return a completable future that is resolved when the operation has
     *         completed, not <code>null</code>
     */
    CompletableFuture<Boolean> moveAfter(ListKey key, ListKey keyToMove);

    /**
     * Sets a new value for the item identified by the given key.
     * <p>
     * It return the result of the operation as a {@link CompletableFuture}
     * which resolves to <code>true<code> if the operation succeeds,
     * <code>false</code> otherwise.
     *
     * @param key
     *            the item key, not <code>null</code>
     * @param value
     *            the new value of the item
     * @return a completable future that is resolved when the operation has
     *         completed, not <code>null</code>
     * @since 4.1
     */
    default CompletableFuture<Boolean> set(ListKey key, Object value) {
        return set(key, value, EntryScope.TOPIC);
    }

    /**
     * Sets a new value for the item identified by the given key, with the given
     * scope.
     * <p>
     * It return the result of the operation as a {@link CompletableFuture}
     * which resolves to <code>true<code> if the operation succeeds,
     * <code>false</code> otherwise.
     *
     * @param key
     *            the item key, not <code>null</code>
     * @param value
     *            the new value of the item
     * @param scope
     *            the scope of the entry, not <code>null</code>
     * @return a completable future that is resolved when the operation has
     *         completed, not <code>null</code>
     * @since 4.1
     */
    CompletableFuture<Boolean> set(ListKey key, Object value, EntryScope scope);

    /**
     * Removes the value for the item identified by the given key.
     * <p>
     * It returns the result of the operation as a {@link CompletableFuture}
     * which resolves to <code>true</code> if the operation succeeds,
     * <code>false</code> otherwise.
     *
     * @param key
     *            the item key, not <code>null</code>
     * @return a completable future that is resolved when the operation has
     *         completed, not <code>null</code>
     */
    default CompletableFuture<Boolean> remove(ListKey key) {
        return set(key, null);
    }

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
     * @deprecated Use {@link #insertLast(Object)}
     */
    @Deprecated
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
     * @deprecated Use {@link #insertLast(Object, EntryScope)}
     */
    @Deprecated
    default CompletableFuture<Void> append(Object item, EntryScope scope) {
        return insertLast(item, scope).getCompletableFuture();
    }

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
