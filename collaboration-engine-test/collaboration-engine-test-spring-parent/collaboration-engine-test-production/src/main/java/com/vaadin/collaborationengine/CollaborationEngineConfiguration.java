package com.vaadin.collaborationengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * Configuration class for the Collaboration Engine instance.
 */
@SpringComponent
public class CollaborationEngineConfiguration
        implements VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CollaborationEngineConfiguration.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        CollaborationEngine.getInstance(service).setLicenseEventHandler(e -> {
            LOGGER.error(e.getMessage());
        });
    }
}
