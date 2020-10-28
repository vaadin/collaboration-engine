package com.vaadin.collaborationengine.util;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Tag;

@Tag("generic-test-field")
public class GenericTestField<T> extends AbstractField<GenericTestField<T>, T> {

    public GenericTestField() {
        super(null);
    }

    @Override
    protected void setPresentationValue(T newPresentationValue) {
        // nop
    }
}
