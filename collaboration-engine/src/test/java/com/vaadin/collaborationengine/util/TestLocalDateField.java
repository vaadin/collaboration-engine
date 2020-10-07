package com.vaadin.collaborationengine.util;

import java.time.LocalDate;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Tag;

@Tag("test-local-date-field")
public class TestLocalDateField
        extends AbstractField<TestLocalDateField, LocalDate>
        implements Focusable<TestLocalDateField> {

    public TestLocalDateField() {
        super(null);
    }

    @Override
    protected void setPresentationValue(LocalDate newPresentationValue) {
        // no impl
    }
}
