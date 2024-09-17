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

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Functional interface that defines how to handle highlight changes for
 * properties in a topic.
 *
 * @see FormManager#setHighlightHandler(HighlightHandler)
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface HighlightHandler extends Serializable {
    /**
     * The context of the highlight.
     */
    interface HighlightContext extends Serializable {
        /**
         * Gets the user.
         *
         * @return the user, not {@code null}
         */
        UserInfo getUser();

        /**
         * Gets the property name.
         *
         * @return the property name, not {@code null}
         */
        String getPropertyName();

        /**
         * Gets the field index.
         *
         * @return the field index
         */
        int getFieldIndex();
    }

    /**
     * Handles a change of highlight for a property in a topic.
     *
     * @param context
     *            the context of the highlight, not {@code null}
     * @return a registration that will be removed with the highlight is toggled
     *         off
     */
    Registration handleHighlight(HighlightContext context);
}
