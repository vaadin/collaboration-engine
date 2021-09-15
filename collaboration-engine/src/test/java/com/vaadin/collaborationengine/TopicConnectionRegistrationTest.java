package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.SpyActivationHandler;
import com.vaadin.flow.server.Command;

public class TopicConnectionRegistrationTest extends AbstractLicenseTest {

    @Test
    public void onConnectionFailed_licenseTermsNotExceeded_actionNotCalled() {
        testOnConnectionFailed(false);
    }

    @Test
    public void onConnectionFailed_licenseTermsExceeded_actionCalled() {
        IntStream.range(0, GRACE_QUOTA)
                .forEach(i -> testOnConnectionFailed(false));
        testOnConnectionFailed(true);
        testOnConnectionFailed(true);
    }

    @Test
    public void onConnectionFailed_calledThroughDispatchAction() {
        fillGraceQuota();

        List<Runnable> executedActions = new ArrayList<>();
        MockConnectionContext context = MockConnectionContext.createEager();
        context.setExecutor(executedActions::add);
        TopicConnectionRegistration registration = openTopicConnection(context);
        AtomicInteger connectionFailedCallCount = new AtomicInteger(0);
        registration.onConnectionFailed(
                e -> connectionFailedCallCount.incrementAndGet());
        Assert.assertEquals(0, connectionFailedCallCount.get());
        Assert.assertEquals(1, executedActions.size());
        Assert.assertEquals(1, context.getDispathActionCount());

        executedActions.get(0).run();
        Assert.assertEquals(1, connectionFailedCallCount.get());
    }

    @Test
    public void connectionFailedEvent_getSource_returnsRegistration() {
        fillGraceQuota();

        TopicConnectionRegistration registration = openTopicConnection(
                MockConnectionContext.createEager());

        AtomicBoolean isCalled = new AtomicBoolean(false);
        registration.onConnectionFailed(e -> {

            // Validate at compile-time that the static type is correct:
            TopicConnectionRegistration source = e.getSource();

            Assert.assertSame(registration, source);

            isCalled.set(true);
        });
        Assert.assertTrue(isCalled.get());
    }

    private void testOnConnectionFailed(boolean expectToFail) {
        TopicConnectionRegistration registration = openTopicConnection(
                MockConnectionContext.createEager());

        AtomicInteger connectionFailedCallCount = new AtomicInteger(0);
        registration.onConnectionFailed(
                e -> connectionFailedCallCount.incrementAndGet());

        int expectedCallCount = expectToFail ? 1 : 0;
        Assert.assertEquals(
                "The connection failed handler was not called the correct number of times.",
                expectedCallCount, connectionFailedCallCount.get());
    }

    private TopicConnectionRegistration openTopicConnection(
            ConnectionContext connectionContext) {
        return ce.openTopicConnection(connectionContext, "topic-id",
                new UserInfo(UUID.randomUUID().toString()),
                topicConnection -> null);
    }
}
