package com.vaadin.collaborationengine;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * A {@link VaadinServiceInitListener} which uses the {@link Instantiator} to
 * find whether an instance of {@link CollaborationEngineConfiguration} is
 * provided as a bean in the current environment, using that instance if found.
 */
public class CollaborationEngineServiceInitListener
        implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        Instantiator instantiator = service.getInstantiator();
        try {
            CollaborationEngineConfiguration configuration = instantiator
                    .getOrCreate(CollaborationEngineConfiguration.class);
            CollaborationEngine.configure(service, configuration);
        } catch (Exception e) {
            // No bean could be automatically found, ignore the exception.
            // If no configuration is set, a warning will be logged when
            // calling CollaborationEngine.getInstance() suggesting to
            // provide a bean if using Spring or CDI.
        }
    }
}
