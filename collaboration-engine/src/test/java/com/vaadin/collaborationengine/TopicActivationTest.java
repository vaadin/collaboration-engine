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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestComponent;
import com.vaadin.flow.shared.Registration;

public class TopicActivationTest {
    private CollaborationEngine collaborationEngine;
    private MockUI ui;
    private TestComponent component;
    private Registration topicRegistration;

    private SpyTopicActivationHandler topicActivationHandler;

    @Before
    public void init() {
        ui = new MockUI();
        component = new TestComponent();

        topicActivationHandler = new SpyTopicActivationHandler();
        collaborationEngine = new TestUtil.TestCollaborationEngine(
                topicActivationHandler);
        TestUtil.configureTestCollaborationEngine(ui.getSession().getService(),
                collaborationEngine);

        topicRegistration = collaborationEngine.openTopicConnection(component,
                "fooTopic", SystemUserInfo.getInstance(), tc -> null);
    }

    @Test
    public void noActiveTopicConnection_noActiveTopic() {
        topicActivationHandler.assertActivated("fooTopic", null,
                "Should have no active the topic.");
    }

    @Test
    public void activateTwoTopicConnections_hasActiveTopic_noDuplicatedCall() {
        ui.add(component);
        TestComponent component2 = new TestComponent();
        collaborationEngine.openTopicConnection(component2, "fooTopic",
                SystemUserInfo.getInstance(), tc -> null);
        ui.add(component2);
        // failed if setActive(true) is called again on an activated topic.
    }

    @Test
    public void hasActiveTopic_deactivateTopicConnections_hasNoActiveTopic() {
        ui.add(component);
        TestComponent component2 = new TestComponent();
        collaborationEngine.openTopicConnection(component2, "fooTopic",
                SystemUserInfo.getInstance(), tc -> null);
        ui.add(component2);

        ui.remove(component);
        topicActivationHandler.assertActivated("fooTopic", true,
                "Topic should remain activated.");

        ui.remove(component2);
        topicActivationHandler.assertActivated("fooTopic", false,
                "Topic should be deactivated.");
    }

    @Test
    public void hasActiveTopic_closeTopicConnections_hasNoActiveTopic() {
        ui.add(component);
        topicActivationHandler.assertActivated("fooTopic", true,
                "Topic should be activated.");

        topicRegistration.remove();
        topicActivationHandler.assertActivated("fooTopic", false,
                "Topic should be deactivated.");
    }

    @Test
    public void hasTwoActiveTopicConnections_deactivateOne_handlerNotTriggered() {
        ui.add(component);
        TestComponent component2 = new TestComponent();
        collaborationEngine.openTopicConnection(component2, "fooTopic",
                SystemUserInfo.getInstance(), tc -> null);
        ui.add(component2);

        topicActivationHandler.assertActivated("fooTopic", true,
                "Handler should be triggered already.");
        topicActivationHandler.reset();
        ui.remove(component2);
        topicActivationHandler.assertActivated("fooTopic", null,
                "Handler should not be triggered again when turning from 2 to 1 active connection.");
    }

    @Test
    public void hasOneTopicConnection_toggleActivation_topicActivationUpdatedAccordingly() {
        ui.add(component);
        topicActivationHandler.assertActivated("fooTopic", true,
                "Topic should be activated.");

        ui.remove(component);
        topicActivationHandler.assertActivated("fooTopic", false,
                "Topic should be deactivated.");

        ui.add(component);
        topicActivationHandler.assertActivated("fooTopic", true,
                "Topic should be re-activated.");
    }

    @Test
    public void hasTwoTopic_toggleActivationOneTopic_otherTopicNotAffected() {
        ui.add(component);
        topicActivationHandler.assertActivated("fooTopic", true, "");
        topicActivationHandler.reset(); // remove fooTopic from activation log

        TestComponent component2 = new TestComponent();
        collaborationEngine.openTopicConnection(component2, "barTopic",
                SystemUserInfo.getInstance(), tc -> null);
        ui.add(component2);
        topicActivationHandler.assertActivated("barTopic", true, "");
        topicActivationHandler.assertActivated("fooTopic", null,
                "fooTopic is not affected.");

        ui.remove(component2);
        topicActivationHandler.assertActivated("barTopic", false, "");
        topicActivationHandler.assertActivated("fooTopic", null,
                "fooTopic is not affected.");
    }

    class SpyTopicActivationHandler implements TopicActivationHandler {
        private boolean changeExpected = true;
        private Map<String, Boolean> topicsActivation = new HashMap<>();

        @Override
        public void setActive(String topicId, boolean isActive) {
            if (!changeExpected) {
                Assert.fail("No change expected");
            }
            topicsActivation.put(topicId, isActive);
            changeExpected = false;
        }

        public void assertActivated(String topicId, Boolean expect,
                String message) {
            Assert.assertEquals(message, expect,
                    topicsActivation.getOrDefault(topicId, null));
            changeExpected = true;
        }

        public void reset() {
            topicsActivation = new HashMap<>();
        }
    }

}
