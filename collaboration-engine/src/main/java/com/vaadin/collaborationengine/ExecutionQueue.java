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

import java.util.concurrent.ConcurrentLinkedQueue;

import com.vaadin.flow.server.Command;

class ExecutionQueue {
    private final ConcurrentLinkedQueue<Command> inbox = new ConcurrentLinkedQueue<>();

    void add(Command command) {
        inbox.add(command);
    }

    void runPendingCommands() {
        while (true) {
            Command command = inbox.poll();
            if (command == null) {
                break;
            }
            command.execute();
        }
    }

    boolean isEmpty() {
        return inbox.isEmpty();
    }
}
