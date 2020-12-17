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
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;

class LicenseHandler {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LicenseInfo {
        final String key;
        final int quota;
        final LocalDate endDate;

        @JsonCreator
        LicenseInfo(@JsonProperty(value = "key", required = true) String key,
                @JsonProperty(value = "quota", required = true) int quota,
                @JsonProperty(value = "endDate", required = true) LocalDate endDate) {
            this.key = key;
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
        String licenseKey;
        Map<YearMonth, Set<String>> statistics;
        LocalDate gracePeriodStart;
        Map<LicenseEventType, LocalDate> licenseEvents;

        StatisticsInfo(
                @JsonProperty(value = "licenseKey", required = true) String licenseKey,
                @JsonProperty(value = "statistics", required = true) Map<YearMonth, List<String>> userIdsFromFile,
                @JsonProperty(value = "gracePeriodStart") LocalDate gracePeriodStart,
                @JsonProperty(value = "licenseEvents", required = true) Map<LicenseEventType, LocalDate> licenseEvents) {
            this.licenseKey = licenseKey;
            this.statistics = copyMap(userIdsFromFile);
            this.gracePeriodStart = gracePeriodStart;
            this.licenseEvents = new HashMap<>(licenseEvents);
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

    static class StatisticsInfoWrapper {

        final StatisticsInfo content;

        final String checksum;

        @JsonCreator
        StatisticsInfoWrapper(
                @JsonProperty(value = "content", required = true) StatisticsInfo content,
                @JsonProperty(value = "checksum", required = true) String checksum) {
            this.content = content;
            this.checksum = checksum;
        }
    }

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    private final CollaborationEngine ce;
    private final FileHandler fileHandler;
    private final LicenseInfo license;
    final StatisticsInfo statistics;

    LicenseHandler(CollaborationEngine collaborationEngine) {
        this.ce = collaborationEngine;
        CollaborationEngineConfiguration configuration = collaborationEngine
                .getConfiguration();
        if (configuration.isLicenseCheckingEnabled()) {
            fileHandler = new FileHandler(configuration);
            license = fileHandler.readLicenseFile();
            statistics = fileHandler.readStatsFile();
            if (!license.key.equals(statistics.licenseKey)) {
                statistics.licenseKey = license.key;
                statistics.gracePeriodStart = null;
                statistics.licenseEvents.clear();
            }
            if (license.endDate.isBefore(getCurrentDate())) {
                CollaborationEngine.LOGGER
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

        if (isGracePeriodEnded(currentDate)) {
            fireLicenseEvent(LicenseEventType.GRACE_PERIOD_ENDED);
        }
        if (license.endDate.isBefore(currentDate)) {
            fireLicenseEvent(LicenseEventType.LICENSE_EXPIRED);
            return false;
        }
        if (license.endDate.minusDays(31).isBefore(currentDate)) {
            fireLicenseEvent(LicenseEventType.LICENSE_EXPIRES_SOON);
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
            fireLicenseEvent(LicenseEventType.GRACE_PERIOD_STARTED);
        }

        users.add(userId);
        fileHandler.writeStats(statistics);
        return true;
    }

    private boolean isGracePeriodOngoing(LocalDate currentDate) {
        return statistics.gracePeriodStart != null
                && !isGracePeriodEnded(currentDate);
    }

    private boolean isGracePeriodEnded(LocalDate currentDate) {
        return statistics.gracePeriodStart != null
                && currentDate.isAfter(getLastGracePeriodDate());
    }

    private LocalDate getLastGracePeriodDate() {
        return statistics.gracePeriodStart.plusDays(30);
    }

    private void fireLicenseEvent(LicenseEventType type) {
        if (statistics.licenseEvents.get(type) != null) {
            // Event already fired, do nothing.
            return;
        }
        String message;
        switch (type) {
        case GRACE_PERIOD_STARTED:
            LocalDate gracePeriodEnd = getLastGracePeriodDate().plusDays(1);
            message = type.createMessage(gracePeriodEnd.format(DATE_FORMATTER));
            break;
        case LICENSE_EXPIRES_SOON:
            message = type
                    .createMessage(license.endDate.format(DATE_FORMATTER));
            break;
        default:
            message = type.createMessage();
        }
        LicenseEvent event = new LicenseEvent(ce, type, message);
        statistics.licenseEvents.put(type, getCurrentDate());
        fileHandler.writeStats(statistics);
        ce.getConfiguration().getLicenseEventHandler()
                .handleLicenseEvent(event);
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
