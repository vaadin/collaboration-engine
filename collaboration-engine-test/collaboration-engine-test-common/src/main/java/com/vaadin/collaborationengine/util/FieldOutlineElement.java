package com.vaadin.collaborationengine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-field-outline")
public class FieldOutlineElement extends TestBenchElement {

    public Integer getColorIndex() {
        String color = (String) executeScript(
                "return arguments[0].style.getPropertyValue('--_active-user-color')",
                this);
        if (color != null && color.contains("--vaadin-user-color")) {
            Matcher matcher = Pattern.compile("\\d+").matcher(color);
            matcher.find();
            return Integer.valueOf(matcher.group());
        }
        return null;
    }
}
