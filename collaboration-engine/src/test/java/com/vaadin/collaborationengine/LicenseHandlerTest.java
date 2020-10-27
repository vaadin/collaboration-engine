package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public class LicenseHandlerWithMonthControl extends LicenseHandler {

        @Override
        protected YearMonth getCurrentMonth() {
            return configuredYearMonth;
        }

        protected void setCurrentMonth(int year, int month) {
            configuredYearMonth = YearMonth.of(year, month);
        }
    }

    private YearMonth configuredYearMonth = YearMonth.of(2020, 5);
    private LicenseHandlerWithMonthControl licenseHandler;
    private CollaborationEngine ce;

    @Before
    public void init() throws IOException {
        FileHandler.setDataDirectory(Paths.get(System.getProperty("user.home"),
                ".vaadin", "ce-tests"));
        if (!FileHandler.getDataDirPath().toFile().exists()) {
            Files.createDirectories(FileHandler.getDataDirPath());
        }

        // Delete the stats file before each run to make sure we can test with a
        // clean state
        Files.deleteIfExists(FileHandler.getStatsFilePath());

        // Set quota to 3
        writeToLicenseFile("{\"quota\":3,\"endDate\":\"2222-01-01\"}");

        licenseHandler = new LicenseHandlerWithMonthControl();

        ce = new CollaborationEngine(true, (topicId, isActive) -> {
            // NO-OP
        });
    }

    @After
    public void cleanUp() {
        FileHandler.setDataDirectory(FileHandler.DEFAULT_DATA_DIR);
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        Assert.assertTrue(this.licenseHandler.getStatistics().isEmpty());
        this.licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(1, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        this.licenseHandler.registerUser("bob");

        statistics = this.licenseHandler.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(2, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        Assert.assertTrue(statistics.get(configuredYearMonth).contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Set<String> usersInMonth = statistics.get(configuredYearMonth);
        Assert.assertEquals(2, usersInMonth.size());
        Assert.assertTrue(usersInMonth.contains("steve"));
        Assert.assertTrue(usersInMonth.contains("bob"));
    }

    @Test
    public void registerUser_monthChanges_userIsReAdded() {
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.setCurrentMonth(2020, 6);
        licenseHandler.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.licenseHandler
                .getStatistics();
        YearMonth firstMonth = YearMonth.of(2020, 5);
        YearMonth secondMonth = YearMonth.of(2020, 6);
        Assert.assertEquals(2, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(firstMonth));
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
        Assert.assertTrue(statistics.keySet().contains(YearMonth.now()));
        Set<String> currentMonth = statistics.get(YearMonth.now());
        Assert.assertEquals(1, currentMonth.size());
        Assert.assertTrue(currentMonth.contains("steve"));
    }

    @Test
    public void noStatsFile_initStatistics_hasEmptyMap() throws IOException {
        Files.deleteIfExists(FileHandler.getStatsFilePath());
        LicenseHandler stats = new LicenseHandler();
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(FileHandler.getStatsFilePath()));
        Assert.assertEquals(Collections.emptyMap(), stats.getStatistics());
    }

    @Test
    public void noStatsFile_registerUser_statsFileCreated() throws IOException {
        Files.deleteIfExists(FileHandler.getStatsFilePath());
        licenseHandler.setCurrentMonth(2020, 2);
        licenseHandler.registerUser("steve");
        assertStatsFileContent("{\"statistics\":{\"2020-02\":[\"steve\"]}}");
    }

    @Test
    public void statsFileHasData_initStatistics_readsDataFromFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2000-01\":[\"bob\"]}}");
        LicenseHandler stats = new LicenseHandler();

        Assert.assertEquals(1, stats.getStatistics().size());
        Assert.assertEquals(Collections.singleton("bob"),
                stats.getStatistics().get(YearMonth.of(2000, 1)));
    }

    @Test
    public void statsFileHasData_initStatistics_registerUser_allDataIncludedInFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2002-01\":[\"bob\"]}}");
        LicenseHandlerWithMonthControl stats = new LicenseHandlerWithMonthControl();
        stats.setCurrentMonth(2002, 1);
        stats.registerUser("steve");
        Assert.assertEquals(1, stats.getStatistics().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("bob", "steve")),
                stats.getStatistics().get(YearMonth.of(2002, 1)));
        assertStatsFileContent(
                "{\"statistics\":{\"2002-01\":[\"bob\",\"steve\"]}}");
    }

    @Test
    public void registerMultipleUsersAndMonths_writeToFile_readFromFile() {
        licenseHandler.setCurrentMonth(2020, 2);
        licenseHandler.registerUser("steve");
        licenseHandler.registerUser("bob");
        licenseHandler.registerUser("steve");

        licenseHandler.setCurrentMonth(2020, 3);
        licenseHandler.registerUser("steve");

        assertStatsFileContent(
                "{\"statistics\":{\"2020-02\":[\"steve\",\"bob\"],"
                        + "\"2020-03\":[\"steve\"]}}");

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
        Files.deleteIfExists(FileHandler.getLicenseFilePath());
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
    public void registerUser_quotaExceeded_userNotRegistered() {
        List<String> userIds = generateIds(5);
        userIds.forEach(licenseHandler::registerUser);

        Set<String> expected = new HashSet<>(userIds.subList(0, 3));

        Assert.assertEquals(1, licenseHandler.getStatistics().size());
        Assert.assertEquals(expected, licenseHandler.getStatistics()
                .get(licenseHandler.getCurrentMonth()));
    }

    @Test
    public void openTopicConnection_quotaExceeded_connectionNotActivated() {
        List<String> usersWithConnectionActivated = new ArrayList<>();

        generateIds(5).forEach(userId -> {
            ce.openTopicConnection(new EagerConnectionContext(), "topic",
                    new UserInfo(userId), topicConnection -> {
                        usersWithConnectionActivated.add(userId);
                        return null;
                    });
        });

        Assert.assertEquals(Arrays.asList("0", "1", "2"),
                usersWithConnectionActivated);
    }

    @Test
    public void openTopicConnection_quotaExceededButUserHasSeat_connectionActivated() {
        List<String> userIds = generateIds(5);
        userIds.forEach(userId -> {
            ce.openTopicConnection(new EagerConnectionContext(), "topic",
                    new UserInfo(userId), topicConnection -> null);
        });

        AtomicBoolean connectionActivated = new AtomicBoolean(false);
        ce.openTopicConnection(new EagerConnectionContext(), "topic",
                new UserInfo(userIds.get(0)), topicConnection -> {
                    Assert.assertFalse(connectionActivated.get());
                    connectionActivated.set(true);
                    return null;
                });

        Assert.assertTrue(connectionActivated.get());
    }

    private void assertStatsFileContent(String expected) {
        String fileContent = null;
        try {
            fileContent = new String(
                    Files.readAllBytes(FileHandler.getStatsFilePath()));
        } catch (IOException e) {
            Assert.fail("Failed to read the file at "
                    + FileHandler.getStatsFilePath());
        }
        Assert.assertEquals("Unexpected statistics file content", expected,
                fileContent);
    }

    private void writeToStatsFile(String content) throws IOException {
        Files.write(FileHandler.getStatsFilePath(), content.getBytes());
    }

    private void writeToLicenseFile(String content) throws IOException {
        Files.write(FileHandler.getLicenseFilePath(), content.getBytes());
    }

    private List<String> generateIds(int count) {
        return IntStream.range(0, count).mapToObj(String::valueOf)
                .collect(Collectors.toList());
    }
}
