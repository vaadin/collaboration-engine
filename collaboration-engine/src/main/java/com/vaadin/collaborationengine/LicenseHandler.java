/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.LoggerFactory;

import com.vaadin.collaborationengine.CollaborationEngine.CollaborationEngineConfig;

class LicenseHandler {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LicenseInfo {
        final int quota;
        final LocalDate endDate;

        @JsonCreator
        LicenseInfo(@JsonProperty(value = "quota", required = true) int quota,
                @JsonProperty(value = "endDate", required = true) LocalDate endDate) {
            this.quota = quota;
            this.endDate = endDate;
        }
    }

    static class LicenseInfoWrapper {

        final LicenseInfo content;

        final String checksum;

        @JsonCreator
        LicenseInfoWrapper(
                @JsonProperty(value = "content", required = true) LicenseInfo content,
                @JsonProperty(value = "checksum", required = true) String checksum) {
            this.content = content;
            this.checksum = checksum;
        }
    }

    static class StatisticsInfo {
        Map<YearMonth, Set<String>> statistics;
        LocalDate gracePeriodStart;

        StatisticsInfo(
                @JsonProperty(value = "statistics", required = true) Map<YearMonth, List<String>> userIdsFromFile,
                @JsonProperty(value = "gracePeriodStart") LocalDate gracePeriodStart) {
            this.statistics = copyMap(userIdsFromFile);
            this.gracePeriodStart = gracePeriodStart;
        }

        private Map<YearMonth, Set<String>> copyMap(
                Map<YearMonth, ? extends Collection<String>> map) {
            TreeMap<YearMonth, Set<String>> treeMap = new TreeMap<>();
            for (Map.Entry<YearMonth, ? extends Collection<String>> month : map
                    .entrySet()) {
                treeMap.put(month.getKey(),
                        new LinkedHashSet<>(month.getValue()));
            }
            return treeMap;
        }
    }

    private final FileHandler fileHandler;
    private final LicenseInfo license;
    final StatisticsInfo statistics;

    LicenseHandler(CollaborationEngineConfig config) {
        if (config.licenseCheckingEnabled) {
            fileHandler = new FileHandler(config);
            license = fileHandler.readLicenseFile();
            statistics = fileHandler.readStatsFile();
            if (license.endDate.isBefore(getCurrentDate())) {
                LoggerFactory.getLogger(CollaborationEngine.class)
                        .warn("Your Collaboration Engine license has expired. "
                                + "Your application will still continue to "
                                + "work, but the collaborative features will be "
                                + "disabled. Please contact Vaadin about "
                                + "obtaining a new, up-to-date license for "
                                + "your application. "
                                + "https://vaadin.com/collaboration");
            }
        } else {
            fileHandler = null;
            license = null;
            statistics = null;
            if (config.dataDirPath == null) {
                LoggerFactory.getLogger(CollaborationEngine.class).warn(
                        "Collaboration Engine is used in development/trial mode. "
                                + "Note that in order to make a production build, "
                                + "you need to obtain a license from Vaadin and configure the '"
                                + FileHandler.DATA_DIR_PUBLIC_PROPERTY
                                + "' property, which is currently not configured. "
                                + "More info in Vaadin documentation.");
            }
        }
    }

    /**
     * Tries to register a seat for the user with the given id for the current
     * calendar month. Returns whether the user has a seat or not. If license
     * checking/statistics is not enabled, just returns {@code true}.
     *
     * @param userId
     *            the user id to register
     * @return {@code true} if the user can use Collaboration Engine,
     *         {@code false} otherwise
     */
    synchronized boolean registerUser(String userId) {
        LocalDate currentDate = getCurrentDate();

        if (license.endDate.isBefore(currentDate)) {
            return false;
        }

        Set<String> users = statistics.statistics.computeIfAbsent(
                YearMonth.from(currentDate),
                yearMonth -> new LinkedHashSet<>());

        int effectiveQuota = isGracePeriodOngoing(currentDate)
                ? license.quota * 10
                : license.quota;

        boolean hasActiveSeat = users.size() <= effectiveQuota
                ? users.contains(userId)
                : users.stream().limit(effectiveQuota)
                        .anyMatch(user -> user.equals(userId));

        if (hasActiveSeat) {
            return true;
        }

        if (users.size() >= effectiveQuota) {
            if (statistics.gracePeriodStart != null) {
                return false;
            }
            statistics.gracePeriodStart = currentDate;
        }

        users.add(userId);
        fileHandler.writeStats(statistics);
        return true;
    }

    private boolean isGracePeriodOngoing(LocalDate currentDate) {
        return statistics.gracePeriodStart != null
                && statistics.gracePeriodStart
                        .isAfter(currentDate.minusDays(31));
    }

    LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /*
     * For testing internal state of Statistics gathering
     */
    Map<YearMonth, Set<String>> getStatistics() {
        return statistics.copyMap(statistics.statistics);
    }

}
