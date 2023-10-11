package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.collaborationengine.LicenseHandler.LicenseInfoWrapper;
import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfo;
import com.vaadin.collaborationengine.TestUtil.MockConfiguration;
import com.vaadin.collaborationengine.util.MockConnectionContext;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.MockUI;
import com.vaadin.collaborationengine.util.TestBackendFactory;
import com.vaadin.flow.component.UI;

import static org.hamcrest.CoreMatchers.containsString;

public class LicenseHandlerTest extends AbstractLicenseTest {

    private static final String INVALID_LICENSE = "license property or file is not valid";

    private TestBackendFactory backendFactory;

    @Before
    public void setup() {
        backendFactory = new TestBackendFactory();
        setCurrentDate(LocalDate.of(2020, 5, 1));
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        Assert.assertTrue(licenseHandler.getStatistics().isEmpty());
        licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(getCurrentMonth()));
        Assert.assertEquals(1, statistics.get(getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(getCurrentMonth()).contains("steve"));
        licenseHandler.registerUser("bob");

        statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(getCurrentMonth()));
        Assert.assertEquals(2, statistics.get(getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(getCurrentMonth()).contains("steve"));
        Assert.assertTrue(statistics.get(getCurrentMonth()).contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(getCurrentMonth()));
        Set<String> usersInMonth = statistics.get(getCurrentMonth());
        Assert.assertEquals(2, usersInMonth.size());
        Assert.assertTrue(usersInMonth.contains("steve"));
        Assert.assertTrue(usersInMonth.contains("bob"));
    }

    @Test
    public void registerUser_monthChanges_userIsReAdded() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        setCurrentDate(LocalDate.of(2020, 6, 1));
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
        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "foo",
                new UserInfo("steve"), topicConnection -> null);
        Map<YearMonth, Set<String>> statistics = getCollaborationEngine()
                .getLicenseHandler().getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.containsKey(getCurrentMonth()));
        Set<String> currentMonth = statistics.get(getCurrentMonth());
        Assert.assertEquals(1, currentMonth.size());
        Assert.assertTrue(currentMonth.contains("steve"));
    }

    @Test
    public void noStatsFile_initStatistics_hasEmptyMap() throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertEquals(Collections.emptyMap(),
                licenseHandler.getStatistics());
    }

    @Test
    public void noStatsFile_initStatistics_hasNullGracePeriodStart()
            throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertNull("Grace period start should have been unset",
                licenseHandler.getGracePeriodStarted());
    }

    @Test
    public void noStatsFile_registerUser_statsFileCreated() throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");

        assertStatsFileContent("{\"licenseKey\":\"" + licenseHandler.license.key
                + "\",\"statistics\":{\"2020-02\":[\"steve\"]},"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void statsFileHasData_initStatistics_readsDataFromFile()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2000-01\":[\"bob\"]},\"licenseEvents\":{}}");
        setCurrentDate(LocalDate.of(2000, 1, 1));
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(Collections.singleton("bob"),
                licenseHandler.getStatistics().get(YearMonth.of(2000, 1)));
    }

    @Test
    public void statsFileHasData_initStatistics_registerUser_allDataIncludedInFile()
            throws IOException {
        writeToStatsFile("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\"]},\"licenseEvents\":{}}");
        setCurrentDate(LocalDate.of(2020, 1, 1));
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        licenseHandler.registerUser("steve");

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("bob", "steve")),
                licenseHandler.getStatistics().get(YearMonth.of(2020, 1)));
        assertStatsFileContent("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void statsFileHasData_newLicenseGiven_statsFileCorrectlyUpdated()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2020-01\":[\"bob\"]},"
                        + "\"licenseEvents\":{"
                        + "\"licenseExpiresSoon\":\"2019-12-10\","
                        + "\"licenseExpired\":\"2020-01-10\","
                        + "\"gracePeriodStarted\":\"2019-12-15\","
                        + "\"gracePeriodEnded\":\"2020-01-15\"" + "}}");
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(LocalDate.of(2020, 1, 25));

        // Do an action to trigger rewrite of the stats file
        licenseHandler.registerUser("steve");
        assertStatsFileContent("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},"
                + "\"licenseEvents\":{}}");
    }

    @Test
    public void statsFileHasData_restartHappens_graceAndEventsAreNotEmptied()
            throws IOException {
        writeToStatsFile("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\"2020-01\":[\"bob\"]},"
                + "\"licenseEvents\":{"
                + "\"licenseExpiresSoon\":\"2020-01-10\","
                + "\"gracePeriodStarted\":\"2019-12-15\","
                + "\"gracePeriodEnded\":\"2020-01-15\"" + "}}");
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(LocalDate.of(2020, 1, 25));
        // Do an action to trigger rewrite of the stats file
        licenseHandler.registerUser("steve");

        StatisticsInfo statisticsInfo = readStatsFileContent();
        Assert.assertEquals(LocalDate.of(2020, 1, 10),
                statisticsInfo.licenseEvents
                        .get(LicenseEventType.LICENSE_EXPIRES_SOON));
        Assert.assertNull(statisticsInfo.licenseEvents
                .get(LicenseEventType.LICENSE_EXPIRED));
        Assert.assertEquals(LocalDate.of(2019, 12, 15),
                statisticsInfo.licenseEvents
                        .get(LicenseEventType.GRACE_PERIOD_STARTED));
        Assert.assertEquals(LocalDate.of(2020, 1, 15),
                statisticsInfo.licenseEvents
                        .get(LicenseEventType.GRACE_PERIOD_ENDED));

    }

    @Test
    public void registerMultipleUsersAndMonths_writeToFile_readFromFile() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");

        setCurrentDate(LocalDate.of(2020, 3, 1));
        licenseHandler.registerUser("steve");

        assertStatsFileContent("{\"licenseKey\":\"" + licenseHandler.license.key
                + "\",\"statistics\":{\"2020-02\":[\"steve\",\"bob\"],"
                + "\"2020-03\":[\"steve\"]},\"licenseEvents\":{}}");

        Map<YearMonth, Set<String>> newStats = new LicenseHandler(ceSupplier)
                .getStatistics();

        // Only current month is loaded to cache
        Assert.assertEquals(1, newStats.size());
        Assert.assertEquals(Collections.singleton("steve"),
                newStats.get(YearMonth.of(2020, 3)));
    }

    @Test(expected = IllegalStateException.class)
    public void statsFileHasInvalidData_initLicenseHandler_throws()
            throws IOException {
        writeToStatsFile("I'm invalid");
        new LicenseHandler(ceSupplier);
    }

    @Test
    public void statsFileTampered_openTopicConnection_throws()
            throws IOException {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("statistics file is not valid");

        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");

        String stats = new String(Files.readAllBytes(statsFilePath));
        String tamperedStats = stats.replace("[\"steve\",\"bob\"]",
                "[\"steve\"]");
        Files.write(statsFilePath, tamperedStats.getBytes());

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void dataDirNotConfigured_openTopicConnection_throws() {
        configuration.setDataDirPath(null);

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required configuration property");

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void noDataDir_openTopicConnection_throws() {
        configuration.setDataDirPath(Paths.get(System.getProperty("user.home"),
                ".vaadin", "ce-tests", "non-existing", "dir"));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("failed to find the license file");

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void noLicenseFile_openTopicConnection_throws() throws IOException {
        Files.deleteIfExists(licenseFilePath);

        exception.expect(IllegalStateException.class);
        exception.expectMessage("failed to find the license file");

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileMissingContent_openTopicConnection_throws()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        exception.expect(IllegalStateException.class);
        exception.expectMessage(INVALID_LICENSE);

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
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
        exception.expectMessage(INVALID_LICENSE);

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
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
        exception.expectMessage(INVALID_LICENSE);

        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void licenseFileInvalid_openManyTopicConnections_throwsEveryTime()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        try {
            getCollaborationEngine().openTopicConnection(
                    MockConnectionContext.createEager(), "topic-id",
                    new UserInfo("user-id"), topicConnection -> null);
        } catch (IllegalStateException e) {
            // expected
        }

        exception.expect(IllegalStateException.class);
        exception.expectMessage(INVALID_LICENSE);

        // open a second connection
        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic-id",
                new UserInfo("user-id"), topicConnection -> null);
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceeded_connectionNotActivated() {
        List<String> users = generateIds(GRACE_QUOTA + 5);
        List<String> usersWithConnectionActivated = new ArrayList<>();

        users.forEach(userId -> getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic",
                new UserInfo(userId), topicConnection -> {
                    usersWithConnectionActivated.add(userId);
                    return null;
                }));

        Assert.assertEquals(users.subList(0, GRACE_QUOTA),
                usersWithConnectionActivated);
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceededButUserHasSeat_connectionActivated() {
        List<String> userIds = generateIds(GRACE_QUOTA + 5);
        userIds.forEach(userId -> getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic",
                new UserInfo(userId), topicConnection -> null));

        AtomicBoolean connectionActivated = new AtomicBoolean(false);
        getCollaborationEngine().openTopicConnection(
                MockConnectionContext.createEager(), "topic",
                new UserInfo(userIds.get(0)), topicConnection -> {
                    Assert.assertFalse(connectionActivated.get());
                    connectionActivated.set(true);
                    return null;
                });

        Assert.assertTrue(connectionActivated.get());
    }

    @Test
    public void registerUser_normalQuotaFullNoGraceNewUserEnters_graceStartedAndAccessGranted() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        registerUsers(licenseHandler, QUOTA);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.getGracePeriodStarted());
        boolean wasAccessGranted = licenseHandler.registerUser("bob");
        Assert.assertTrue(
                "Was not able to register a new person with the grace period",
                wasAccessGranted);
        Assert.assertNotNull(
                "Grace period was not automatically started from going over the limit",
                licenseHandler.getGracePeriodStarted());
        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(4,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceOngoingNewUserEnters_accessGranted() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        registerUsers(licenseHandler, QUOTA + 1);
        Assert.assertNotNull("Grace period was expected to be ongoing",
                licenseHandler.getGracePeriodStarted());
        boolean wasAccessGranted = licenseHandler.registerUser("bob");
        Assert.assertTrue(
                "Was not able to register a new person with the grace period",
                wasAccessGranted);
        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(5,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void registerUser_normalQuotaFullNoGraceExistingUserEnters_accessGrantedNoGracePeriod() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        List<String> users = registerUsers(licenseHandler, QUOTA);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.getGracePeriodStarted());
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(0));
        Assert.assertTrue("Was not able to register an existing user",
                wasAccessGranted);
        Assert.assertNull("Grace period was expected to not have started",
                licenseHandler.getGracePeriodStarted());
    }

    @Test
    public void registerUser_tenTimesQuotaAvailableInGrace_accessGranted() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        List<String> users = generateIds(GRACE_QUOTA);
        for (String user : users) {
            Assert.assertTrue("User wasn't given access",
                    licenseHandler.registerUser(user));
        }
        Assert.assertEquals(GRACE_QUOTA,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void registerUser_usersExceedingGraceQuota_accessDenied() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler.registerUser("steve");
        Assert.assertFalse("User should have been denied access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullNormalQuotaUserReturns_accessGranted() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        List<String> users = registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(1));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullGraceQuotaUserReturns_accessGranted() {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        List<String> users = registerUsers(licenseHandler, GRACE_QUOTA);
        boolean wasAccessGranted = licenseHandler
                .registerUser(users.get(QUOTA * 2));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(GRACE_QUOTA,
                licenseHandler.getStatistics().get(getCurrentMonth()).size());
    }

    @Test
    public void unSetGracePeriodReadFromFile_gracePeriodIsNull()
            throws IOException {
        writeToStatsFile(
                "{\"licenseKey\":\"123\",\"statistics\":{\"2020-05\":[\"userId-1\",\"userId-2\""
                        + "]},\"licenseEvents\":{}}");
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        Assert.assertNull(licenseHandler.getGracePeriodStarted());
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
        MockConnectionContext spyContext = new MockConnectionContext();
        spyContext.setEager(true);
        getCollaborationEngine().requestAccess(spyContext, user, result -> {
        });
        Assert.assertTrue(spyContext.getDispatchActionCount() > 0);
    }

    @Test
    public void requestAccess_uiIsAccessed() {
        UserInfo user = new UserInfo("steve");
        MockUI ui = new MockUI();
        UI.setCurrent(ui);
        getCollaborationEngine().requestAccess(user, result -> {
        });
        Assert.assertFalse(ui.getAccessTasks().isEmpty());
        UI.setCurrent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void requestAccess_throwsIfNoUiAvailable() {
        UserInfo user = new UserInfo("steve");
        getCollaborationEngine().requestAccess(user, result -> {
        });
    }

    @Test
    public void requestAccess_licenseCheckingNotEnabled_resolvesWithTrue() {
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(false);
        MockConnectionContext spyContext = new MockConnectionContext();
        spyContext.setEager(true);
        configuration.setLicenseCheckingEnabled(false);
        getCollaborationEngine().requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertTrue(result.get());

    }

    @Test
    public void requestAccess_userHasAccess_resolvesWithTrue() {
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(false);
        MockConnectionContext spyContext = new MockConnectionContext();
        spyContext.setEager(true);
        getCollaborationEngine().requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertTrue(result.get());
    }

    @Test
    public void requestAccess_userDoesNotHaveAccess_resolvesWithFalse() {
        fillGraceQuota();
        UserInfo user = new UserInfo("steve");
        AtomicBoolean result = new AtomicBoolean(true);
        MockConnectionContext spyContext = new MockConnectionContext();
        spyContext.setEager(true);
        getCollaborationEngine().requestAccess(spyContext, user, response -> {
            result.set(response.hasAccess());
        });
        Assert.assertFalse(result.get());
    }

    @Test
    public void requestAccess_invalidLicense_throwsException()
            throws IOException {
        writeToLicenseFile("{checksum:\"foo\"}");

        exception.expect(IllegalStateException.class);
        exception.expectMessage(INVALID_LICENSE);

        UserInfo user = new UserInfo("steve");
        MockConnectionContext spyContext = MockConnectionContext.createEager();
        getCollaborationEngine().requestAccess(spyContext, user, response -> {
        });
    }

    @Test
    public void registerUser_licenseLastDayValid_accessGranted()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        writeToLicenseFile(3, dateNow);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(dateNow);
        Assert.assertTrue("User should have been given access",
                licenseHandler.registerUser("userId-1"));
    }

    @Test
    public void registerUser_licenseExpired_accessDenied() throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(dateNow);
        Assert.assertFalse("User should have been denied access",
                licenseHandler.registerUser("userId-1"));
    }

    @Test
    public void licenseEventHandler_licenseExpired_eventFiredOnce()
            throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));

        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        setCurrentDate(dateNow.minusDays(1));
        licenseHandler.registerUser("steve");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRED);
        Assert.assertFalse(eventFired);

        setCurrentDate(dateNow);
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

        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        setCurrentDate(dateNow.minusDays(1));
        licenseHandler.registerUser("steve");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRES_SOON);
        Assert.assertFalse(eventFired);

        setCurrentDate(dateNow);
        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.LICENSE_EXPIRES_SOON);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void licenseEventHandler_gracePeriodStarted_eventFiredOnce()
            throws IOException {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
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

        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        setCurrentDate(dateNow);
        registerUsers(licenseHandler, 4);
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertFalse(eventFired);

        setCurrentDate(dateNow.plusDays(30));
        licenseHandler.registerUser("steve");
        eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertFalse(eventFired);

        setCurrentDate(dateNow.plusDays(31));
        licenseHandler.registerUser("foo");
        licenseHandler.registerUser("bar");
        int firedEvents = spyEventHandler.getHandledEvents()
                .get(LicenseEventType.GRACE_PERIOD_ENDED);
        Assert.assertEquals(1, firedEvents);
    }

    @Test
    public void readEventFiredDateFromStatsFile_licenseExpired_eventNotFiredAgain()
            throws IOException {
        writeToStatsFile("{\"licenseKey\":\"" + LICENSE_KEY + ""
                + "\",\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},"
                + "\"licenseEvents\":{\"licenseExpired\":\"2020-06-11\"}}");
        LocalDate dateNow = LocalDate.of(2020, 6, 11);
        writeToLicenseFile(3, LocalDate.of(2020, 6, 10));
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        setCurrentDate(dateNow);
        licenseHandler.registerUser("foo");
        boolean eventFired = spyEventHandler.getHandledEvents()
                .containsKey(LicenseEventType.LICENSE_EXPIRED);
        Assert.assertFalse(eventFired);
    }

    @Test
    public void licenseEventHandler_gracePeriodStarted_messageHasCorrectDate()
            throws IOException {
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
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

        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        setCurrentDate(dateNow);
        licenseHandler.registerUser("steve");

        String message = spyEventHandler.getMessages().get(0);
        Assert.assertThat(message, containsString("2020-07-10"));
    }

    @Test
    public void configureCustomStorage_customStorageIsUsed() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        configuration.setLicenseStorage(storage);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        licenseHandler.registerUser("foo");

        Assert.assertEquals(1, storage.licenses.size());
    }

    @Test
    public void licenseSystemPropertySet_readLicenseFromSystemProperty()
            throws IOException {
        byte[] license = licenseGenerator
                .generateLicense("Foo", 100, LocalDate.of(2020, 3, 4))
                .getBytes();
        String encodedLicense = Base64.getEncoder().encodeToString(license);

        // System property cleared in the cleanUp method
        System.setProperty(
                CollaborationEngineConfiguration.LICENSE_PUBLIC_PROPERTY,
                encodedLicense);

        LicenseInfoWrapper wrapper = LicenseHandler.MAPPER.readValue(license,
                LicenseInfoWrapper.class);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);

        Assert.assertEquals(wrapper.content.key, licenseHandler.license.key);
    }

    @Test
    public void licenseSystemPropertyInvalid_throwsProperException() {
        // System property cleared in the cleanUp method
        System.setProperty(
                CollaborationEngineConfiguration.LICENSE_PUBLIC_PROPERTY,
                "invalid");

        exception.expect(IllegalStateException.class);
        exception.expectMessage(INVALID_LICENSE);

        new LicenseHandler(ceSupplier);
    }

    @Test
    public void twoBackendNodes_firstIsLeader() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        CollaborationEngine node1 = createNode(storage);
        CollaborationEngine node2 = createNode(storage);

        Assert.assertTrue(node1.getLicenseHandler().isLeader());
        Assert.assertFalse(node2.getLicenseHandler().isLeader());
    }

    @Test
    public void twoBackendNodes_firstLeave_secondBecomesLeader() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        CollaborationEngine node1 = createNode(storage);
        CollaborationEngine node2 = createNode(storage);

        leave(node1);

        Assert.assertTrue(node2.getLicenseHandler().isLeader());
    }

    @Test
    public void twoBackendNodes_firstAddsUserEntry_secondIsUpdated() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        CollaborationEngine node1 = createNode(storage);
        CollaborationEngine node2 = createNode(storage);

        node1.getLicenseHandler().registerUser("user-1");

        Assert.assertTrue(node2.getLicenseHandler().getStatistics()
                .get(YearMonth.now()).contains("user-1"));
    }

    @Test
    public void twoBackendNodes_secondAddsUserEntry_entryIsSavedToStorage() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        CollaborationEngine node1 = createNode(storage);
        CollaborationEngine node2 = createNode(storage);

        node2.getLicenseHandler().registerUser("user-1");

        Assert.assertTrue(
                storage.getUserEntries(LICENSE_KEY.toString(), YearMonth.now())
                        .contains("user-1"));
    }

    @Test
    public void existingStorageData_newBackendNodeJoins_receivesCurrentData() {
        CustomLicenseStorage storage = new CustomLicenseStorage();
        YearMonth month = YearMonth.now();
        storage.addUserEntry(LICENSE_KEY.toString(), month, "user-1");

        CollaborationEngine node1 = createNode(storage);
        CollaborationEngine node2 = createNode(storage);

        Assert.assertTrue(node2.getLicenseHandler().getStatistics().get(month)
                .contains("user-1"));
    }

    private CollaborationEngine createNode(LicenseStorage storage) {
        SpyLicenseEventHandler spyEventHandler = new SpyLicenseEventHandler();
        MockConfiguration conf = new MockConfiguration(spyEventHandler);
        conf.setDataDirPath(testDataDir);
        conf.setLicenseCheckingEnabled(true);
        conf.setBackend(backendFactory.createBackend());
        conf.setLicenseStorage(storage);

        CollaborationEngine ce = CollaborationEngine.configure(
                new MockService(), conf, new TestUtil.TestCollaborationEngine(),
                true);
        backendFactory.join(conf.getBackend());
        ce.ensureConfigAndLicenseHandlerInitialization();
        return ce;
    }

    private void join(CollaborationEngine node) {
        backendFactory.join(node.getConfiguration().getBackend());
    }

    private void leave(CollaborationEngine node) {
        backendFactory.leave(node.getConfiguration().getBackend());
    }

    private LicenseHandler getLicenseHandlerWithGracePeriod(
            int daysSinceGracePeriodStarted) throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        LocalDate graceStart = dateNow.minusDays(daysSinceGracePeriodStarted);

        writeToStatsFile("{\"licenseKey\":\"" + LICENSE_KEY
                + "\",\"statistics\":{\""
                + YearMonth.from(dateNow).minusMonths(1)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"],\""
                + YearMonth.from(dateNow)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"]},"
                + "\"licenseEvents\":{\"gracePeriodStarted\":\"" + graceStart
                + "\"}}");
        setCurrentDate(dateNow);
        LicenseHandler licenseHandler = new LicenseHandler(ceSupplier);
        return licenseHandler;
    }

    private List<String> registerUsers(LicenseHandler licenseHandler, int i) {
        List<String> users = generateIds(i);
        users.forEach(licenseHandler::registerUser);
        return users;
    }

    static class CustomLicenseStorage implements LicenseStorage {

        private class LicenseData {
            private final Map<YearMonth, Set<String>> userEntries = new HashMap<>();
            private final Map<String, LocalDate> licenseEvents = new HashMap<>();
        }

        private final Map<String, LicenseData> licenses = new HashMap<>();

        @Override
        public List<String> getUserEntries(String licenseKey, YearMonth month) {
            return List.copyOf(getLicense(licenseKey).userEntries
                    .getOrDefault(month, Collections.emptySet()));
        }

        @Override
        public void addUserEntry(String licenseKey, YearMonth month,
                String payload) {
            getLicense(licenseKey).userEntries
                    .computeIfAbsent(month, k -> new LinkedHashSet<>())
                    .add(payload);
        }

        @Override
        public Map<String, LocalDate> getLatestLicenseEvents(
                String licenseKey) {
            return Map.copyOf(getLicense(licenseKey).licenseEvents);
        }

        @Override
        public void setLicenseEvent(String licenseKey, String eventName,
                LocalDate latestOccurrence) {
            getLicense(licenseKey).licenseEvents.put(eventName,
                    latestOccurrence);
        }

        private LicenseData getLicense(String licenseKey) {
            return licenses.computeIfAbsent(licenseKey, k -> new LicenseData());
        }
    }
}
