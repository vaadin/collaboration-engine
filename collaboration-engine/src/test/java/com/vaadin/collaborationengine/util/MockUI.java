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
package com.vaadin.collaborationengine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;

public class MockUI extends UI {

    private List<Command> accessTasks = new ArrayList<>();

    private boolean executeAccessTasks = true;

    public MockUI() {
        super();
        ReentrantLock lock = new ReentrantLock();
        // Lock to avoid triggering internal checks
        lock.lock();
        this.getInternals().setSession(new VaadinSession(new MockService()) {
            @Override
            public Lock getLockInstance() {
                return lock;
            }

            @Override
            public DeploymentConfiguration getConfiguration() {
                return this.getService().getDeploymentConfiguration();
            }

            @Override
            public Future<Void> access(Command command) {
                accessTasks.add(command);
                if (executeAccessTasks) {
                    command.execute();
                }
                return null;
            }
        });
    }

    @Override
    public int getUIId() {
        return 42;
    }

    public List<Command> getAccessTasks() {
        return accessTasks;
    }

    public void setExecuteAccessTasks(boolean executeAccessTasks) {
        this.executeAccessTasks = executeAccessTasks;
    }

    public void runAccessTasks() {
        // Need to this the hard way since a task might add another task
        while (!accessTasks.isEmpty()) {
            ArrayList<Command> copy = new ArrayList<>(accessTasks);
            accessTasks.clear();
            copy.forEach(Command::execute);
        }
    }
}
