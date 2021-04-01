/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.time.Instant;
import java.util.EventObject;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Persister of {@link CollaborationMessage} items, which enables to read and
 * write messages from/to a backend, for example a database.
 * <p>
 * It can be used with a {@link CollaborationMessageList} to have the component
 * read messages from the backend when attached and write new messages to it
 * when appended to the list with a sumbmitter component, e.g.
 * {@link CollaborationMessageInput}.
 *
 * @author Vaadin Ltd
 * @see CollaborationMessageList#setTopic(String, CollaborationMessagePersister)
 */
public interface CollaborationMessagePersister extends Serializable {

    /**
     * A query to fetch messages from a backend. It provides information such as
     * the topic identifier and the timestamp since when messages should be
     * fetched.
     */
    class FetchQuery extends EventObject {
        private final String topicId;
        private final Instant since;

        FetchQuery(CollaborationMessageList list, String topicId,
                Instant since) {
            super(list);
            this.topicId = topicId;
            this.since = since;
        }

        /**
         * Gets the topic identifier.
         *
         * @return the topic identifier
         */
        public String getTopicId() {
            return topicId;
        }

        /**
         * Gets the timestamp since when messages should be fetched.
         *
         * @return the timestamp
         */
        public Instant getSince() {
            return since;
        }

        @Override
        public CollaborationMessageList getSource() {
            return (CollaborationMessageList) super.getSource();
        }
    }

    /**
     * A request to persist messages to a backend. It provides information such
     * as the topic identifier and the target {@link CollaborationMessage}.
     */
    class PersistRequest extends EventObject {
        private final String topicId;
        private final CollaborationMessage message;

        PersistRequest(CollaborationMessageList list, String topicId,
                CollaborationMessage message) {
            super(list);
            this.topicId = topicId;
            this.message = message;
        }

        /**
         * Gets the topic identifier.
         *
         * @return the topic identifier
         */
        public String getTopicId() {
            return topicId;
        }

        /**
         * Gets the message to persist.
         *
         * @return the message
         */
        public CollaborationMessage getMessage() {
            return message;
        }

        @Override
        public CollaborationMessageList getSource() {
            return (CollaborationMessageList) super.getSource();
        }
    }

    /**
     * Creates an instance of {@link CollaborationMessagePersister} from the
     * provided callbacks.
     *
     * @param fetchCallback
     *            the callback to fetch messages, not null
     * @param persistCallback
     *            the callback to persist messages, not null
     * @return the persister instance
     */
    static CollaborationMessagePersister fromCallbacks(
            SerializableFunction<FetchQuery, Stream<CollaborationMessage>> fetchCallback,
            SerializableConsumer<PersistRequest> persistCallback) {
        Objects.requireNonNull(fetchCallback,
                "The fetch callback cannot be null");
        Objects.requireNonNull(persistCallback,
                "The persist callback cannot be null");
        return new CollaborationMessagePersister() {

            @Override
            public Stream<CollaborationMessage> fetchMessages(
                    FetchQuery query) {
                return fetchCallback.apply(query);
            }

            @Override
            public void persistMessage(PersistRequest request) {
                persistCallback.accept(request);
            }
        };
    }

    /**
     * Reads a stream of {@link CollaborationMessage} items from a persistence
     * backend. The query parameter contains the topic identifier and the
     * timestamp after which messages should be read.
     *
     * @param query
     *            the fetch query
     * @return a stream of messages
     */
    Stream<CollaborationMessage> fetchMessages(FetchQuery query);

    /**
     * Writes a {@link CollaborationMessage} to the persistence backend.
     *
     * @param request
     *            the request to persist the message
     */
    void persistMessage(PersistRequest request);
}
