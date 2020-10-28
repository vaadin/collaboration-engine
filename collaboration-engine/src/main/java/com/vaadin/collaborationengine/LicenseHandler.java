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
import com.fasterxml.jackson.annotation.JsonProperty;

class LicenseHandler {

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

    private final FileHandler fileHandler = new FileHandler();
    private final LicenseInfo license;

    private Map<YearMonth, Set<String>> userIdsPerMonth;

    LicenseHandler() {
        license = fileHandler.readLicenseFile();
        Map<YearMonth, List<String>> userIdsFromFile = fileHandler.readStats();
        userIdsPerMonth = copyMap(userIdsFromFile);
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
        Set<String> users = userIdsPerMonth.computeIfAbsent(getCurrentMonth(),
                yearMonth -> new LinkedHashSet<>());
        if (users.contains(userId)) {
            return true;
        } else if (users.size() >= license.quota) {
            return false;
        } else {
            users.add(userId);
            fileHandler.writeStats(userIdsPerMonth);
            return true;
        }
    }

    /*
     * For overriding for testing purposes
     */
    YearMonth getCurrentMonth() {
        return YearMonth.now();
    }

    /*
     * For testing internal state of Statistics gathering
     */
    Map<YearMonth, Set<String>> getStatistics() {
        return copyMap(this.userIdsPerMonth);
    }

    private Map<YearMonth, Set<String>> copyMap(
            Map<YearMonth, ? extends Collection<String>> map) {
        TreeMap<YearMonth, Set<String>> treeMap = new TreeMap<>();
        for (Map.Entry<YearMonth, ? extends Collection<String>> month : map
                .entrySet()) {
            treeMap.put(month.getKey(), new LinkedHashSet<>(month.getValue()));
        }
        return treeMap;
    }
}
