package com.vaadin.collaborationengine.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.AbstractTheme;

public class MockService extends VaadinService {

    static class MockContext implements VaadinContext {

        private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
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
    public URL getResource(String url, WebBrowser browser,
            AbstractTheme theme) {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String url, WebBrowser browser,
            AbstractTheme theme) {
        return null;
    }

    @Override
    public String resolveResource(String url, WebBrowser browser) {
        return null;
    }

    @Override
    public Optional<String> getThemedUrl(String url, WebBrowser browser,
            AbstractTheme theme) {
        return null;
    }

    @Override
    protected VaadinContext constructVaadinContext() {
        return new MockContext();
    }

    @Override
    public void destroy() {
        try {
            /*
             * super.destroy() will try to remove this registration and throws
             * NPE if it isn't initialized. That is usually done by init(), but
             * being able to run that method to set it up would require mocking
             * quite many other parts as well.
             */
            Field registrationField = VaadinService.class.getDeclaredField(
                    "htmlImportDependencyCacheClearRegistration");
            registrationField.setAccessible(true);
            registrationField.set(this, (Registration) () -> {
            });
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        super.destroy();
    }
}
