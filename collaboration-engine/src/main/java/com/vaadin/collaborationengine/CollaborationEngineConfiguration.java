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

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import com.vaadin.experimental.FeatureFlags;
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

    /**
     * When querying properties from Vaadin's
     * {@link com.vaadin.flow.function.DeploymentConfiguration}, they are looked
     * within the `vaadin.` namespace. When querying, we should therefore not
     * include the prefix. However, when instructing people on how to set the
     * parameter, we should include the prefix.
     */
    static final String BEACON_PATH_CONFIG_PROPERTY = "ce.beaconPath";
    static final String DEFAULT_BEACON_PATH = "/";

    static final boolean DEFAULT_AUTOMATICALLY_ACTIVATE_PUSH = true;

    static final int DEFAULT_EVENT_LOG_SUBSCRIBE_RETRY_ATTEMPTS = 40;

    private LicenseEventHandler licenseEventHandler;
    private VaadinService vaadinService;
    private String configuredDataDir;
    private String configuredBeaconPath = DEFAULT_BEACON_PATH;
    private boolean automaticallyActivatePush = DEFAULT_AUTOMATICALLY_ACTIVATE_PUSH;

    private Backend backend = new LocalBackend();

    private ExecutorService executorService;

    private LicenseStorage licenseStorage;

    /**
     * Creates a new Collaboration Engine configuration with the provided
     * handler for license events.
     * <p>
     * This constructor is deprecated and the provided handler won't receive any
     * events.
     *
     * @param licenseEventHandler
     *            the license event handler, not {@code null}
     *
     * @since 3.0
     * @deprecated the provided handler won't receive any events, please prefer
     *             using the default constructor with no parameters
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public CollaborationEngineConfiguration(
            LicenseEventHandler licenseEventHandler) {
        this.licenseEventHandler = Objects.requireNonNull(licenseEventHandler,
                "The license event handler cannot be null");
    }

    /**
     * Creates a new Collaboration Engine configuration.
     */
    public CollaborationEngineConfiguration() {
        // This default constructor does nothing, it is explicitly declared
        // until the deprecated constructor above is removed
    }

    /**
     * Gets the license event handler of this configuration.
     *
     * @return the license event handler
     *
     * @since 3.0
     * @deprecated the handler is not used since 6.3
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public LicenseEventHandler getLicenseEventHandler() {
        return licenseEventHandler;
    }

    /**
     * Gets the configured data-directory.
     *
     * @return the data-directory
     *
     * @since 3.0
     * @deprecated the data-directory is not used since 6.3
     */
    @Deprecated(since = "6.3", forRemoval = true)
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
     * @deprecated the data-directory is not used since 6.3
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public void setDataDir(String dataDir) {
        configuredDataDir = dataDir;
    }

    /**
     * Gets the configured beacon path.
     *
     * @return the beacon path
     */
    public String getBeaconPath() {
        return configuredBeaconPath;
    }

    /**
     * Sets the path that is used for the beacon handler. This is used to detect
     * when the user has closed a tab.
     * <p>
     * The beacon path can also be configured by setting the
     * {@code vaadin.ce.beaconPath} system property either in the command line
     * or with {@link System#setProperty(String, String)}. If a system property
     * is set, it will take precedence over this setting.
     *
     * @param beaconPath
     *            path used by the beacon handler
     */
    public void setBeaconPath(String beaconPath) {
        configuredBeaconPath = beaconPath;
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
        requireBackendFeatureEnabled();
    }

    void requireBackendFeatureEnabled() {
        if (vaadinService != null
                && !backend.getClass().equals(LocalBackend.class)
                && !FeatureFlags.get(vaadinService.getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND)) {
            throw new BackendFeatureNotEnabledException();
        }
    }

    /**
     * Sets the backend implementation to use. A backend can be used to
     * distribute changes between multiple nodes in a cluster. By default, a
     * local in-memory backend is used.
     * <p>
     * This is currently an experimental feature and needs to be explicitly
     * enabled using the Vaadin dev-mode Gizmo, in the experimental features
     * tab, or by adding a
     * <code>src/main/resources/vaadin-featureflags.properties</code> file with
     * the following content:
     * <code>com.vaadin.experimental.collaborationEngineBackend=true</code>
     *
     * @param backend
     *            the backend to use, not <code>null</code>
     */
    public void setBackend(Backend backend) {
        this.backend = Objects.requireNonNull(backend);
        requireBackendFeatureEnabled();
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
     * Gets the configured license-storage implementation.
     *
     * @return the license-storage implementation, or <code>null</code> if not
     *         configured
     * @deprecated license storage is not needed since 6.3
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public LicenseStorage getLicenseStorage() {
        return licenseStorage;
    }

    /**
     * Sets a configured license-storage implementation.
     *
     * @param licenseStorage
     *            the license-storage implementation, or <code>null</code> to
     *            unset
     * @deprecated license storage is not needed since 6.3
     */
    @Deprecated(since = "6.3", forRemoval = true)
    public void setLicenseStorage(LicenseStorage licenseStorage) {
        this.licenseStorage = licenseStorage;
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

    String getBeaconPathProperty() {
        String beaconPath = vaadinService.getDeploymentConfiguration()
                .getStringProperty(BEACON_PATH_CONFIG_PROPERTY, null);
        if (beaconPath == null) {
            beaconPath = configuredBeaconPath;
        }
        return beaconPath;
    }

    int getEventLogSubscribeRetryAttempts() {
        return DEFAULT_EVENT_LOG_SUBSCRIBE_RETRY_ATTEMPTS;
    }
}
