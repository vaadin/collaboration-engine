/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

class Statistics {
    private Map<YearMonth, Set<String>> userIdsPerMonth = new ConcurrentSkipListMap<>();

    void registerUser(String userId) {
        Set<String> users = userIdsPerMonth.computeIfAbsent(getCurrentMonth(),
                yearMonth -> Collections
                        .synchronizedSet(new LinkedHashSet<>()));
        if (users.add(userId)) {
            // Will be used to write into file
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
        Map<YearMonth, Set<String>> copy = new ConcurrentSkipListMap<>();
        for (Map.Entry<YearMonth, Set<String>> month : userIdsPerMonth
                .entrySet()) {
            copy.put(month.getKey(), new LinkedHashSet<>(month.getValue()));
        }
        return copy;
    }
}
