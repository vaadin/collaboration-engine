package com.vaadin.collaborationengine;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;

import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfo;
import com.vaadin.collaborationengine.util.MockConnectionContext;

public class StatsFilePerformanceTest extends AbstractLicenseTest {

    @Test
    @Ignore
    // Run manually when needed and read the console logs for results
    public void testPerformance() {
        log("\nMeasuring how long openTopicConnection takes (in milliseconds)\n"
                + "in production mode when the stats file contains a lot of data.");

        testPerformance(6, 10000, 5, 5);
        testPerformance(12, 15000, 5, 5);
    }

    /**
     * Measures the time of opening a topic connection, when there's statistics
     * data that is edited and written to a file for each new monthly end user.
     *
     * @param months
     *            how many months of user data the statistics should have
     *            initially
     * @param usersPerMonth
     *            how many user ids should be registered for each of the months
     *            initially
     * @param repeat
     *            how many times the test should be repeated
     * @param warmUpRounds
     *            how many times the test should be repeated silently before the
     *            actual test rounds (helps to get more consistent results)
     */
    private void testPerformance(int months, int usersPerMonth, int repeat,
            int warmUpRounds) {

        log(String.format(
                "\nTesting with stats containing %s users for %s months.\n",
                usersPerMonth, months));

        List<List<Double>> allResults = IntStream
                .range(0, warmUpRounds + repeat)
                .mapToObj(i -> testPerformance(months, usersPerMonth))
                .skip(warmUpRounds).collect(Collectors.toList());

        reportResults(allResults);
    }

    private List<Double> testPerformance(int months, int usersPerMonth) {

        try {
            super.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        generateStatsFile(months, usersPerMonth);

        // File is read and then written.
        double firstConnectionAfterStartupResult = measureOpenTopicConnection(
                "foo");

        // File is only written.
        double newMonthlyUserResult = measureOpenTopicConnection("bar");

        // File is neither read or written.
        double existingMonthlyUserResult = measureOpenTopicConnection("foo");

        List<Double> results = Arrays.asList(firstConnectionAfterStartupResult,
                newMonthlyUserResult, existingMonthlyUserResult);

        return results;
    }

    private double measureOpenTopicConnection(String userId) {

        final long startTime = System.nanoTime();

        final AtomicReference<Double> result = new AtomicReference<>(null);

        ce.openTopicConnection(MockConnectionContext.createEager(), "topic-id",
                new UserInfo(userId), topicConnection -> {
                    final long endTime = System.nanoTime();
                    result.set((endTime - startTime) / 1000000d);
                    return null;
                });
        assert result.get() != null;
        return result.get();
    }

    private void generateStatsFile(int months, int usersPerMonth) {

        List<String> generatedUserIds = IntStream.range(0, usersPerMonth)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        Map<YearMonth, List<String>> userIdsPerMonth = new HashMap<>();
        IntStream.range(0, months).forEach(i -> {
            YearMonth month = YearMonth.of(2000, 1).plusMonths(i);
            userIdsPerMonth.put(month, new ArrayList<>(generatedUserIds));
        });

        StatisticsInfo statisticsInfo = new StatisticsInfo("licenseKey",
                userIdsPerMonth, Collections.emptyMap());

        writeToStatsFile(statisticsInfo);
    }

    private void reportResults(List<List<Double>> allResults) {

        StringBuilder report = new StringBuilder();

        report.append(rowToString("", "1st after startup", "New monthly user",
                "Registered user"));
        report.append(
                rowToString("", "(read + write)", "(write)", "(no file I/O)"));
        report.append(rowToString("", "---", "---", "---"));

        IntStream.range(0, allResults.size()).forEach(i -> {
            List<Double> result = allResults.get(i);
            String row = rowToString("Run " + (i + 1), result);
            report.append(row);
        });
        report.append(rowToString("Average", averages(allResults)));

        log(report.toString());
    }

    private String rowToString(String name, List<Double> numbers) {
        Object[] params = Stream.concat(Stream.of(name),
                // Show only 3 decimals of the milliseconds:
                numbers.stream().map(d -> String.format("%.3f", d))).toArray();
        return rowToString(params);
    }

    private String rowToString(Object... params) {
        StringBuilder s = new StringBuilder();
        // Formats each "cell" to be x chars wide
        IntStream.range(0, params.length).forEach(i -> s.append("%20s"));
        return String.format(s.toString() + "\n", params);
    }

    private void log(String message) {
        System.out.println(message);
    }

    private List<Double> averages(List<List<Double>> input) {
        int size = input.get(0).size();

        return IntStream.range(0, size)
                .mapToObj(i -> input.stream()
                        .collect(Collectors.summarizingDouble(
                                innerList -> innerList.get(i)))
                        .getAverage())
                .collect(Collectors.toList());
    }
}
