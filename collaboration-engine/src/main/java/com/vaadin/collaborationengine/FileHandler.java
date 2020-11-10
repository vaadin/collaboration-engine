/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.collaborationengine.LicenseHandler.LicenseInfo;
import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfo;

class FileHandler {

    static final Path DEFAULT_DATA_DIR = Paths.get(
            System.getProperty("user.home"), ".vaadin", "collaboration-engine");

    static Path dataDirPath = DEFAULT_DATA_DIR;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Path statsFilePath;
    private final Path licenseFilePath;

    FileHandler() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.FIELD,
                Visibility.NON_PRIVATE);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        objectMapper.setDateFormat(df);

        statsFilePath = createStatsFilePath(dataDirPath);
        licenseFilePath = createLicenseFilePath(dataDirPath);
    }

    static Path createStatsFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-statistics.json");
    }

    static Path createLicenseFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-license.json");
    }

    void writeStats(StatisticsInfo stats) {
        try {
            objectMapper.writeValue(statsFilePath.toFile(), stats);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write statistics into file at '"
                            + statsFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    LicenseInfo readLicenseFile() {
        JsonNode licenseJson = readFileAsJson(licenseFilePath)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to read the license file at '" + licenseFilePath
                                + "'."));
        try {
            return objectMapper.treeToValue(licenseJson.get("content"),
                    LicenseInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse the license information from file '"
                            + licenseFilePath + "'.",
                    e);
        }
    }

    StatisticsInfo readStatsFile() {
        return readFileAsJson(statsFilePath).map(statsJson -> {
            try {
                return objectMapper.treeToValue(statsJson,
                        StatisticsInfo.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Failed to parse the license information from file '"
                                + licenseFilePath + "'.",
                        e);
            }
        }).orElseGet(() -> new StatisticsInfo(Collections.emptyMap(), null));
    }

    private Optional<JsonNode> readFileAsJson(Path filePath) {
        try {
            File file = filePath.toFile();
            if (!file.exists()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(file));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to read the file at '"
                            + filePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }
}
