/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
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
