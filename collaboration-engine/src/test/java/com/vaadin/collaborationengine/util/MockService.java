package com.vaadin.collaborationengine.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class MockService extends VaadinService {

    class MockApplicationConfiguration implements ApplicationConfiguration {
        boolean productionMode;

        public MockApplicationConfiguration(boolean productionMode) {
            this.productionMode = productionMode;
        }

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
            return MockService.super.getContext();
        }

        @Override
        public boolean isDevModeSessionSerializationEnabled() {
            return false;
        }

        @Override
        public boolean isProductionMode() {
            return productionMode;
        }

        @Override
        public String getStringProperty(String s, String s1) {
            return s1;
        }

        @Override
        public boolean getBooleanProperty(String s, boolean b) {
            return false;
        }
    }

    static class MockContext implements VaadinContext {

        private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            if (Lookup.class.isAssignableFrom(type)) {
                return type.cast(Lookup.of(this));
            }
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

    static class MockInstantiator extends DefaultInstantiator {

        final Map<Class<?>, Object> instances = new HashMap<>();

        public MockInstantiator(VaadinService service) {
            super(service);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getOrCreate(Class<T> type) {
            return (T) instances.computeIfAbsent(type, super::getOrCreate);
        }
    }

    private DeploymentConfiguration deploymentConfiguration;
    private ApplicationConfiguration applicationConfiguration;
    private Instantiator instantiator;

    public MockService(boolean productionMode) {
        applicationConfiguration = new MockApplicationConfiguration(
                productionMode);
        deploymentConfiguration = new DefaultDeploymentConfiguration(
                applicationConfiguration, Object.class, new Properties()) {
        };
        instantiator = new MockInstantiator(this);
    }

    public MockService() {
        this(false);
    }

    @Override
    public Instantiator getInstantiator() {
        return instantiator;
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
        MockContext context = new MockContext();
        context.setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);
        return context;
    }
}
