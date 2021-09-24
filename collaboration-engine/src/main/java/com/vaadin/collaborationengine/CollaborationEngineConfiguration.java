package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Configuration object for {@link CollaborationEngine}. When running in
 * production mode, it is required to set this configuration for the
 * Collaboration Engine associated with the current {@link VaadinService}. This
 * should be done by calling
 * {@link CollaborationEngine#configure(VaadinService, CollaborationEngineConfiguration)}
 * in a {@link VaadinServiceInitListener}.
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public class CollaborationEngineConfiguration {

    static final boolean DEFAULT_AUTOMATICALLY_ACTIVATE_PUSH = true;

    private LicenseEventHandler licenseEventHandler;
    private VaadinService vaadinService;
    private String configuredDataDir;
    private boolean automaticallyActivatePush = DEFAULT_AUTOMATICALLY_ACTIVATE_PUSH;

    private Backend backend = new LocalBackend();

    private ExecutorService executorService;

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
     *
     * @since 3.0
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
     *
     * @since 3.0
     */
    public LicenseEventHandler getLicenseEventHandler() {
        return licenseEventHandler;
    }

    /**
     * Gets the configured data-directory.
     *
     * @return the data-directory
     *
     * @since 3.0
     */
    public String getDataDir() {
        return configuredDataDir;
    }

    /**
     * Sets the path to the data-directory, which is used by Collaboration
     * Engine to store files.
     * <p>
     * The data-directory can also be configured by setting the
     * {@code vaadin.ce.dataDir} system property either in the command line or
     * with {@link System#setProperty(String, String)}. If a system property is
     * set, it will take precedence over this setting.
     *
     * @param dataDir
     *            path to the data-directory
     *
     * @since 3.0
     */
    public void setDataDir(String dataDir) {
        configuredDataDir = dataDir;
    }

    /**
     * Sets whether server push should be automatically activated if needed.
     * When enabled, which is the default, Collaboration Engine will
     * automatically activate {@link PushMode#AUTOMATIC} if neither push nor
     * polling is active for a UI where CollaborationEngine is used. When
     * disabled, no automatic changes are made to the application's push
     * configuration.
     *
     * @param automaticallyActivatePush
     *            <code>true</code> to automatically activate server push if
     *            needed, <code>false</code> to not make any automatic changes
     *            to the push configuration
     *
     * @since 3.0
     */
    public void setAutomaticallyActivatePush(
            boolean automaticallyActivatePush) {
        this.automaticallyActivatePush = automaticallyActivatePush;
    }

    /**
     * Checks whether automatic push activation is enabled.
     *
     * @see #setAutomaticallyActivatePush(boolean)
     *
     * @return <code>true</code> if automatic server push configuration is
     *         enabled, <code>false</code> if it's no enabled
     *
     * @since 3.0
     */
    public boolean isAutomaticallyActivatePush() {
        return automaticallyActivatePush;
    }

    void setVaadinService(VaadinService vaadinService) {
        this.vaadinService = vaadinService;
    }

    boolean isLicenseCheckingEnabled() {
        return vaadinService.getDeploymentConfiguration().isProductionMode();
    }

    /**
     * Sets the backend implementation to use. A backend can be used to
     * distribute changes between multiple nodes in a cluster. By default, a
     * local in-memory backend is used.
     *
     * @param backend
     *            the backend to use, not <code>null</code>
     */
    public void setBackend(Backend backend) {
        this.backend = Objects.requireNonNull(backend);
    }

    /**
     * Gets the configured backend implementation.
     *
     * @see #setBackend(Backend)
     * @return the backend implementation, not <code>null</code>
     */
    public Backend getBackend() {
        return backend;
    }

    /**
     * Gets the configured {@link ExecutorService} which will be used to
     * dispatch actions asynchronously. A custom executor service can be
     * configured with {@link #setExecutorService(ExecutorService)}.
     *
     * @return the configured executor service, or <code>null</code> if not set
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Sets the {@link ExecutorService} which will be used to dispatch actions
     * asynchronously. An executor service set with this method won't be
     * shutdown automatically, so the developer should take care of that if
     * needed. If not configured, Collaboration Engine will use a thread pool
     * with a fixed number of threads equal to the number of available
     * processors and will take care of shutting it down.
     *
     * @param executorService
     *            the executor service, or <code>null</code> to remove a
     *            previously configured one
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    Path getDataDirPath() {
        String dataDirectory = vaadinService.getDeploymentConfiguration()
                .getStringProperty(FileHandler.DATA_DIR_CONFIG_PROPERTY, null);
        if (dataDirectory == null) {
            dataDirectory = configuredDataDir;
        }
        return dataDirectory != null ? Paths.get(dataDirectory) : null;
    }
}
