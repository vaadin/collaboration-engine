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

/**
 * Exception thrown if the feature-flag for the {@link Backend} API is not
 * enabled.
 *
 * @author Vaadin Ltd
 */
public class BackendFeatureNotEnabledException extends RuntimeException {

    BackendFeatureNotEnabledException() {
        super("The Backend API is currently an experimental feature and needs "
                + "to be explicitly enabled. The feature can be enabled "
                + "using the Vaadin dev-mode Gizmo, in the experimental "
                + "features tab, or by adding a "
                + "`src/main/resources/vaadin-featureflags.properties` file "
                + "with the following content: "
                + "`com.vaadin.experimental.collaborationEngineBackend=true`");
    }
}
