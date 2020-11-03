package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;

public class LicenseHandlerTest {

    public static class MockedLicenseHandler extends LicenseHandler {

        private LocalDate configuredCurrentDate = LocalDate.of(2020, 5, 1);

        protected void setCurrentDate(LocalDate currentDate) {
            configuredCurrentDate = currentDate;
        }

        @Override
        public LocalDate getCurrentDate() {
            return configuredCurrentDate;
        }

        public YearMonth getCurrentMonth() {
            return YearMonth.from(configuredCurrentDate);
        }
    }

    private Path statsFilePath;
    private Path licenseFilePath;

    private MockedLicenseHandler licenseHandler;
    private CollaborationEngine ce;

    @Before
    public void init() throws IOException {
        FileHandler.dataDirPath = Paths.get(System.getProperty("user.home"),
                ".vaadin", "ce-tests");
        if (!FileHandler.dataDirPath.toFile().exists()) {
            Files.createDirectories(FileHandler.dataDirPath);
        }
        statsFilePath = FileHandler
                .createStatsFilePath(FileHandler.dataDirPath);
        licenseFilePath = FileHandler
                .createLicenseFilePath(FileHandler.dataDirPath);

        // Delete the stats file before each run to make sure we can test with a
        // clean state
        Files.deleteIfExists(statsFilePath);

        // Set quota to 3
        writeToLicenseFile("{\"quota\":3,\"endDate\":\"2222-01-01\"}");

        licenseHandler = new MockedLicenseHandler();

        ce = new CollaborationEngine(true, (topicId, isActive) -> {
            // NO-OP
        });
    }

    @After
    public void cleanUp() {
        FileHandler.dataDirPath = FileHandler.DEFAULT_DATA_DIR;
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        Assert.assertTrue(this.licenseHandler.getStatistics().isEmpty());
        this.licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(this.licenseHandler.getCurrentMonth()));
        Assert.assertEquals(1,
                statistics.get(this.licenseHandler.getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(this.licenseHandler.getCurrentMonth())
                .contains("steve"));
        this.licenseHandler.registerUser("bob");

        statistics = this.licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(this.licenseHandler.getCurrentMonth()));
        Assert.assertEquals(2,
                statistics.get(this.licenseHandler.getCurrentMonth()).size());
        Assert.assertTrue(statistics.get(this.licenseHandler.getCurrentMonth())
                .contains("steve"));
        Assert.assertTrue(statistics.get(this.licenseHandler.getCurrentMonth())
                .contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(
                statistics.containsKey(this.licenseHandler.getCurrentMonth()));
        Set<String> usersInMonth = statistics
                .get(this.licenseHandler.getCurrentMonth());
        Assert.assertEquals(2, usersInMonth.size());
        Assert.assertTrue(usersInMonth.contains("steve"));
        Assert.assertTrue(usersInMonth.contains("bob"));
    }

    @Test
    public void registerUser_monthChanges_userIsReAdded() {
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.setCurrentDate(LocalDate.of(2020, 6, 1));
        licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
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
        LicenseHandler licenseHandler = new LicenseHandler();
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertEquals(Collections.emptyMap(),
                licenseHandler.getStatistics());
    }

    @Test
    public void noStatsFile_initStatistics_hasNullGracePeriodStart()
            throws IOException {
        Files.deleteIfExists(statsFilePath);
        LicenseHandler licenseHandler = new LicenseHandler();
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(statsFilePath));
        Assert.assertNull("Grace period start should have been unset",
                licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void noStatsFile_registerUser_statsFileCreated() throws IOException {
        Files.deleteIfExists(statsFilePath);
        licenseHandler.setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");
        assertStatsFileContent(
                "{\"statistics\":{\"2020-02\":[\"steve\"]},\"gracePeriodStart\":null}");
    }

    @Test
    public void statsFileHasData_initStatistics_readsDataFromFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2000-01\":[\"bob\"]}}");
        LicenseHandler licenseHandler = new LicenseHandler();

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(Collections.singleton("bob"),
                licenseHandler.getStatistics().get(YearMonth.of(2000, 1)));
    }

    @Test
    public void statsFileHasData_initStatistics_registerUser_allDataIncludedInFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2020-01\":[\"bob\"]}}");
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler();
        licenseHandler.setCurrentDate(LocalDate.of(2020, 1, 1));
        licenseHandler.registerUser("steve");
        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("bob", "steve")),
                licenseHandler.getStatistics().get(YearMonth.of(2020, 1)));
        assertStatsFileContent(
                "{\"statistics\":{\"2020-01\":[\"bob\",\"steve\"]},\"gracePeriodStart\":null}");
    }

    @Test
    public void registerMultipleUsersAndMonths_writeToFile_readFromFile() {
        licenseHandler.setCurrentDate(LocalDate.of(2020, 2, 1));
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");

        licenseHandler.setCurrentDate(LocalDate.of(2020, 3, 1));
        licenseHandler.registerUser("steve");

        assertStatsFileContent(
                "{\"statistics\":{\"2020-02\":[\"steve\",\"bob\"],"
                        + "\"2020-03\":[\"steve\"]},\"gracePeriodStart\":null}");

        Map<YearMonth, Set<String>> newStats = new LicenseHandler()
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
        new LicenseHandler();
    }

    @Test(expected = IllegalStateException.class)
    public void noLicenseFile_initCollaborationEngine_throws()
            throws IOException {
        Files.deleteIfExists(licenseFilePath);
        new CollaborationEngine(true, (topicId, isActive) -> {
            // NO-OP
        });
    }

    @Test
    public void licenseFileHasInvalidData_initLicenseHandler_throws()
            throws IOException {
        for (String invalidLicenseContent : Arrays.asList("", "not json",
                // Missing quota:
                "{\"endDate\":\"2222-01-01\"}",
                // Missing endDate:
                "{\"quota\":100}",
                // Invalid quota:
                "{\"quota\":\"not a number\",\"endDate\":\"2222-01-01\"}",
                // Invalid date:
                "{\"quota\":100,\"endDate\":\"not a date\"}")) {

            writeToLicenseFile(invalidLicenseContent);
            try {
                new LicenseHandler();
                Assert.fail(
                        "Expected to throw because of invalid license file content: '"
                                + invalidLicenseContent + "'.");
            } catch (IllegalStateException e) {
                // Expected
            }
        }
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceeded_connectionNotActivated() {
        List<String> users = generateIds(35);
        List<String> usersWithConnectionActivated = new ArrayList<>();

        users.forEach(
                userId -> ce.openTopicConnection(new EagerConnectionContext(),
                        "topic", new UserInfo(userId), topicConnection -> {
                            usersWithConnectionActivated.add(userId);
                            return null;
                        }));

        Assert.assertEquals(users.subList(0, 30), usersWithConnectionActivated);
    }

    @Test
    public void openTopicConnection_effectiveQuotaExceededButUserHasSeat_connectionActivated() {
        List<String> userIds = generateIds(35);
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
        registerUsers(3);
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
        registerUsers(4);
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
        List<String> users = registerUsers(3);
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
        List<String> users = generateIds(30);
        for (String user : users) {
            Assert.assertTrue("User wasn't given access",
                    licenseHandler.registerUser(user));
        }
        Assert.assertEquals(30, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_usersExceedingGraceQuota_accessDenied() {
        registerUsers(30);
        boolean wasAccessGranted = licenseHandler.registerUser("steve");
        Assert.assertFalse("User should have been denied access",
                wasAccessGranted);
        Assert.assertEquals(30, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullNormalQuotaUserReturns_accessGranted() {
        List<String> users = registerUsers(30);
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(1));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(30, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void registerUser_graceQuotaFullGraceQuotaUserReturns_accessGranted() {
        List<String> users = registerUsers(30);
        boolean wasAccessGranted = licenseHandler.registerUser(users.get(7));
        Assert.assertTrue("User should have been given access",
                wasAccessGranted);
        Assert.assertEquals(30, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()).size());
    }

    @Test
    public void unSetGracePeriodReadFromFile_gracePeriodIsNull()
            throws IOException {
        writeToStatsFile(
                "{\"statistics\":{\"2020-05\":[\"userId-1\",\"userId-2\""
                        + "]}," + "\"gracePeriodStart\":null}");
        LicenseHandler licenseHandler = new LicenseHandler();
        Assert.assertNull(licenseHandler.statistics.gracePeriodStart);
    }

    @Test
    public void setGracePeriodReadFromFile_gracePeriodIsSet()
            throws IOException {
        writeToStatsFile(
                "{\"statistics\":{\"2020-05\":[\"userId-1\",\"userId-2\","
                        + "\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\","
                        + "\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"]},"
                        + "\"gracePeriodStart\":\"2020-10-28\"}");
        LicenseHandler licenseHandler = new LicenseHandler();
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

    private LicenseHandler getLicenseHandlerWithGracePeriod(
            int daysSinceGracePeriodStarted) throws IOException {
        LocalDate dateNow = LocalDate.of(2020, 6, 10);
        LocalDate graceStart = dateNow.minusDays(daysSinceGracePeriodStarted);

        writeToStatsFile("{\"statistics\":" + "{\""
                + YearMonth.from(dateNow).minusMonths(1)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"], \""
                + YearMonth.from(dateNow)
                + "\":[\"userId-1\",\"userId-2\",\"userId-3\",\"userId-4\",\"userId-5\",\"userId-6\",\"userId-7\",\"userId-8\",\"userId-9\",\"userId-10\"] },"
                + "\"gracePeriodStart\":\"" + graceStart + "\"}");
        MockedLicenseHandler licenseHandler = new MockedLicenseHandler();
        licenseHandler.setCurrentDate(dateNow);
        return licenseHandler;
    }

    private void assertStatsFileContent(String expected) {
        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(statsFilePath));
        } catch (IOException e) {
            Assert.fail("Failed to read the file at " + statsFilePath);
        }
        Assert.assertEquals("Unexpected statistics file content", expected,
                fileContent);
    }

    private void writeToStatsFile(String content) throws IOException {
        Files.write(statsFilePath, content.getBytes());
    }

    private void writeToLicenseFile(String content) throws IOException {
        Files.write(licenseFilePath, content.getBytes());
    }

    private List<String> generateIds(int count) {
        return IntStream.range(0, count).mapToObj(String::valueOf)
                .collect(Collectors.toList());
    }

    private List<String> registerUsers(int i) {
        List<String> users = generateIds(i);
        users.forEach(licenseHandler::registerUser);
        return users;
    }
}
