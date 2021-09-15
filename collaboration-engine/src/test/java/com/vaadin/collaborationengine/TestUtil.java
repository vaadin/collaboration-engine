package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.util.concurrent.Executor;

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

    static TestCollaborationEngine createTestCollaborationEngine() {
        return createTestCollaborationEngine(new MockService());
    }

    static TestCollaborationEngine createTestCollaborationEngine(
            VaadinService service) {
        TestCollaborationEngine ce = new TestCollaborationEngine();
        configureTestCollaborationEngine(service, ce);
        return ce;
    }

    static void configureTestCollaborationEngine(VaadinService service,
            CollaborationEngine ce) {
        MockConfiguration configuration = new MockConfiguration(e -> {
        });
        configuration.setLicenseCheckingEnabled(false);
        CollaborationEngine.configure(service, configuration, ce, true);
    }

    static class TestCollaborationEngine extends CollaborationEngine {

        private volatile boolean asynchronous;

        public TestCollaborationEngine() {
        }

        public TestCollaborationEngine(
                TopicActivationHandler topicActivationHandler) {
            super(topicActivationHandler);
        }

        @Override
        public Executor getExecutorService() {
            return runnable -> {
                if (asynchronous) {
                    super.getExecutorService().execute(runnable);
                } else {
                    runnable.run();
                }
            };
        }

        public void setAsynchronous(boolean asynchronous) {
            this.asynchronous = asynchronous;
        }
    }
}
