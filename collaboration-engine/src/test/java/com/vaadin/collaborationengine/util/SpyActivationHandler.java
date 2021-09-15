package com.vaadin.collaborationengine.util;

import com.vaadin.collaborationengine.ActionDispatcher;
import com.vaadin.collaborationengine.ActivationHandler;
import org.junit.Assert;

public class SpyActivationHandler implements ActivationHandler {
    private boolean changeExpected = true;

    private boolean active;
    private ActionDispatcher actionDispatcher;

    @Override
    public void accept(ActionDispatcher actionDispatcher) {
        if (!this.changeExpected) {
            Assert.fail("No change expected");
        }
        this.actionDispatcher = actionDispatcher;
        this.changeExpected = false;
        this.active = this.actionDispatcher != null;
    }

    public void assertActive(String message) {
        Assert.assertTrue(message, active);
        changeExpected = true;
    }

    public void assertInactive(String message) {
        Assert.assertFalse(message, active);
        changeExpected = true;
    }

    public ActionDispatcher getActionDispatcher() {
        return actionDispatcher;
    }
}
