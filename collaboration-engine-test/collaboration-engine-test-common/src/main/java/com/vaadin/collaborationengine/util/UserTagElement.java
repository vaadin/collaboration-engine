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

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-user-tag")
public class UserTagElement extends TestBenchElement {

    /**
     * Gets the user name that is set for the tag.
     */
    public String getName() {
        return getPropertyString("name");
    }

    /**
     * Gets the color index that is set for the tag, used for the user color.
     */
    public Integer getColorIndex() {
        boolean isNumber = (boolean) executeScript(
                "return (typeof arguments[0].colorIndex) === 'number'", this);
        if (!isNumber) {
            return null;
        }
        return getPropertyInteger("colorIndex");
    }
}
