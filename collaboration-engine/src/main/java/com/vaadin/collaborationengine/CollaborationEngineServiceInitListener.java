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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * A {@link VaadinServiceInitListener} which uses the {@link Instantiator} to
 * find whether an instance of {@link CollaborationEngineConfiguration} is
 * provided as a bean in the current environment, using that instance if found.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class CollaborationEngineServiceInitListener
        implements VaadinServiceInitListener {

    private static final List<Consumer<VaadinService>> reinitializers = new ArrayList<>();

    public static void addReinitializer(Consumer<VaadinService> reinitializer) {
        reinitializers.add(reinitializer);
    }

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

        event.addRequestHandler((session, request, response) -> {
            VaadinService requestService = request.getService();
            if (requestService != null && !reinitializers.isEmpty()) {
                synchronized (reinitializers) {
                    for (Consumer<VaadinService> reinitializer : reinitializers) {
                        reinitializer.accept(requestService);
                    }
                    reinitializers.clear();
                }
            }
            return false;
        });
    }
}
