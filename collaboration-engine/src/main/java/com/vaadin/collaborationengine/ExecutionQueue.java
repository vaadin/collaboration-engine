/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
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
}
