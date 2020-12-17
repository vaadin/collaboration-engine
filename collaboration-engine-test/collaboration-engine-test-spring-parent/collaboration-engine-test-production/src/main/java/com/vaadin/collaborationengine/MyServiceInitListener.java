package com.vaadin.collaborationengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * Configures the Collaboration Engine instance.
 */
@SpringComponent
public class MyServiceInitListener implements VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MyServiceInitListener.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        CollaborationEngine.configure(service,
                new CollaborationEngineConfiguration(e -> {
                    LOGGER.error(e.getMessage());
                }));
    }
}
