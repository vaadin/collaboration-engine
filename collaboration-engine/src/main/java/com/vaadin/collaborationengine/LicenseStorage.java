/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.Reader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

interface LicenseStorage {

    Reader getLicense();

    List<String> getUserEntries(String licenseKey, YearMonth month);

    void addUserEntry(String licenseKey, YearMonth month, String payload);

    Map<String, LocalDate> getLatestLicenseEvents(String licenseKey);

    void setLicenseEvent(String licenseKey, String eventName,
            LocalDate latestOccurrence);
}
