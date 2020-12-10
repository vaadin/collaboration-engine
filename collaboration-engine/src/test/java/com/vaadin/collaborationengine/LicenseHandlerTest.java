package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.CollaborationEngine.CollaborationEngineConfig;
import com.vaadin.collaborationengine.ConnectionContextTest.SimpleConnectionContext;
import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.collaborationengine.util.EagerConnectionContext;
import com.vaadin.collaborationengine.util.MockUI;

import static org.hamcrest.CoreMatchers.containsString;

public class LicenseHandlerTest extends AbstractLicenseTest {

    public static class MockedLicenseHandler extends LicenseHandler {

        private LocalDate configuredCurrentDate = null;

        MockedLicenseHandler(CollaborationEngine collaborationEngine) {
            super(collaborationEngine);
        }

        protected void setCurrentDate(LocalDate currentDate) {
            configuredCurrentDate = currentDate;
        }

        @Override
        public LocalDate getCurrentDate() {
            return configuredCurrentDate != null ? configuredCurrentDate
                    : LocalDate.of(2020, 5, 1);
        }

        public YearMonth getCurrentMonth() {
            return YearMonth.from(getCurrentDate());
        }
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        Assert.assertTrue(licenseHandler.getStatistics().isEmpty());
        licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(licenseHandler.getCurrentMonth()));
        Assert.assertEquals(1,
                statistics.get(licenseHandler.getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(licenseHandler.getCurrentMonth())
                .contains("steve"));
        licenseHandler.registerUser("bob");

        statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(licenseHandler.getCurrentMonth()));
        Assert.assertEquals(2,
                statistics.get(licenseHandler.getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(licenseHandler.getCurrentMonth())
                .contains("steve"));
        Assert.assertTrue(statistics.get(licenseHandler.getCurrentMonth())
                .contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(licenseHandler.getCurrentMonth()));
        Set<String> usersInMonth = statistics
                .get(licenseHandler.getCurrentMonth());
        Assert.assertEquals(2, usersInMonth.size());
        Assert.assertTrue(usersInMonth.contains("steve"));
        Assert.assertTrue(usersInMonth.contains("bob"));
    }

    @Test
    public void registerUser_monthChanges_userIsReAdded() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.setCurrentDate(LocalDate.of(2020, 6, 1));
        licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = licenseHandler.getStatistics();
        YearMonth firstMonth = YearMonth.of(2020, 5);
        YearMonth secondMonth = YearMonth.of(2020, 6);
        Assert.assertEquals(2, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(firstMonth));
        Assert.assertEquals(2, statistics.get(firstMonth).size());
        Assert.assertTrue(statistics.get(firstMonth).contains("steve"));
        Assert.assertTrue(statistics.get(firstMonth).contains("bob"));
        Assert.assertEquals(1, statistics.get(secondMonth).size());
        Assert.assertTrue(statistics.get(secondMonth).contains("steve"));
    }

    @Test
    public void openTopicConnection_userRegistered() {
        ce.openTopicConnection(new EagerConnectionContext(), "foo",
                new UserInfo("steve"), topicConnection -> null);
        Map<YearMonth, Set<String>> statistics = ce.getLicenseHandler()
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(YearMonth.now()));
        Set<String> currentMonth = statistics.get(YearMonth.now());
        Assert.assertEquals(1, currentMonth.size());
        Assert.assertTrue(currentMonth.contains("steve"));
    }

    @Test
    public void noStatsFile_initStatistics_hasEmptyMap() throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler(ce);
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertEquals(Collections.emptyMap(),
                licenseHandler.getStatistics());
    }

    @Test
    public void noStatsFile_initStatistics_hasNullGracePeriodStart()
            throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler(ce);
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertNull("Grace period start should have been unset",
                licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void noStatsFile_registerUser_statsFileCreated() throws IOException {
        Files.deleteIfExists(statsFilePath);
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");

        assertStatsFileContent("{\"licenseKey\":\""
                + licenseHandler.statistics.licenseKey
                + "\",\"statistics\":{\"2020-02\":[\"steve\"]},\"gracePeriodStart\":null,"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void statsFileHasData_initStatistics_readsDataFromFile()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2000-01\":[\"bob\"]},\"licenseEvents\":{}}");
        LicenseHandler licenseHandler = new LicenseHandler(ce);

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(Collections.singleton("bob"),
                licenseHandler.getStatistics().get(YearMonth.of(2000, 1)));
    }

    @Test
    public void statsFileHasData_initStatistics_registerUser_allDataIncludedInFile()
            throws IOException {
        writeToStatsFile("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\"]},\"licenseEvents\":{}}");
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(LocalDate.of(2020, 1, 1));
        licenseHandler.registerUser("steve");

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("bob", "steve")),
                licenseHandler.getStatistics().get(YearMonth.of(2020, 1)));
        assertStatsFileContent("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},\"gracePeriodStart\":null,"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void statsFileHasData_newLicenseGiven_licenseKeyUpdatedInStats()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2020-01\":[\"bob\"]},"
                        + "\"gracePeriodStart\":null,\"licenseEvents\":{}}");
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(LocalDate.of(2020, 1, 1));

        // Do an action to trigger rewrite of the stats file
        licenseHandler.registerUser("steve");
        assertStatsFileContent("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},\"gracePeriodStart\":null,"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void registerMultipleUsersAndMonths_writeToFile_readFromFile() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");

        licenseHandler.setCurrentDate(LocalDate.of(2020, 3, 1));
        licenseHandler.registerUser("steve");

        assertStatsFileContent(
                "{\"licenseKey\":\"" + licenseHandler.statistics.licenseKey
                        + "\",\"statistics\":{\"2020-02\":[\"steve\",\"bob\"],"
                        + "\"2020-03\":[\"steve\"]},\"gracePeriodStart\":null,"
                        + "\"licenseEvents\":{}}");

        Map<YearMonth, Set<String>> newStats = new LicenseHandler(ce)
                .getStatistics();
        Assert.assertEquals(2, newStats.size());
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("steve", "bob")),
                newStats.get(YearMonth.of(2020, 2)));
        Assert.assertEquals(Collections.singleton("steve"),
                newStats.get(YearMonth.of(2020, 3)));
    }

    @Test(expected = IllegalStateException.class)
    public void statsFileHasInvalidData_initLicenseHandler_throws()
            throws IOException {
        writeToStatsFile("I'm invalid");
        new LicenseHandler(ce);
    }

    @Test
    public void dataDirNotConfigured_openTopicConnection_throws() {
        ce.setConfigProvider(() -> new CollaborationEngineConfig(true, null));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required configuration property");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void noDataDir_openTopicConnection_throws() {
        ce.setConfigProvider(() -> new CollaborationEngineConfig(true,
                Paths.get(System.getProperty("user.home"), ".vaadin",
                        "ce-tests", "non-existing", "dir")));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("failed to find the license file");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void noLicenseFile_openTopicConnection_throws() throws IOException {
        Files.deleteIfExists(licenseFilePath);

        exception.expect(IllegalStateException.class);
        exception.expectMessage("failed to find the license file");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileMissingContent_openTopicConnection_throws()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("license file is not valid");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileMissingChecksum_openTopicConnection_throws()
            throws IOException {
        String license = licenseGenerator.generateLicense("Foo", 100,
                LocalDate.of(2020, 3, 4));
        int index = license.indexOf(",\"checksum\":");
        String licenseWithoutChecksum = license
                .replace(license.substring(index, index + 58), "");
        writeToLicenseFile(licenseWithoutChecksum);

        exception.expect(IllegalStateException.class);
        exception.expectMessage("license file is not valid");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileTampered_openTopicConnection_throws()
            throws IOException {
        String license = licenseGenerator.generateLicense("Foo", 100,
                LocalDate.of(2020, 3, 4));

        String tamperedLicense = license.replace("\"quota\":100",
                "\"quota\":200");

        writeToLicenseFile(tamperedLicense);

        exception.expect(IllegalStateException.class);
        exception.expectMessage("license file is not valid");

        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileInvalid_openManyTopicConnections_throwsEveryTime()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        try {
            ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                    new UserInfo("user-id"), topicConnection -> null);
        } catch (IllegalStateException e) {
            // expected
        }

        exception.expect(IllegalStateException.class);
        exception.expectMessage("license file is not valid");

        // open a second connection
        ce.openTopicConnection(new EagerConnectionContext(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceeded_connectionNotActivated() {
        List<String> users = generateIds(GRACE_QUOTA + 5);
        List<String> usersWithConnectionActivated = new ArrayList<>();

        users.forEach(
                userId -> ce.openTopicConnection(new EagerConnectionContext(),
                        "topic", new UserInfo(userId), topicConnection -> {
                            usersWithConnectionActivated.add(userId);
                            return null;
                        }));

        Assert.assertEquals(users.subList(0, GRACE_QUOTA),
                usersWithConnectionActivated);
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceededButUserHasSeat_connectionActivated() {
        List<String> userIds = generateIds(GRACE_QUOTA + 5);
        userIds.forEach(userId -> ce.openTopicConnection(
                new EagerConnectionContext(), "topic", new UserInfo(userId),
                topicConnection -> null));

        AtomicBoolean connectionActivated = new AtomicBoolean(false);
        ce.openTopicConnection(new EagerConnectionContext(), "topic",
                new UserInfo(userIds.get(0)), topicConnection -> {
                    Assert.assertFalse(connectionActivated.get());
                    connectionActivated.set(true);
                    return null;
                });

        Assert.assertTrue(connectionActivated.get());
    }

    @Test
    public void registerUser_normalQuotaFullNoGraceNewUserEnters_graceStartedAndAccessGranted() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        registerUsers(licenseHandler, QUOTA);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.statistics.gracePeriodStart);
        boolean wasAccessGranted = licenseHandler.registerUser("bob");
        Assert.assertTrue(
                "Was not able to register a new person with the grace period",
                wasAccessGranted);
        Assert.assertNotNull(
                "Grace period was not automatically started from going over the limit",
                licenseHandler.statistics.gracePeriodStart);
        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(4, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceOngoingNewUserEnters_accessGranted() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        registerUsers(licenseHandler, QUOTA + 1);
        Assert.assertNotNull("Grace period was expected to be ongoing",
                licenseHandler.statistics.gracePeriodStart);
        boolean wasAccessGranted = licenseHandler.registerUser("bob");
        Assert.assertTrue(
                "Was not able to register a new person with the grace period",
                wasAccessGranted);
        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(5, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_normalQuotaFullNoGraceExistingUserEnters_accessGrantedNoGracePeriod() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        List<String> users = registerUsers(licenseHandler, QUOTA);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.statistics.gracePeriodStart);
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(0));
        Assert.assertTrue("Was not able to register an existing user",
                wasAccessGranted);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void registerUser_tenTimesQuotaAvailableInGrace_accessGranted() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        List<String> users = generateIds(GRACE_QUOTA);
        for (String user : users) {
            Assert.assertTrue("User wasn't given access",
                    licenseHandler.registerUser(user));
        }
        Assert.assertEquals(GRACE_QUOTA, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_usersExceedingGraceQuota_accessDenied() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler.registerUser("steve");
        Assert.assertFalse("User should have been denied access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullNormalQuotaUserReturns_accessGranted() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        List<String> users = registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(1));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullGraceQuotaUserReturns_accessGranted() {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        List<String> users = registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler
                .registerUser(users.get(QUOTA * 2));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void unSetGracePeriodReadFromFile_gracePeriodIsNull()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2020-05\":[\"userId-1\",\"userId-2\""
                        + "]},"
                        + "\"gracePeriodStart\":null,\"licenseEvents\":{}}");
        LicenseHandler licenseHandler = new LicenseHandler(ce);
        Assert.assertNull(licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void setGracePeriodReadFromFile_gracePeriodIsSet()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2020-05\":[\"userId-1\",\"userId-2\","
                        + "\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\","
                        + "\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"]},"
                        + "\"gracePeriodStart\":\"2020-10-28\",\"licenseEvents\":{}}");
        LicenseHandler licenseHandler = new LicenseHandler(ce);
        Assert.assertEquals(LocalDate.of(2020, 10, 28),
                licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void registerUser_lastDayOfGracePeriodNewUserEnters_accessGranted()
            throws IOException {
        LicenseHandler licenseHandler = getLicenseHandlerWithGracePeriod(30);
        Assert.assertTrue("User should have been given access",
                licenseHandler.registerUser("dean"));
    }

    @Test
    public void registerUser_gracePeriodExpiredNewUserEnters_accessDenied()
            throws IOException {
        LicenseHandler licenseHandler = getLicenseHandlerWithGracePeriod(31);
        Assert.assertFalse("User should have been denied access",
                licenseHandler.registerUser("dean"));
    }

    @Test
    public void registerUser_gracePeriodExpiredNormalQuotaUserEnters_accessGranted()
            throws IOException {
        LicenseHandler licenseHandler = getLicenseHandlerWithGracePeriod(31);
        Assert.assertTrue("User should have been given access",
                licenseHandler.registerUser("userId-2"));
    }

    @Test
    public void registerUser_gracePeriodExpiredGraceQuotaUserEnters_accessDenied()
            throws IOException {
        LicenseHandler licenseHandler = getLicenseHandlerWithGracePeriod(31);
        Assert.assertFalse("User should have been denied access",
                licenseHandler.registerUser("userId-7"));
    }

    @Test
    public void requestAccess_actionIsDispatched() {
        UserInfo user = new UserInfo("steve");
        SimpleConnectionContext spyContext = new SimpleConnectionContext();
        ce.requestAccess(spyContext, user, result -> {
        });
        Assert.assertTrue(spyContext.isCalled);
    }

    @Test
    public void requestAccess_uiIsAccessed() {
        UserInfo user = new UserInfo("steve");
        MockUI ui = new MockUI();
        ce.requestAccess(ui, user, result -> {
        });
        Assert.assertFalse(ui.getAccessTasks().isEmpty());
    }

    @Test
    public void requestAccess_licenseCheckingNotEnabled_resolvesWithTrue() {
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(false);
        SimpleConnectionContext spyContext = new SimpleConnectionContext();
        ce.setConfigProvider(() -> new CollaborationEngineConfig(false, null));
        ce.requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertTrue(result.get());

    }

    @Test
    public void requestAccess_userHasAccess_resolvesWithTrue() {
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(false);
        SimpleConnectionContext spyContext = new SimpleConnectionContext();
        ce.requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertTrue(result.get());
    }

    @Test
    public void requestAccess_userDoesNotHaveAccess_resolvesWithFalse() {
        fillGraceQuota();
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(true);
        SimpleConnectionContext spyContext = new SimpleConnectionContext();
        ce.requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertFalse(result.get());
    }

    @Test
    public void requestAccess_invalidLicense_throwsException()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("license file is not valid");

        UserInfo user = new UserInfo("steve");
        ConnectionContext spyContext = new EagerConnectionContext();
        ce.requestAccess(spyContext, user, response -> {
        });
    }

    @Test
    public void registerUser_licenseLastDayValid_accessGranted()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        writeToLicenseFile(3, dateNow);
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(dateNow);
        Assert.assertTrue("User should have been given access",
                licenseHandler.registerUser("userId-1"));
    }

    @Test
    public void registerUser_licenseExpired_accessDenied() throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(dateNow);
        Assert.assertFalse("User should have been denied access",
                licenseHandler.registerUser("userId-1"));
    }

    @Test
    public void licenseEventHandler_licenseExpired_eventFiredOnce()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));

        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);

        licenseHandler.setCurrentDate(dateNow.minusDays(1));
        licenseHandler.registerUser("steve");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRED);
        Assert.assertFalse(eventFired);

        licenseHandler.setCurrentDate(dateNow);
        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.LICENSE_EXPIRED);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void licenseEventHandler_licenseExpiresSoon_eventFiredOnce()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        writeToLicenseFile(3, LocalDate.of(2020, 7, 10));

        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);

        licenseHandler.setCurrentDate(dateNow.minusDays(1));
        licenseHandler.registerUser("steve");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRES_SOON);
        Assert.assertFalse(eventFired);

        licenseHandler.setCurrentDate(dateNow);
        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.LICENSE_EXPIRES_SOON);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void licenseEventHandler_gracePeriodStarted_eventFiredOnce()
            throws IOException {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        registerUsers(licenseHandler, 3);
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.GRACE_PERIOD_STARTED);
        Assert.assertFalse(eventFired);

        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.GRACE_PERIOD_STARTED);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void licenseEventHandler_gracePeriodEnded_eventFiredOnce()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 8, 10));

        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);

        licenseHandler.setCurrentDate(dateNow);
        registerUsers(licenseHandler, 4);
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertFalse(eventFired);

        licenseHandler.setCurrentDate(dateNow.plusDays(30));
        licenseHandler.registerUser("steve");
        eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertFalse(eventFired);

        licenseHandler.setCurrentDate(dateNow.plusDays(31));
        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void readEventFiredDateFromStatsFile_licenseExpired_eventNotFiredAgain()
            throws IOException {
        writeToStatsFile("{\"licenseKey\":\"123"
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},\"gracePeriodStart\":null,"
                + "\"licenseEvents\":{\"licenseExpired\":\"2020-06-11\"}}");
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(dateNow);
        licenseHandler.registerUser("foo");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRED);
        Assert.assertFalse(eventFired);
    }

    @Test
    public void licenseEventHandler_gracePeriodStarted_messageHasCorrectDate()
            throws IOException {
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        registerUsers(licenseHandler, 4);

        String message = spyEventHandler.getMessages().get(0);
        Assert.assertThat(message, containsString("2020-06-01"));
    }

    @Test
    public void licenseEventHandler_licenseExpiresSoon_messageHasCorrectDate()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        LocalDate endDate = LocalDate.of(2020, 7, 10);
        writeToLicenseFile(3, endDate);

        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);

        licenseHandler.setCurrentDate(dateNow);
        licenseHandler.registerUser("steve");

        String message = spyEventHandler.getMessages().get(0);
        Assert.assertThat(message, containsString("2020-07-10"));
    }

    private LicenseHandler getLicenseHandlerWithGracePeriod(
            int daysSinceGracePeriodStarted) throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        LocalDate graceStart = dateNow.minusDays(daysSinceGracePeriodStarted);

        writeToStatsFile("{\"licenseKey\":\"123\",\"statistics\":{\""
                + YearMonth.from(dateNow).minusMonths(1)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"], \""
                + YearMonth.from(dateNow)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"] },"
                + "\"gracePeriodStart\":\"" + graceStart
                + "\", \"licenseEvents\":{}}");
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler(ce);
        licenseHandler.setCurrentDate(dateNow);
        return licenseHandler;
    }

    private List<String> registerUsers(LicenseHandler licenseHandler, int i) {
        List<String> users = generateIds(i);
        users.forEach(licenseHandler::registerUser);
        return users;
    }
}
