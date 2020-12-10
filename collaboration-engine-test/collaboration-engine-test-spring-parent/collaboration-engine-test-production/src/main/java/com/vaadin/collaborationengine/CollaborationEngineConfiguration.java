package com.vaadin.collaborationengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * Configuration class for the Collaboration Engine instance.
 */
@SpringComponent
public class CollaborationEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CollaborationEngineConfiguration.class);

    public CollaborationEngineConfiguration() {
        CollaborationEngine.getInstance().setLicenseEventHandler(event -> {
            LOGGER.error(event.getMessage());
        });
    }
}
