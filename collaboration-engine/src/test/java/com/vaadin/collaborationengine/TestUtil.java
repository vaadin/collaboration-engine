/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.server.VaadinService;

public class TestUtil {

    private static ReferenceQueue<VaadinService> serviceReferenceQueue = new ReferenceQueue<>();
    private static Map<WeakReference<VaadinService>, ExecutorService> executorServices = new ConcurrentHashMap<>();

    /*
     * Wait for VaadinService instances to be garbage collected and shut down
     * the corresponding CE executor service.
     */
    private static Thread executorServiceShutdownThread = new Thread(() -> {
        while (true) {
            try {
                Reference<? extends VaadinService> collectedServiceReference = serviceReferenceQueue
                        .remove();
                executorServices.remove(collectedServiceReference).shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    });
    static {
        executorServiceShutdownThread.setDaemon(true);
        executorServiceShutdownThread.start();
    }

    public static class MockConfiguration
            extends CollaborationEngineConfiguration {

        private String beaconPath;

        private boolean backendFeatureCheckingEnabled;
        private boolean beaconPathMocked;

        @Override
        void requireBackendFeatureEnabled() {
            if (backendFeatureCheckingEnabled) {
                super.requireBackendFeatureEnabled();
            }
        }

        @Override
        String getBeaconPathProperty() {
            if (beaconPathMocked) {
                return beaconPath;
            } else {
                return super.getBeaconPathProperty();
            }
        }

        public void setBackendFeatureCheckingEnabled(
                boolean backendFeatureCheckingEnabled) {
            this.backendFeatureCheckingEnabled = backendFeatureCheckingEnabled;
        }

        public void setBeaconPathProperty(String beaconPath) {
            this.beaconPath = beaconPath;
            this.beaconPathMocked = true;
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
        MockConfiguration configuration = new MockConfiguration();
        configuration.setExecutorService(executor);
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
        MockConfiguration configuration = new MockConfiguration();
        configureTestCollaborationEngine(service, ce, configuration);
    }

    static void configureTestCollaborationEngine(VaadinService service,
            CollaborationEngine ce,
            CollaborationEngineConfiguration configuration) {
        CollaborationEngine.configure(service, configuration, ce, true);

        /*
         * Hook up the executor service to be shut down when the VaadinService
         * instance is garbage collected
         */
        executorServices.put(
                new WeakReference<>(service, serviceReferenceQueue),
                getRealExecutorService(ce));
    }

    private static ExecutorService getRealExecutorService(
            CollaborationEngine ce) {
        if (ce instanceof TestCollaborationEngine) {
            /*
             * Get the underlying executor since the wrapper has a back
             * reference to the CE instance which would prevent it from being
             * garbage collected
             */
            return ((TestCollaborationEngine) ce)
                    .getUnderlyingExecutorService();
        } else {
            return ce.getExecutorService();
        }
    }

    static class TestCollaborationEngine extends CollaborationEngine
            implements Serializable {

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

        public ExecutorService getUnderlyingExecutorService() {
            return super.getExecutorService();
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
