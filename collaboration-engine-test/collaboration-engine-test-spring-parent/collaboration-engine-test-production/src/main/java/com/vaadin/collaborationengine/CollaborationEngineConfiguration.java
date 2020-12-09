package com.vaadin.collaborationengine;

import org.springframework.stereotype.Component;

/**
 * Configuration class for the Collaboration Engine instance.
 */
@Component
public class CollaborationEngineConfiguration {

    public CollaborationEngineConfiguration() {
        CollaborationEngine.getInstance().setLicenseEventHandler(event -> {
        });
    }
}
