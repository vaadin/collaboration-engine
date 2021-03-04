package com.vaadin.collaborationengine.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class MockService extends VaadinService {

    static class MockContext implements VaadinContext {

        private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            if (defaultValueSupplier == null) {
                return (T) context.get(type);
            }
            return (T) context.computeIfAbsent(type,
                    k -> defaultValueSupplier.get());
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            context.put(clazz, value);
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            context.remove(clazz);
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    }

    private DeploymentConfiguration deploymentConfiguration;

    public MockService(boolean productionMode) {
        deploymentConfiguration = new DefaultDeploymentConfiguration(
                new ApplicationConfiguration() {
                    @Override
                    public Enumeration<String> getPropertyNames() {
                        return new Enumeration<String>() {
                            @Override
                            public boolean hasMoreElements() {
                                return false;
                            }

                            @Override
                            public String nextElement() {
                                return null;
                            }
                        };
                    }

                    @Override
                    public VaadinContext getContext() {
                        return null;
                    }

                    @Override
                    public FallbackChunk getFallbackChunk() {
                        return null;
                    }

                    @Override
                    public boolean isProductionMode() {
                        return productionMode;
                    }

                    @Override
                    public String getStringProperty(String s, String s1) {
                        return null;
                    }

                    @Override
                    public boolean getBooleanProperty(String s, boolean b) {
                        return false;
                    }
                }, Object.class, new Properties()) {
        };
    }

    public MockService() {
        this(false);
    }

    @Override
    public DeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return null;
    }

    @Override
    protected PwaRegistry getPwaRegistry() {
        return null;
    }

    @Override
    public String getContextRootRelativePath(VaadinRequest request) {
        return null;
    }

    @Override
    public String getMimeType(String resourceName) {
        return null;
    }

    @Override
    protected boolean requestCanCreateSession(VaadinRequest request) {
        return false;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        return null;
    }

    @Override
    public URL getStaticResource(String url) {
        return null;
    }

    @Override
    public URL getResource(String url) {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String url) {
        return null;
    }

    @Override
    public String resolveResource(String url) {
        return null;
    }

    @Override
    protected VaadinContext constructVaadinContext() {
        return new MockContext();
    }
}
