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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.Topic.MapChangeNotifier;
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

    TopicConnection(ConnectionContext context, Topic topic) {
        this.topic = topic;
        this.context = context;
    }

    Topic getTopic() {
        return topic;
    }

    /**
     * Gets a collaborative map that can be used to track multiple values in a
     * single topic.
     *
     * @return the collaborative map, not <code>null</code>
     */
    public CollaborativeMap getMap() {
        return new CollaborativeMap() {
            @Override
            public Registration subscribe(MapSubscriber subscriber) {
                Objects.requireNonNull(subscriber, "Subscriber cannot be null");
                return topic.subscribeMap((key, oldValue, newValue) -> {
                    MapChangeEvent event = new MapChangeEvent(this, key,
                            oldValue, newValue);
                    context.dispatchAction(() -> subscriber.onMapChange(event));
                });
            }

            @Override
            public boolean replace(String key, Object expectedValue,
                    Object newValue) {
                Objects.requireNonNull(key, "Key cannot be null");
                return topic.withMap((map, changeListener) -> {
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

            @Override
            public void put(String key, Object value) {
                Objects.requireNonNull(key, "Key cannot be null");
                topic.withMap((map, changeListener) -> {
                    Object oldValue = map.get(key);
                    if (Objects.equals(oldValue, value)) {
                        return null;
                    }

                    updateMapValue(map, changeListener, key, value, oldValue);

                    return null;
                });
            }

            private void updateMapValue(Map<String, Object> map,
                    MapChangeNotifier changeNotifier, String key,
                    Object newValue, Object oldValue) {
                if (newValue == null) {
                    map.remove(key);
                } else {
                    map.put(key, newValue);
                }

                changeNotifier.onMapChange(key, oldValue, newValue);
            }

            @Override
            public Stream<String> getKeys() {
                return topic.withMap((map, changeListener) -> {
                    ArrayList<String> snapshot = new ArrayList<>(map.keySet());
                    return snapshot.stream();
                });
            }

            @Override
            public Object get(String key) {
                Objects.requireNonNull(key, "Key cannot be null");
                return topic.withMap((map, changeListener) -> map.get(key));
            }
        };
    }

    /**
     * Gets the current topic value.
     *
     * @return the topic value
     * @deprecated Store values in the map instead
     */
    @Deprecated
    public Object getValue() {
        return topic.getValue();
    }

    /**
     * Sets the value of the connected collaboration topic, notifying all
     * subscribers.
     *
     * @param value
     *            the new value to set for the topic
     * @deprecated Store values in the map instead
     */
    @Deprecated
    public void setValue(Object value) {
        topic.setValue(value);
    }

    /**
     * Atomically updates the value if the current value {@code equals} the
     * expected value. Subscribers are notified only if the value is updated.
     *
     * @param expected
     *            the expected value
     * @param update
     *            the value to set if the expected value is currently set
     * @return <code>true</code> if the value was updated, <code>false</code> if
     *         the previous value is retained
     * @deprecated Store values in the map instead
     */
    @Deprecated
    public boolean compareAndSet(Object expected, Object update) {
        return topic.compareAndSet(expected, update);
    }

    /**
     * Adds a subscriber which will be notified whenever someone changes the
     * value of the collaboration topic.
     *
     * @param subscriber
     *            the callback for handling topic value changes
     * @return a handle that can be used for removing the subscription, not
     *         <code>null</code>
     * @deprecated Store values in the map instead
     */
    @Deprecated
    public Registration subscribe(SingleValueSubscriber subscriber) {
        return topic.subscribe(newValue -> context
                .dispatchAction(() -> subscriber.onValueChange(newValue)));
    }

}
