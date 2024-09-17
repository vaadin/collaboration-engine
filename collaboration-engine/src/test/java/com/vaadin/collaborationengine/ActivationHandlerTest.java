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

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.TestUtils;

public class ActivationHandlerTest {

    private CollaborationEngine collaborationEngine;
    private MockConnectionContext context;

    @Before
    public void init() {
        collaborationEngine = TestUtil.createTestCollaborationEngine();
        context = new MockConnectionContext();
    }

    @Test
    public void openTopicConnection_triggerCallbackOnlyWhenActivated() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    isCalled.set(true);
                    return null;
                });

        Assert.assertFalse("Expect the connection callback not to be called",
                isCalled.get());
        context.activate();
        Assert.assertTrue("Expect the connection callback to be called",
                isCalled.get());
    }

    @Test
    public void deactivateConnection_subscriberNoLongerReceiveCurrentValue() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map")
                            .subscribe(event -> isCalled.set(true));
                    return null;
                });
        context.activate();
        isCalled.set(false);
        context.deactivate();

        MockConnectionContext otherContext = new MockConnectionContext();
        collaborationEngine.openTopicConnection(otherContext, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map").put("bar", "baz");
                    return null;
                });
        otherContext.activate();

        Assert.assertFalse(
                "Expect the deactivated connection to not notify its subscriber",
                isCalled.get());
    }

    WeakReference<MapSubscriber> weakSubscriber;

    @Test
    public void deactivateConnection_garbageCollectedTheTopicSubscriber()
            throws InterruptedException {
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    MapSubscriber subscriber = new MapSubscriber() {
                        @Override
                        public void onMapChange(MapChangeEvent event) {
                            // nop
                        }
                    };
                    weakSubscriber = new WeakReference<>(subscriber);
                    topicConnection.getNamedMap("map").subscribe(subscriber);
                    subscriber = null;
                    return null;
                });
        context.activate();
        context.deactivate();
        Assert.assertTrue(
                "Expect subscriber to be garbage-collected when connection is deactivated",
                TestUtils.isGarbageCollected(this.weakSubscriber));
    }

    @Test
    public void deactivatedConnection_triggerConnectionCallback() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topicConnection.getNamedMap("map").subscribe(event -> {
                    });
                    return () -> isCalled.set(true);
                });
        context.activate();
        context.deactivate();
        Assert.assertTrue(
                "Expect the returned Registration of connection callback to be called when deactivated",
                isCalled.get());
    }

    @Test
    public void reactivatedConnection_triggerConnectionCallbackAgain() {
        AtomicBoolean isCalled = new AtomicBoolean(false);
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    isCalled.set(true);
                    topicConnection.getNamedMap("").subscribe(event -> {
                    });
                    return null;
                });
        context.activate();
        context.deactivate();
        isCalled.set(false);

        context.activate();
        Assert.assertTrue(
                "Expect the returned Registration of connection callback to be called when deactivated",
                isCalled.get());
    }

    @Test
    public void serializeHandler() {
        ActivationHandler handler = context.getActivationHandler();

        ActivationHandler deserializedHandler = TestUtils.serialize(handler);
    }
}
