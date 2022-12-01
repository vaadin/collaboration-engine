/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

import java.util.EventObject;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Event that is fired when the value in a collaboration list changes.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public class ListChangeEvent extends EventObject {

    private final ListChange change;

    /**
     * Creates a new event.
     *
     * @param list
     *            the list source of the event
     * @param change
     *            the list change
     */
    ListChangeEvent(CollaborationList list, ListChange change) {
        super(list);
        this.change = change;
    }

    @Override
    public CollaborationList getSource() {
        return (CollaborationList) super.getSource();
    }

    ListChangeType getType() {
        return change.getType();
    }

    /**
     * Gets the key of the list item affected by the change.
     *
     * @return the key of the changed item, not <code>null</code>
     */
    public ListKey getKey() {
        return new ListKey(change.getKey());
    }

    /**
     * Gets the current value of the list item affected by the change as
     * instance of the given class.
     * <p>
     * If the item was removed by the change, this method returns
     * <code>null</code> and {@link #getOldValue(Class)} return the removed item
     * value.
     *
     * @param <T>
     *            the type of the value from <code>type</code> parameter, e.g.
     *            <code>String</code>
     * @param type
     *            the expected type of the returned instance
     * @return the current value of the item affected by the change
     */
    public <T> T getValue(Class<T> type) {
        return JsonUtil.toInstance(change.getValue(), type);
    }

    /**
     * Gets the current value of the list item affected by the change as
     * instance corresponding to the given type reference.
     * <p>
     * If the item was removed by the change, this method returns
     * <code>null</code> and {@link #getOldValue(TypeReference)} return the
     * removed item value.
     *
     * @param <T>
     *            the type reference of the value from <code>type</code>
     *            parameter, e.g. <code>List<String></code>
     * @param type
     *            the expected type reference of the returned instance
     * @return the current value of the item affected by the change
     */
    public <T> T getValue(TypeReference<T> type) {
        return JsonUtil.toInstance(change.getValue(), type);
    }

    /**
     * Gets the old value of the list item affected by the change as instance of
     * the given class.
     *
     * @param <T>
     *            the type of the value from <code>type</code> parameter, e.g.
     *            <code>String</code>
     * @param type
     *            the expected type of the returned instance
     * @return the old value of the item affected by the change
     */
    public <T> T getOldValue(Class<T> type) {
        return JsonUtil.toInstance(change.getOldValue(), type);
    }

    /**
     * Gets the old value of the list item affected by the change as instance
     * corresponding to the given type reference.
     *
     * @param <T>
     *            the type reference of the value from <code>type</code>
     *            parameter, e.g. <code>List<String></code>
     * @param type
     *            the expected type reference of the returned instance
     * @return the old value of the item affected by the change
     */
    public <T> T getOldValue(TypeReference<T> type) {
        return JsonUtil.toInstance(change.getOldValue(), type);
    }

    /**
     * Gets the key of the item which is after the current item after the
     * change.
     *
     * @return the key of the item which is after the current item, or
     *         <code>null</code> if there is none
     */
    public ListKey getNext() {
        return ListKey.of(change.getNext());
    }

    /**
     * Gets the key of the item which was after the current item before the
     * change.
     *
     * @return the key of the item which was after the current item, or
     *         <code>null</code> if there was none
     */
    public ListKey getOldNext() {
        return ListKey.of(change.getOldNext());
    }

    /**
     * Gets the key of the item which is before the current item after the
     * change.
     *
     * @return the key of the item which is before the current item, or
     *         <code>null</code> if there is none
     */
    public ListKey getPrev() {
        return ListKey.of(change.getPrev());
    }

    /**
     * Gets the key of the item which was before the current item before the
     * change.
     *
     * @return the key of the item which was before the current item, or
     *         <code>null</code> if there was none
     */
    public ListKey getOldPrev() {
        return ListKey.of(change.getOldPrev());
    }

    /**
     * Gets the added item as instance of the given class.
     *
     * @param type
     *            the class of the expected type of the returned instance
     * @param <T>
     *            the type of the class given as the <code>type</code> argument
     * @return the added item, or an empty optional if no item was added
     *
     * @deprecated This method is preserved for backwards compatibility from the
     *             initial version when the only possible list change was to add
     *             a new item to the end of the list.
     */
    @Deprecated
    public <T> Optional<T> getAddedItem(Class<T> type) {
        return convertAddedItem(JsonUtil.fromJsonConverter(type));
    }

    /**
     * Gets the added item as instance of the given type reference.
     *
     * @param type
     *            the expected type reference of the returned instance
     * @param <T>
     *            the type of the reference given as the <code>type</code>
     *            argument
     * @return the added item, or an empty optional if no item was added
     *
     * @deprecated This method is preserved for backwards compatibility from the
     *             initial version when the only possible list change was to add
     *             a new item to the end of the list.
     */
    @Deprecated
    public <T> Optional<T> getAddedItem(TypeReference<T> type) {
        return convertAddedItem(JsonUtil.fromJsonConverter(type));
    }

    private <T> Optional<T> convertAddedItem(Function<JsonNode, T> converter) {
        if (change.getType() != ListChangeType.INSERT) {
            return Optional.empty();
        }
        return Optional.ofNullable(change.getValue()).map(converter);
    }
}
