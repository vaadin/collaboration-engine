package com.vaadin.collaborationengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.collaborationengine.util.EagerConnectionContext;

public class StatisticsTest {
    private YearMonth configuredYearMonth = YearMonth.of(2020, 5);

    public class StatisticsWithMonthControl extends Statistics {

        @Override
        protected YearMonth getCurrentMonth() {
            return configuredYearMonth;
        }

        protected void setCurrentMonth(int year, int month) {
            configuredYearMonth = YearMonth.of(year, month);
        }
    }

    private StatisticsWithMonthControl statistics;

    @Before
    public void init() throws IOException {
        FileHandler.setDataDirectory(Paths.get(System.getProperty("user.home"),
                ".vaadin", "ce-tests"));

        // Delete the stats file before each run to make sure we can test with a
        // clean state
        Files.deleteIfExists(FileHandler.getStatsFilePath());
        statistics = new StatisticsWithMonthControl();
    }

    @After
    public void cleanUp() {
        FileHandler.setDataDirectory(FileHandler.DEFAULT_DATA_DIR);
    }

    @Test
    public void registerUser_addUsers_usersAdded() {
        Assert.assertTrue(this.statistics.getStatistics().isEmpty());
        this.statistics.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.statistics
                .getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(1, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        this.statistics.registerUser("bob");

        statistics = this.statistics.getStatistics();
        Assert.assertEquals(1, statistics.keySet().size());
        Assert.assertTrue(statistics.keySet().contains(configuredYearMonth));
        Assert.assertEquals(2, statistics.get(configuredYearMonth).size());
        Assert.assertTrue(
                statistics.get(configuredYearMonth).contains("steve"));
        Assert.assertTrue(statistics.get(configuredYearMonth).contains("bob"));
    }

    @Test
    public void registerUser_addSameUserTwice_userAddedOnlyOnce() {
        statistics.registerUser("steve");
        statistics.registerUser("bob");
        statistics.registerUser("steve");
        Map<YearMonth, Set<String>> statistics = this.statistics
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
        statistics.registerUser("steve");
        statistics.registerUser("bob");
        statistics.setCurrentMonth(2020, 6);
        statistics.registerUser("steve");

        Map<YearMonth, Set<String>> statistics = this.statistics
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
        CollaborationEngine engine = new CollaborationEngine();
        engine.openTopicConnection(new EagerConnectionContext(), "foo",
                new UserInfo("steve"), topicConnection -> null);
        Map<YearMonth, Set<String>> statistics = engine.getStatistics()
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
        Statistics stats = new Statistics();
        Assert.assertFalse("Expected the stats file not to exist.",
                Files.exists(FileHandler.getStatsFilePath()));
        Assert.assertEquals(Collections.emptyMap(), stats.getStatistics());
    }

    @Test
    public void noCollaborationEngineDirectory_initStatistics_hasEmptyMap()
            throws IOException {
        if (Files.exists(FileHandler.getDataDirPath())) {
            deleteDirectoryRecursively(FileHandler.getDataDirPath());
        }
        Statistics stats = new Statistics();
        Assert.assertFalse(
                "Expected the Collaboration Engine directory not to exist.",
                Files.exists(FileHandler.getDataDirPath()));
        Assert.assertEquals(Collections.emptyMap(), stats.getStatistics());
    }

    @Test
    public void noStatsFile_registerUser_statsFileCreated() throws IOException {
        Files.deleteIfExists(FileHandler.getStatsFilePath());
        statistics.setCurrentMonth(2020, 2);
        statistics.registerUser("steve");
        assertStatsFileContent("{\"statistics\":{\"2020-02\":[\"steve\"]}}");
    }

    @Test
    public void noCollaborationEngineDirectory_registerUser_folderAndStatsFileCreated()
            throws IOException {
        if (Files.exists(FileHandler.getDataDirPath())) {
            deleteDirectoryRecursively(FileHandler.getDataDirPath());
        }
        Assert.assertFalse(
                "Expected the Collaboration Engine directory not to exist.",
                Files.exists(FileHandler.getDataDirPath()));
        statistics.setCurrentMonth(2020, 1);
        statistics.registerUser("bob");
        assertStatsFileContent("{\"statistics\":{\"2020-01\":[\"bob\"]}}");
    }

    @Test
    public void statsFileHasData_initStatistics_readsDataFromFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2000-01\":[\"bob\"]}}");
        Statistics stats = new Statistics();

        Assert.assertEquals(1, stats.getStatistics().size());
        Assert.assertEquals(Collections.singleton("bob"),
                stats.getStatistics().get(YearMonth.of(2000, 1)));
    }

    @Test
    public void statsFileHasData_initStatistics_registerUser_allDataIncludedInFile()
            throws IOException {
        writeToStatsFile("{\"statistics\":{\"2002-01\":[\"bob\"]}}");
        StatisticsWithMonthControl stats = new StatisticsWithMonthControl();
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
        statistics.setCurrentMonth(2020, 2);
        statistics.registerUser("steve");
        statistics.registerUser("bob");
        statistics.registerUser("steve");

        statistics.setCurrentMonth(2020, 3);
        statistics.registerUser("steve");

        assertStatsFileContent(
                "{\"statistics\":{\"2020-02\":[\"steve\",\"bob\"],"
                        + "\"2020-03\":[\"steve\"]}}");

        Map<YearMonth, Set<String>> newStats = new Statistics().getStatistics();
        Assert.assertEquals(2, newStats.size());
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("steve", "bob")),
                newStats.get(YearMonth.of(2020, 2)));
        Assert.assertEquals(Collections.singleton("steve"),
                newStats.get(YearMonth.of(2020, 3)));
    }

    @Test(expected = IllegalStateException.class)
    public void statsFileHasInvalidData_initStatistics_throws()
            throws IOException {
        writeToStatsFile("I'm invalid");
        new Statistics();
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

    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
    }
}
