/*
 */

package com.vaadin.collaborationengine;

import jakarta.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;

public class MyVaadinInitListener {

    static final Logger LOGGER = LoggerFactory
            .getLogger(MyVaadinInitListener.class);

    public void serviceInit(@Observes ServiceInitEvent serviceEvent) {
        VaadinService service = serviceEvent.getSource();
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration();
        CollaborationEngine.configure(service, configuration);
    }
}
