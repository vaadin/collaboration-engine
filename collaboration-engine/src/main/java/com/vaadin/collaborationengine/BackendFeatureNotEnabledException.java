/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
