/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
 * @deprecated license storage is not needed since 6.3
 */
@Deprecated(since = "6.3", forRemoval = true)
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
