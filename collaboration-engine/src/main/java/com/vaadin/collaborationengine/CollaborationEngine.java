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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

/**
 * CollaborationEngine is an API for creating collaborative experiences in
 * Vaadin applications. It's used by sending and subscribing to changes between
 * collaborators via {@link TopicConnection collaboration topics}.
 * <p>
 * Use {@link #getInstance()} to get a reference to the singleton object.
 *
 * @author Vaadin Ltd
 */
@JsModule("./field-highlighter/src/vaadin-field-highlighter.js")
public class CollaborationEngine {

    private static final CollaborationEngine collaborationEngine = new CollaborationEngine();
    static final int USER_COLOR_COUNT = 10;

    private Map<String, Topic> topics = new ConcurrentHashMap<>();
    private Map<String, Integer> userColors = new ConcurrentHashMap<>();

    CollaborationEngine() {
        // package-protected to hide from users but to be usable in unit tests
    }

    /**
     * Gets the {@link CollaborationEngine} singleton.
     *
     * @return the {@link CollaborationEngine} singleton
     */
    public static CollaborationEngine getInstance() {
        return collaborationEngine;
    }

    /**
     * Opens a connection to the collaboration topic with the provided id based
     * on a component instance. If the topic with the provided id does not exist
     * yet, it's created on demand.
     *
     * @param component
     *            the component which hold UI access, not {@code null}
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     * @param connectionActivationCallback
     *            the callback to be executed when a connection is activated,
     *            not {@code null}
     * @return the handle that can be used for closing the connection
     */
    public Registration openTopicConnection(Component component, String topicId,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(component, "Connection context can't be null");
        ConnectionContext context = new ComponentConnectionContext(component);
        return openTopicConnection(context, topicId,
                connectionActivationCallback);
    }

    /**
     * Opens a connection to the collaboration topic with the provided id based
     * on a generic context definition. If the topic with the provided id does
     * not exist yet, it's created on demand.
     *
     * @param context
     *            context for the connection
     * @param topicId
     *            the id of the topic to connect to, not {@code null}
     * @param connectionActivationCallback
     *            the callback to be executed when a connection is activated,
     *            not {@code null}
     * @return the handle that can be used for closing the connection
     */
    public Registration openTopicConnection(ConnectionContext context,
            String topicId,
            SerializableFunction<TopicConnection, Registration> connectionActivationCallback) {
        Objects.requireNonNull(context, "Connection context can't be null");
        Objects.requireNonNull(topicId, "Topic id can't be null");
        Objects.requireNonNull(connectionActivationCallback,
                "Callback for connection activation can't be null");
        Topic topic = topics.computeIfAbsent(topicId, id -> new Topic());

        TopicConnection connection = new TopicConnection(context, topic,
                connectionActivationCallback);
        return connection::close;
    }

    /**
     * Gets the color index of a user based on the user id. If the color index
     * for a user id does not exist yet, it's created on demand.
     *
     * @param userId
     *            user id
     * @return the color index
     */
    int getUserColorIndex(String userId) {
        Integer colorIndex = userColors.computeIfAbsent(userId,
                id -> userColors.size() % USER_COLOR_COUNT);
        return colorIndex.intValue();
    }
}
