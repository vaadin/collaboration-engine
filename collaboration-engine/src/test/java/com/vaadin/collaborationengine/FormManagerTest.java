package com.vaadin.collaborationengine;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

public class FormManagerTest {

    private static final String TOPIC_ID = "form";

    private UserInfo user = new UserInfo("foo");

    private CollaborationEngine ce;

    @Before
    public void init() {
        VaadinService service = new MockService();
        VaadinService.setCurrent(service);
        ce = TestUtil.createTestCollaborationEngine(service);
    }

    @After
    public void cleanUp() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    @Test
    public void setValue_getValueIsCorrect() {
        FormManager manager = createActiveManager();
        manager.setValue("foo", "bar");

        Assert.assertEquals("bar", manager.getValue("foo", String.class));
    }

    @Test
    public void setHandler_setValue_handlerInvoked() {
        Map<String, Object> entries = new HashMap<>();
        FormManager manager = createActiveManager();

        manager.setPropertyChangeHandler(event -> entries
                .put(event.getPropertyName(), event.getValue()));
        manager.setValue("foo", "bar");

        Assert.assertTrue(entries.containsKey("foo"));
        Assert.assertEquals("bar", entries.get("foo"));
    }

    @Test
    public void setValue_setHandler_handlerInvoked() {
        Map<String, Object> entries = new HashMap<>();
        FormManager manager = createActiveManager();

        manager.setValue("foo", "bar");
        manager.setPropertyChangeHandler(event -> entries
                .put(event.getPropertyName(), event.getValue()));

        Assert.assertTrue(entries.containsKey("foo"));
        Assert.assertEquals("bar", entries.get("foo"));
    }

    @Test
    public void highlight_propertyIsHighlighted() {
        FormManager manager = createActiveManager();
        manager.highlight("foo", true);

        Assert.assertTrue(manager.isHighlight("foo"));
    }

    @Test
    public void highlight_removeHighlight_propertyIsNotHighlighted() {
        FormManager manager = createActiveManager();
        manager.highlight("foo", true);
        manager.highlight("foo", false);

        Assert.assertFalse(manager.isHighlight("foo"));
    }

    @Test
    public void setHandler_highlight_handlerInvoked() {
        Set<String> highlights = new HashSet<>();
        FormManager manager = createActiveManager();

        manager.setHighlightHandler(event -> {
            highlights.add(event.getPropertyName());
            return () -> highlights.remove(event.getPropertyName());
        });
        manager.highlight("foo", true);

        Assert.assertTrue(highlights.contains("foo"));
    }

    @Test
    public void highlight_setHandler_handlerInvoked() {
        Set<String> highlights = new HashSet<>();
        FormManager manager = createActiveManager();

        manager.highlight("foo", true);
        manager.setHighlightHandler(event -> {
            highlights.add(event.getPropertyName());
            return () -> highlights.remove(event.getPropertyName());
        });

        Assert.assertTrue(highlights.contains("foo"));
    }

    @Test
    public void setHandler_removeHighlight_removeRegistrationInvoked() {
        Set<String> highlights = new HashSet<>();
        FormManager manager = createActiveManager();

        manager.setHighlightHandler(event -> {
            highlights.add(event.getPropertyName());
            return () -> highlights.remove(event.getPropertyName());
        });
        manager.highlight("foo", true);
        manager.highlight("foo", false);

        Assert.assertFalse(highlights.contains("foo"));
    }

    @Test
    public void setHighlight_deactivateContext_highlightCleared() {
        MockConnectionContext context = new MockConnectionContext();
        FormManager manager = createManager(context);

        context.activate();
        manager.highlight("foo", true);
        context.deactivate();

        Assert.assertFalse(manager.isHighlight("foo"));
    }

    @Test
    public void setHandler_deactivateContext_registrationRemoved() {
        MockConnectionContext context = new MockConnectionContext();
        FormManager manager = createManager(context);

        Set<String> highlights = new HashSet<>();

        context.activate();
        manager.setHighlightHandler(event -> {
            highlights.add(event.getPropertyName());
            return () -> highlights.remove(event.getPropertyName());
        });

        manager.highlight("foo", true);
        Assert.assertTrue(highlights.contains("foo"));

        context.deactivate();

        Assert.assertFalse(highlights.contains("foo"));
    }

    @Test
    public void setValue_deactivateContext_valueCleared() {
        MockConnectionContext context = new MockConnectionContext();
        FormManager manager = createManager(context);

        context.activate();
        manager.setValue("foo", "bar");
        context.deactivate();

        Assert.assertNull(manager.getValue("foo", String.class));
    }

    @Test
    public void openTopicConnection_setExpirationTimeout_timeoutIsCorrect() {
        MockConnectionContext context = new MockConnectionContext();
        FormManager manager = createManager(context);

        manager.setExpirationTimeout(Duration.ofMinutes(15));

        long timeout = manager.getExpirationTimeout().get().toMinutes();
        Assert.assertEquals(15, timeout);
    }

    @Test
    public void setExpirationTimeout_openTopicConnection_timeoutIsCorrect() {
        FormManager manager = createActiveManager();
        manager.setExpirationTimeout(Duration.ofMinutes(15));

        AtomicLong timeout = new AtomicLong();
        ce.openTopicConnection(MockConnectionContext.createEager(), TOPIC_ID,
                user, connection -> {
                    timeout.set(
                            connection.getNamedMap(FormManager.COLLECTION_NAME)
                                    .getExpirationTimeout().get().toMinutes());
                    return null;
                });
        Assert.assertEquals(15, timeout.longValue());
    }

    private FormManager createActiveManager() {
        return createManager(MockConnectionContext.createEager());
    }

    private FormManager createManager(ConnectionContext context) {
        return new FormManager(context, user, TOPIC_ID, ce);
    }
}
