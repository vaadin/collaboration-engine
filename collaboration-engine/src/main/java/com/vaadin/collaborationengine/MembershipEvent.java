/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
