/*
 * Copyright (C) 2021 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * The interface to store license usage data.
 *
 * @author Vaadin Ltd
 */
public interface LicenseStorage {

    /**
     * Gets a list of entries for users seen in the specified month.
     *
     * @param licenseKey
     *            the license key
     * @param month
     *            the month
     * @return the list of users
     */
    List<String> getUserEntries(String licenseKey, YearMonth month);

    /**
     * Adds a user entry for the specified month.
     *
     * @param licenseKey
     *            the license key
     * @param month
     *            the month
     * @param payload
     *            the user entry
     */
    void addUserEntry(String licenseKey, YearMonth month, String payload);

    /**
     * Gets a map of license event names with the date of their last occurrence.
     *
     * @param licenseKey
     *            the license key
     * @return the mapping of events and their last occurrence
     */
    Map<String, LocalDate> getLatestLicenseEvents(String licenseKey);

    /**
     * Sets the date of the latest occurrence of the specified license event.
     *
     * @param licenseKey
     *            the license key
     * @param eventName
     *            the event name
     * @param latestOccurrence
     *            the date of the latest occurrence
     */
    void setLicenseEvent(String licenseKey, String eventName,
            LocalDate latestOccurrence);
}
