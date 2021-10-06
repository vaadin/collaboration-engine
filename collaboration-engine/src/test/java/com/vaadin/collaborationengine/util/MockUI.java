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
        });
    }

    @Override
    public Future<Void> access(Command command) {
        accessTasks.add(command);
        if (executeAccessTasks) {
            command.execute();
        }
        return null;
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
