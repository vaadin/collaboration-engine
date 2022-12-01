/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfo;
import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfoWrapper;

class FileLicenseStorage implements LicenseStorage {

    private final Path statsFilePath;

    StatisticsInfo statisticsCache;

    public FileLicenseStorage(CollaborationEngineConfiguration configuration) {
        Path dataDirPath = configuration.getDataDirPath();
        if (dataDirPath == null) {
            throw createDataDirNotConfiguredException();
        }
        if (dataDirPath.toFile().exists() && !Files.isWritable(dataDirPath)) {
            throw createDataDirNotWritableException(dataDirPath);
        }
        statsFilePath = createStatsFilePath(dataDirPath);
        statisticsCache = readStatistics();
    }

    @Override
    public List<String> getUserEntries(String licenseKey, YearMonth month) {
        checkLicenseKey(licenseKey);
        return statisticsCache.getUserEntries(month);
    }

    @Override
    public void addUserEntry(String licenseKey, YearMonth month,
            String payload) {
        checkLicenseKey(licenseKey);
        statisticsCache.addUserEntry(month, payload);
        writeStatistics();
    }

    @Override
    public Map<String, LocalDate> getLatestLicenseEvents(String licenseKey) {
        checkLicenseKey(licenseKey);
        return statisticsCache.getLatestLicenseEvents();
    }

    @Override
    public void setLicenseEvent(String licenseKey, String eventName,
            LocalDate latestOccurrence) {
        checkLicenseKey(licenseKey);
        statisticsCache.setLicenseEvent(eventName, latestOccurrence);
        writeStatistics();
    }

    private StatisticsInfo readStatistics() {
        try {
            Optional<JsonNode> statsJson = readFileAsJson(statsFilePath);
            if (statsJson.isPresent()) {
                JsonNode statisticsJson = statsJson.get();

                StatisticsInfoWrapper statisticsInfoWrapper = LicenseHandler.MAPPER
                        .treeToValue(statisticsJson,
                                StatisticsInfoWrapper.class);

                String calculatedChecksum = LicenseHandler
                        .calculateChecksum(statisticsJson.get("content"));

                if (statisticsInfoWrapper.checksum == null
                        || !statisticsInfoWrapper.checksum
                                .equals(calculatedChecksum)) {
                    throw createStatsInvalidException(statsFilePath);
                }

                return statisticsInfoWrapper.content;
            } else {
                return new StatisticsInfo(null, Collections.emptyMap(),
                        Collections.emptyMap());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Collaboration Engine failed to parse the statistics information from file '"
                            + statsFilePath + "'.",
                    e);
        }
    }

    private void writeStatistics() {
        try {
            String checksum = LicenseHandler.calculateChecksum(
                    LicenseHandler.MAPPER.valueToTree(statisticsCache));
            StatisticsInfoWrapper wrapper = new StatisticsInfoWrapper(
                    statisticsCache, checksum);
            LicenseHandler.MAPPER.writeValue(statsFilePath.toFile(), wrapper);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write statistics into file at '"
                            + statsFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    private void checkLicenseKey(String licenseKey) {
        if (!licenseKey.equals(statisticsCache.licenseKey)) {
            statisticsCache.licenseKey = licenseKey;
            statisticsCache.licenseEvents.clear();
        }
    }

    private Optional<JsonNode> readFileAsJson(Path filePath)
            throws JsonProcessingException {
        try {
            File file = filePath.toFile();
            if (!file.exists()) {
                return Optional.empty();
            }
            return Optional.of(LicenseHandler.MAPPER.readTree(file));
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw createFileNotReadableException(filePath, e);
        }
    }

    static RuntimeException createFileNotReadableException(Path filePath,
            Throwable cause) {
        return new IllegalStateException(
                "Collaboration Engine wasn't able to read the file at '"
                        + filePath
                        + "'. Check that the file is readable by the app, and not locked.",
                cause);
    }

    static RuntimeException createDataDirNotConfiguredException() {
        return new IllegalStateException(
                "Missing required configuration property '"
                        + CollaborationEngineConfiguration.DATA_DIR_PUBLIC_PROPERTY
                        + "'. Using Collaboration Engine in production requires having a valid license file "
                        + "and configuring the directory where that file is stored e.g. as a system property. "
                        + "Instructions can be found in the Vaadin documentation.");
    }

    static RuntimeException createDataDirNotWritableException(
            Path dataDirFilePath) {
        return new IllegalStateException("Collaboration Engine doesn't have "
                + "write permissions for the data directory at '"
                + dataDirFilePath
                + "'. Collaboration Engine needs to be able to write files "
                + "into the folder to function. Make sure that the the system "
                + "user, running the Java environment, has write permissions "
                + "to the directory.");
    }

    static RuntimeException createStatsInvalidException(Path statsFilePath) {
        return new IllegalStateException(
                "Collaboration Engine failed to parse the file '"
                        + statsFilePath
                        + "'. The content of the statistics file is not valid. "
                        + "If you have made any changes to the file, please revert those changes. "
                        + "If that's not possible, contact Vaadin to get support.");
    }

    static Path createStatsFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-statistics.json");
    }
}
