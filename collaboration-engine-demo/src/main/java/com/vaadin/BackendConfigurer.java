/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationEngineConfiguration;
import com.vaadin.collaborationengine.LicenseEventHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class BackendConfigurer implements VaadinServiceInitListener {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(BackendConfigurer.class);

    @Override
    public void serviceInit(ServiceInitEvent serviceEvent) {
        VaadinService service = serviceEvent.getSource();

        LicenseEventHandler licenseEventHandler = licenseEvent -> {
            switch (licenseEvent.getType()) {
            case GRACE_PERIOD_STARTED:
            case LICENSE_EXPIRES_SOON:
                LOGGER.warn(licenseEvent.getMessage());
                break;
            case GRACE_PERIOD_ENDED:
            case LICENSE_EXPIRED:
                LOGGER.error(licenseEvent.getMessage());
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unexpected license event type: "
                                + licenseEvent.getType());
            }
        };

        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                licenseEventHandler);

        if ("hazelcast".equals(System.getProperty("ce.clustering"))) {
            HazelcastInstance hz = Hazelcast.newHazelcastInstance();

            service.addServiceDestroyListener(event -> hz.shutdown());

            configuration.setBackend(new HazelcastBackend(hz));
        }

        CollaborationEngine.configure(service, configuration);
    }

}
