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

import org.junit.Assert;

import com.vaadin.collaborationengine.ActionDispatcher;
import com.vaadin.collaborationengine.ActivationHandler;

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
