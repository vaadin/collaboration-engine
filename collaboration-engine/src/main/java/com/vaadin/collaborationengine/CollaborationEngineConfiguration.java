package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Configuration object for {@link CollaborationEngine}. When running in
 * production mode, it is required to set this configuration for the
 * Collaboration Engine associated with the current {@link VaadinService}. This
 * should be done by calling
 * {@link CollaborationEngine#configure(VaadinService, CollaborationEngineConfiguration)}
 * in a {@link VaadinServiceInitListener}.
 */
public class CollaborationEngineConfiguration {

    private LicenseEventHandler licenseEventHandler;
    private VaadinService vaadinService;

    /**
     * Creates a new Collaboration Engine configuration with the provided
     * handler for license events.
     * <p>
     * The handler will be invoked when license events occur, e.g. when the
     * license is expired or when the end-user quota has entered the grace
     * period. The handler can then be used for example to forward these events
     * via e-mail or to a monitoring application to be alerted about the current
     * status of the license.
     * <p>
     * See {@link LicenseEventType} for a list of license event types.
     *
     * @param licenseEventHandler
     *            the license event handler, not {@code null}
     */
    public CollaborationEngineConfiguration(
            LicenseEventHandler licenseEventHandler) {
        this.licenseEventHandler = Objects.requireNonNull(licenseEventHandler,
                "The license event handler cannot be null");
    }

    /**
     * Gets the license event handler of this configuration.
     *
     * @return the license event handler
     */
    public LicenseEventHandler getLicenseEventHandler() {
        return licenseEventHandler;
    }

    void setVaadinService(VaadinService vaadinService) {
        this.vaadinService = vaadinService;
    }

    boolean isLicenseCheckingEnabled() {
        return vaadinService.getDeploymentConfiguration().isProductionMode();
    }

    Path getDataDirPath() {
        String dataDirectory = vaadinService.getDeploymentConfiguration()
                .getStringProperty(FileHandler.DATA_DIR_CONFIG_PROPERTY, null);
        return dataDirectory != null ? Paths.get(dataDirectory) : null;
    }
}
