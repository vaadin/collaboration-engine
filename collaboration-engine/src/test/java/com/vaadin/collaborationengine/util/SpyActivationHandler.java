package com.vaadin.collaborationengine.util;

import com.vaadin.collaborationengine.ActivationHandler;
import org.junit.Assert;

public class SpyActivationHandler implements ActivationHandler {
    private boolean changeExpected = true;

    private boolean active;

    @Override
    public void setActive(boolean active) {
        if (!changeExpected) {
            Assert.fail("No change expected");
        }
        this.active = active;

        changeExpected = false;
    }

    public void assertActive(String message) {
        Assert.assertTrue(message, active);
        changeExpected = true;
    }

    public void assertInactive(String message) {
        Assert.assertFalse(message, active);
        changeExpected = true;
    }
}
