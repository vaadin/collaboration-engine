package com.vaadin.collaborationengine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    static TestCollaborationEngine createTestCollaborationEngine(
            VaadinService service, ExecutorService executor) {
        TestCollaborationEngine ce = new TestCollaborationEngine();
        MockConfiguration configuration = new MockConfiguration(e -> {
        });
        configuration.setExecutorService(executor);
        configuration.setLicenseCheckingEnabled(false);
        configureTestCollaborationEngine(service, ce, configuration);
        return ce;
    }

    static TestCollaborationEngine createTestCollaborationEngine(
            VaadinService service,
            CollaborationEngineConfiguration configuration) {
        TestCollaborationEngine ce = new TestCollaborationEngine();
        configureTestCollaborationEngine(service, ce, configuration);
        return ce;
    }

    static void configureTestCollaborationEngine(VaadinService service,
            CollaborationEngine ce) {
        MockConfiguration configuration = new MockConfiguration(e -> {
        });
        configuration.setLicenseCheckingEnabled(false);
        configureTestCollaborationEngine(service, ce, configuration);
    }

    static void configureTestCollaborationEngine(VaadinService service,
            CollaborationEngine ce,
            CollaborationEngineConfiguration configuration) {
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

        public void setAsynchronous(boolean asynchronous) {
            this.asynchronous = asynchronous;
        }

        @Override
        public ExecutorService getExecutorService() {
            return new AbstractExecutorService() {

                @Override
                public void execute(Runnable command) {
                    if (TestCollaborationEngine.this.asynchronous) {
                        TestCollaborationEngine.super.getExecutorService()
                                .execute(command);
                    } else {
                        command.run();
                    }
                }

                @Override
                public List<Runnable> shutdownNow() {
                    return TestCollaborationEngine.super.getExecutorService()
                            .shutdownNow();
                }

                @Override
                public void shutdown() {
                    TestCollaborationEngine.super.getExecutorService()
                            .shutdown();
                }

                @Override
                public boolean isTerminated() {
                    return TestCollaborationEngine.super.getExecutorService()
                            .isTerminated();
                }

                @Override
                public boolean isShutdown() {
                    return TestCollaborationEngine.super.getExecutorService()
                            .isShutdown();
                }

                @Override
                public boolean awaitTermination(long timeout, TimeUnit unit)
                        throws InterruptedException {
                    return TestCollaborationEngine.super.getExecutorService()
                            .awaitTermination(timeout, unit);
                }
            };
        }
    }
}
