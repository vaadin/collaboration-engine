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

import com.vaadin.flow.component.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CollaborationEngine is an API for creating collaborative experiences in
 * Vaadin applications. It's used by sending and subscribing to changes between
 * collaborators via {@link TopicConnection collaboration topics}.
 * <p>
 * Use {@link #getInstance()} to get a reference to the singleton object.
 *
 * @author Vaadin Ltd
 */
public class CollaborationEngine {

    private static final CollaborationEngine collaborationEngine = new CollaborationEngine();

    /**
     * Gets the {@link CollaborationEngine} singleton.
     *
     * @return the {@link CollaborationEngine} singleton
     */
    public static CollaborationEngine getInstance() {
        return collaborationEngine;
    }

    private Map<String, Topic> topics = new ConcurrentHashMap<>();

    CollaborationEngine() {
        // package-protected to hide from users but to be usable in unit tests
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
     * @return the {@link TopicConnection} for sending and receiving updates
     * @throws NullPointerException
     *             if given {@code null} component or topic id
     */
    public TopicConnection openTopicConnection(Component component,
            String topicId) {
        return openTopicConnection(new ComponentConnectionContext(component),
                topicId);
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
     * @return the {@link TopicConnection} for sending and receiving updates
     * @throws NullPointerException
     *             if given {@code null} context or topic id
     */
    public TopicConnection openTopicConnection(ConnectionContext context,
            String topicId) {
        Objects.requireNonNull(context, "Connection context can't be null");
        Objects.requireNonNull(topicId, "Topic id can't be null");
        Topic topic = topics.computeIfAbsent(topicId, Topic::new);
        return new TopicConnection(context, topic);
    }
}
