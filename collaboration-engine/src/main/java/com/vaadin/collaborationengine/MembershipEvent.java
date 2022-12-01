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
import java.util.Objects;
import java.util.UUID;

/**
 * The event dispatched when a node joins or leaves the backend.
 *
 * @author Vaadin Ltd
 */
public class MembershipEvent extends EventObject {

    private final MembershipEventType type;

    private final UUID nodeId;

    /**
     * The type of the event.
     */
    public enum MembershipEventType {
        JOIN, LEAVE;
    }

    /**
     * Creates a new event.
     *
     * @param type
     *            the type of the event, not <code>null</code>
     * @param nodeId
     *            the node identifier, not <code>null</code>
     * @param collaborationEngine
     *            the source of the event, not <code>null</code>
     */
    public MembershipEvent(MembershipEventType type, UUID nodeId,
            CollaborationEngine collaborationEngine) {
        super(Objects.requireNonNull(collaborationEngine));
        this.type = Objects.requireNonNull(type);
        this.nodeId = Objects.requireNonNull(nodeId);
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event, not <code>null</code>
     */
    public MembershipEventType getType() {
        return type;
    }

    /**
     * Gets the node identifier.
     *
     * @return the node identifier, not <code>null</code>
     */
    public UUID getNodeId() {
        return nodeId;
    }

    @Override
    public CollaborationEngine getSource() {
        return (CollaborationEngine) super.getSource();
    }
}
