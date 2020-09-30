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
        collaborationEngine = new CollaborationEngine(topicActivationHandler);

        topicRegistration = collaborationEngine.openTopicConnection(component,
                "fooTopic", SystemUserInfo.get(), tc -> null);
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
                SystemUserInfo.get(), tc -> null);
        ui.add(component2);
        // failed if setActive(true) is called again on an activated topic.
    }

    @Test
    public void hasActiveTopic_deactivateTopicConnections_hasNoActiveTopic() {
        ui.add(component);
        TestComponent component2 = new TestComponent();
        collaborationEngine.openTopicConnection(component2, "fooTopic",
                SystemUserInfo.get(), tc -> null);
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
                SystemUserInfo.get(), tc -> null);
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
                SystemUserInfo.get(), tc -> null);
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
