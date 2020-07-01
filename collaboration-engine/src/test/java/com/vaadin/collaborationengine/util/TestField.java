package com.vaadin.collaborationengine.util;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Tag;

@Tag("test-field")
public class TestField extends AbstractField<TestField, String> {

    public TestField() {
        super(null);
    }

    @Override
    protected void setPresentationValue(String newPresentationValue) {
        // NO-OP
    }

    @Override
    public String getEmptyValue() {
        return "empty value";
    }

    @Override
    public boolean hasListener(Class<? extends ComponentEvent> eventType) {
        return super.hasListener(eventType);
    }
}
