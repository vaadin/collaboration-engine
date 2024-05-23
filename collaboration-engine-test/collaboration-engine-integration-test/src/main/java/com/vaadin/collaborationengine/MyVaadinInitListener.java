/*
 */

package com.vaadin.collaborationengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class MyVaadinInitListener implements VaadinServiceInitListener {

    static final Logger LOGGER = LoggerFactory
            .getLogger(MyVaadinInitListener.class);

    @Override
    public void serviceInit(ServiceInitEvent serviceEvent) {
        VaadinService service = serviceEvent.getSource();
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration();
        CollaborationEngine.configure(service, configuration);
    }
}
