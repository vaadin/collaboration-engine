/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.collaborationengine.CollaborationEngine.CollaborationEngineConfig;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * VaadinServiceInitListener which reads Collaboration Engine parameters and
 * configures Collaboration Engine accordingly.
 *
 * @author Vaadin Ltd
 */
public class CollaborationEngineServiceInitListener
        implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        DeploymentConfiguration config = event.getSource()
                .getDeploymentConfiguration();
        CollaborationEngine ce = new CollaborationEngine();
        VaadinContext context = event.getSource().getContext();
        context.setAttribute(CollaborationEngine.class, ce);
        ce.setConfigProvider(() -> {
            boolean licenseCheckingEnabled = config.isProductionMode();
            String dataDirectory = config.getStringProperty(
                    FileHandler.DATA_DIR_CONFIG_PROPERTY, null);
            Path dataDirPath = dataDirectory != null ? Paths.get(dataDirectory)
                    : null;
            return new CollaborationEngineConfig(licenseCheckingEnabled,
                    dataDirPath);
        });
    }
}
