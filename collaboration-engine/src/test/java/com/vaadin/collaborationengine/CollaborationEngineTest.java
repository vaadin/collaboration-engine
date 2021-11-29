package com.vaadin.collaborationengine;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.collaborationengine.TestUtil.TestCollaborationEngine;
import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

import static com.vaadin.collaborationengine.CollaborationEngine.USER_COLOR_COUNT;

public class CollaborationEngineTest {

    private VaadinService service = new MockService();
    private CollaborationEngine collaborationEngine;
    private ConnectionContext context;
    private SerializableFunction<TopicConnection, Registration> connectionCallback;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        VaadinService.setCurrent(service);
        collaborationEngine = TestUtil.createTestCollaborationEngine(service);

        context = MockConnectionContext.createEager();
        connectionCallback = topicConnection -> () -> {
            // no impl
        };
    }

    @After
    public void cleanUp() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void correctVersionPassedToUsageStatistics() {
        String artifactVersion = System
                .getProperty("collaboration-engine.version");

        if (artifactVersion == null) {
            String javaCommand = System.getProperty("sun.java.command");
            Assume.assumeFalse(
                    "Maven version property is not set when run through Eclipse",
                    javaCommand != null && javaCommand.startsWith(
                            "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"));
        }

        Assert.assertThat(artifactVersion, CoreMatchers
                .startsWith(CollaborationEngine.COLLABORATION_ENGINE_VERSION));
    }

    @Test
    public void getInstance_notNull() {
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    Assert.assertNotNull(topicConnection);
                    return null;
                });
    }

    @Test
    public void getInstance_returnsAlwaysSameInstance() {
        Assert.assertSame(CollaborationEngine.getInstance(service),
                CollaborationEngine.getInstance(service));
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullComponent_throws() {
        collaborationEngine.openTopicConnection((Component) null, "foo",
                SystemUserInfo.getInstance(), connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullContext_throws() {
        collaborationEngine.openTopicConnection((ConnectionContext) null, "foo",
                SystemUserInfo.getInstance(), connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullId_throws() {
        collaborationEngine.openTopicConnection(context, null,
                SystemUserInfo.getInstance(), connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnection_nullUserInfo_throws() {
        collaborationEngine.openTopicConnection(context, "foo", null,
                connectionCallback);
    }

    @Test(expected = NullPointerException.class)
    public void openTopicConnectionWithNullCallback_throws() {
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), null);
    }

    @Test
    public void openTopicConnection_sameTopicId_hasSameTopic() {
        Topic[] topics = new Topic[2];
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topics[0] = topicConnection.getTopic();
                    return null;
                });

        ConnectionContext otherContext = MockConnectionContext.createEager();
        collaborationEngine.openTopicConnection(otherContext, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topics[1] = topicConnection.getTopic();
                    return null;
                });
        Assert.assertSame(topics[0], topics[1]);
    }

    @Test
    public void openTopicConnections_distinctTopicIds_hasDistinctTopics() {
        Topic[] topics = new Topic[2];
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topics[0] = topicConnection.getTopic();
                    return null;
                });

        collaborationEngine.openTopicConnection(context, "baz",
                SystemUserInfo.getInstance(), topicConnection -> {
                    topics[1] = topicConnection.getTopic();
                    return null;
                });
        Assert.assertNotSame(topics[0], topics[1]);
    }

    @Test
    public void reopenTopicConnection_newTopicConnectionInstance() {
        TopicConnection[] connections = new TopicConnection[2];
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), topic -> {
                    connections[0] = topic;
                    return null;
                });
        connections[0].deactivateAndClose();
        collaborationEngine.openTopicConnection(context, "foo",
                SystemUserInfo.getInstance(), otherTopic -> {
                    connections[1] = otherTopic;
                    return null;
                });
        Assert.assertNotSame(
                "TopicConnection instance should not be reused after closing.",
                connections[0], connections[1]);
    }

    @Test
    public void closeTopicConnection_garbageCollectedTheActivationHandler()
            throws InterruptedException {
        MockConnectionContext spyContext = new MockConnectionContext();
        Registration registration = collaborationEngine
                .openTopicConnection(spyContext, "foo",
                        SystemUserInfo.getInstance(), topic -> {
                            // no impl
                            return null;
                        });
        spyContext.activate();
        WeakReference weakRef = new WeakReference(
                spyContext.getActivationHandler());
        registration.remove();
        spyContext = null;
        Assert.assertTrue(
                "Expect ActivationHandler to be garbage-collected when connection closed",
                TestUtils.isGarbageCollected(weakRef));

    }

    @Test
    public void userColors_sameColorForSameUserId() {
        UserInfo firstUser = new UserInfo("id1234");
        UserInfo secondUser = new UserInfo("id1234");
        Assert.assertEquals(collaborationEngine.getUserColorIndex(firstUser),
                collaborationEngine.getUserColorIndex(secondUser));
    }

    @Test
    public void userColors_calculateBaseOnMapSize() {
        UserInfo firstUser = new UserInfo("userId-first");
        int offset = collaborationEngine.getUserColorIndex(firstUser);

        for (int i = 0; i < 12; i++) {
            UserInfo user = new UserInfo("user-color-test-id-" + i);
            Assert.assertEquals(collaborationEngine.getUserColorIndex(user),
                    (offset + i + 1) % USER_COLOR_COUNT);
        }
    }

    @Test
    public void userColors_userInfoHasColorSet() {
        UserInfo firstUser = new UserInfo("id1234", 99999);
        Assert.assertEquals(99999,
                collaborationEngine.getUserColorIndex(firstUser));
    }

    @Test(expected = NullPointerException.class)
    public void nullUserId_throws() {
        new UserInfo(null);
    }

    @Test
    public void openConnection_callbackRunThroughDispatch() {
        AtomicBoolean callbackRun = new AtomicBoolean();
        AtomicReference<Runnable> pendingAction = new AtomicReference<>();

        MockConnectionContext context = MockConnectionContext.createEager();
        context.setExecutor(action -> {
            boolean updated = pendingAction.compareAndSet(null, action);
            Assert.assertTrue("There should not be a previous pending action",
                    updated);

        });

        collaborationEngine.openTopicConnection(context, "topic",
                SystemUserInfo.getInstance(), connection -> {
                    boolean previouslyRun = callbackRun.getAndSet(true);
                    Assert.assertFalse(
                            "Callback should have been run only once",
                            previouslyRun);
                    return null;
                });

        Assert.assertFalse(callbackRun.get());

        pendingAction.get().run();

        Assert.assertTrue(callbackRun.get());
    }

    @Test
    public void getInstance_productionMode_configurationNotProvided_throwsException() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                "Collaboration Engine is missing a required configuration object");
        MockService service = new MockService(true);
        CollaborationEngine.getInstance(service);
    }

    @Test
    public void configure_configurationAlreadySet_throwsException() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                "Collaboration Engine has been already configured");
        MockService service = new MockService();
        CollaborationEngine.configure(service,
                new CollaborationEngineConfiguration(e -> {
                }));
        CollaborationEngine.configure(service,
                new CollaborationEngineConfiguration(e -> {
                }));
    }

    @Test(expected = NullPointerException.class)
    public void configureLicenseEventHandlerNull_throwsException() {
        CollaborationEngine.configure(new MockService(),
                new CollaborationEngineConfiguration(null));
    }

    @Test
    public void dataDirPropertyIsSetCorrectly() {
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                event -> {
                });
        configuration.setVaadinService(service);
        configuration.setDataDir("foo");
        String dataDir = configuration.getDataDirPath().toString();
        Assert.assertEquals("foo", dataDir);
    }

    @Test
    public void dataDirPropertyTakesPrecedenceOverConfiguration() {
        System.setProperty("vaadin.ce.dataDir", "foo");
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                event -> {
                });
        configuration.setVaadinService(service);
        configuration.setDataDir("bar");
        String dataDir = configuration.getDataDirPath().toString();
        Assert.assertEquals("foo", dataDir);
    }

    @Test
    public void serviceExecutorConfigured_isUsedToDispatchActions() {
        VaadinService service = new MockService();
        AtomicBoolean executorUsed = new AtomicBoolean();
        ExecutorService customExecutor = Executors
                .newSingleThreadExecutor(runnable -> {
                    executorUsed.set(true);
                    return new Thread(runnable);
                });

        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service, customExecutor);
        ce.setAsynchronous(true);
        ce.openTopicConnection(MockConnectionContext.createEager(), "foo",
                new UserInfo("foo"), connection -> null);

        Assert.assertTrue(executorUsed.get());
        customExecutor.shutdown();
    }

    @Test
    public void serviceExecutorNotConfigured_defaultOneIsSet() {
        Assert.assertNotNull(collaborationEngine.getExecutorService());
    }

    @Test
    public void serviceDestroy_defaultExecutorServiceIsShutdown() {
        service.destroy();

        Assert.assertTrue(
                collaborationEngine.getExecutorService().isShutdown());
    }

    @Test
    public void serviceDestroy_customExecutorServiceNotShutdown() {
        VaadinService service = new MockService();
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();

        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service, customExecutor);
        ce.setAsynchronous(true);

        service.destroy();

        Assert.assertFalse(customExecutor.isShutdown());
        customExecutor.shutdown();
    }

    @Test
    public void serviceDestroy_ExecutorServiceShutdown_closesConnections() {
        VaadinService service = new MockService();
        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        AtomicBoolean clear = new AtomicBoolean();
        ce.openTopicConnection(ce.getSystemContext(), "topic",
                new UserInfo("id"), conn -> () -> clear.set(true));
        service.destroy();
        Assert.assertTrue("Destroy should shutdown the thread pool",
                ce.getExecutorService().isShutdown());
        Assert.assertTrue("Connection should have been closed on shutdown",
                clear.get());
    }

    @Test
    public void serviceDestroy_ExecutorServiceShutdown_closesConnections_timeout() {
        VaadinService service = new MockService();
        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        ce.setAsynchronous(true);

        CompletableFuture<Void> neverCompletedFuture = new CompletableFuture<>();
        MockConnectionContext context = createEagerContextWithAsyncRegistration(
                neverCompletedFuture);

        TopicConnectionRegistration registration = ce.openTopicConnection(
                context, "topic", new UserInfo("id"), conn -> () -> {
                    // No op
                });
        service.destroy();
        Assert.assertSame(
                "TopicConnectionRegistration should contain"
                        + " the same future returned by AsyncRegistration",
                neverCompletedFuture,
                registration.getPendingFuture().orElse(null));
        Assert.assertFalse("Future should not have been completed",
                neverCompletedFuture.isDone());
        Assert.assertTrue(
                "Should shutdown even if the future is never completed",
                ce.getExecutorService().isShutdown());
    }

    @Test
    public void serviceDestroy_ExecutorServiceShutdown_closesConnections_whenFutureCompleted()
            throws ExecutionException, InterruptedException {
        VaadinService service = new MockService();
        TestCollaborationEngine ce = TestUtil
                .createTestCollaborationEngine(service);
        ce.setAsynchronous(true);

        CompletableFuture<Void> registrationFuture = new CompletableFuture<>();
        MockConnectionContext context = createEagerContextWithAsyncRegistration(
                registrationFuture);

        TopicConnectionRegistration registration = ce.openTopicConnection(
                context, "topic", new UserInfo("id"), conn -> () -> {
                    // No op
                });
        Instant start = Instant.now();
        CompletableFuture<Void> destroyFuture = CompletableFuture
                .runAsync(service::destroy);
        Assert.assertFalse("Registration should not have been completed yet",
                registrationFuture.isDone());
        Assert.assertFalse(
                "Shutdown should wait for registrationFuture to be completed",
                ce.getExecutorService().isShutdown());
        Assert.assertFalse(
                "Shutdown should wait for registrationFuture to be completed",
                destroyFuture.isDone());
        // Complete registration future and wait for shutdown
        registrationFuture.complete(null);
        destroyFuture.get();

        Assert.assertTrue("Shutdown should have been completed",
                destroyFuture.isDone());
        Assert.assertTrue("Should not have to wait for timeout to complete",
                Instant.now().isBefore(start.plus(1, ChronoUnit.SECONDS)));
    }

    private MockConnectionContext createEagerContextWithAsyncRegistration(
            CompletableFuture<Void> registrationFuture) {

        MockConnectionContext context = new MockConnectionContext() {
            @Override
            public Registration init(ActivationHandler activationHandler,
                    Executor executor) {
                return new AsyncRegistration(registrationFuture,
                        super.init(activationHandler, executor));
            }
        };
        context.setEager(true);
        return context;
    }
}
