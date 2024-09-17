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

import static org.hamcrest.CoreMatchers.startsWith;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.TestUtils;

public class BeaconHandlerTest {
    @Test
    public void createBeaconUrl_pathUsedCorrectly() {
        BeaconHandler beaconHandler = new BeaconHandler("/foo");
        Assert.assertThat(beaconHandler.createBeaconUrl(),
                startsWith("./foo?"));
    }

    @Test
    public void serializeBeaconHandler() {
        BeaconHandler beaconHandler = new BeaconHandler("/foo");

        BeaconHandler deserializedBeaconHandler = TestUtils
                .serialize(beaconHandler);
    }
}
