package com.vaadin.collaborationengine;

import java.nio.file.Path;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.VaadinService;

public class TestUtil {

    public static class MockConfiguration
            extends CollaborationEngineConfiguration {

        private boolean licenseCheckingEnabled;
        private Path dataDirPath;

        private boolean licenseCheckingEnabledMocked;
        private boolean dataDirPathMocked;

        public MockConfiguration(LicenseEventHandler licenseEventHandler) {
            super(licenseEventHandler);
        }

        @Override
        boolean isLicenseCheckingEnabled() {
            if (licenseCheckingEnabledMocked) {
                return licenseCheckingEnabled;
            } else {
                return super.isLicenseCheckingEnabled();
            }
        }

        @Override
        Path getDataDirPath() {
            if (dataDirPathMocked) {
                return dataDirPath;
            } else {
                return super.getDataDirPath();
            }
        }

        public void setLicenseCheckingEnabled(boolean licenseCheckingEnabled) {
            this.licenseCheckingEnabled = licenseCheckingEnabled;
            this.licenseCheckingEnabledMocked = true;
        }

        public void setDataDirPath(Path dataDirPath) {
            this.dataDirPath = dataDirPath;
            this.dataDirPathMocked = true;
        }
    }

    static CollaborationEngine createTestCollaborationEngine() {
        return createTestCollaborationEngine(new MockService());
    }

    static CollaborationEngine createTestCollaborationEngine(
            VaadinService service) {
        return configureTestCollaborationEngine(service,
                new CollaborationEngine());
    }

    static CollaborationEngine configureTestCollaborationEngine(
            VaadinService service, CollaborationEngine ce) {
        MockConfiguration configuration = new MockConfiguration(e -> {
        });
        configuration.setLicenseCheckingEnabled(false);
        return CollaborationEngine.configure(service, configuration, ce);
    }
}
