package org.vaadin.collaborationengine.it.util;

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
