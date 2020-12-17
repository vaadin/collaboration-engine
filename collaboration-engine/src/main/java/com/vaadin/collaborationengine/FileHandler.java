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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.collaborationengine.LicenseHandler.LicenseInfo;
import com.vaadin.collaborationengine.LicenseHandler.LicenseInfoWrapper;
import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfo;
import com.vaadin.collaborationengine.LicenseHandler.StatisticsInfoWrapper;
import com.vaadin.flow.internal.MessageDigestUtil;

class FileHandler {

    /**
     * When querying properties from Vaadin's
     * {@link com.vaadin.flow.function.DeploymentConfiguration}, they are looked
     * within the `vaadin.` namespace. When querying, we should therefore not
     * include the prefix. However, when instructing people on how to set the
     * parameter, we should include the prefix.
     */
    static final String DATA_DIR_CONFIG_PROPERTY = "ce.dataDir";
    static final String DATA_DIR_PUBLIC_PROPERTY = "vaadin."
            + DATA_DIR_CONFIG_PROPERTY;

    private final ObjectMapper objectMapper;

    private final Path statsFilePath;
    private final Path licenseFilePath;

    FileHandler(CollaborationEngineConfiguration config) {
        objectMapper = createObjectMapper();
        if (config.getDataDirPath() == null) {
            throw createDataDirNotConfiguredException();
        }
        if (config.getDataDirPath().toFile().exists()
                && !Files.isWritable(config.getDataDirPath())) {
            throw createDataDirNotWritableException(config.getDataDirPath());
        }

        statsFilePath = createStatsFilePath(config.getDataDirPath());
        licenseFilePath = createLicenseFilePath(config.getDataDirPath());
        if (statsFilePath.toFile().exists()
                && !Files.isWritable(statsFilePath)) {
            throw createStatsFileNotWritableException();
        }
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.FIELD,
                Visibility.NON_PRIVATE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        return objectMapper;
    }

    static Path createStatsFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-statistics.json");
    }

    static Path createLicenseFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-license.json");
    }

    void writeStats(StatisticsInfo stats) {
        try {
            String checksum = calculateChecksum(
                    objectMapper.valueToTree(stats));
            StatisticsInfoWrapper wrapper = new StatisticsInfoWrapper(stats,
                    checksum);
            objectMapper.writeValue(statsFilePath.toFile(), wrapper);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write statistics into file at '"
                            + statsFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    LicenseInfo readLicenseFile() {
        try {
            JsonNode licenseJson = readFileAsJson(licenseFilePath)
                    .orElseThrow(this::createLicenseNotFoundException);

            LicenseInfoWrapper licenseInfoWrapper = objectMapper
                    .treeToValue(licenseJson, LicenseInfoWrapper.class);

            String calculatedChecksum = calculateChecksum(
                    licenseJson.get("content"));

            if (licenseInfoWrapper.checksum == null
                    || !licenseInfoWrapper.checksum
                            .equals(calculatedChecksum)) {
                throw createLicenseInvalidException(null);
            }

            return licenseInfoWrapper.content;

        } catch (JsonProcessingException e) {
            throw createLicenseInvalidException(e);
        }
    }

    StatisticsInfo readStatsFile() {
        try {
            Optional<JsonNode> statsJson = readFileAsJson(statsFilePath);
            if (statsJson.isPresent()) {
                JsonNode statisticsJson = statsJson.get();

                StatisticsInfoWrapper statisticsInfoWrapper = objectMapper
                        .treeToValue(statisticsJson,
                                StatisticsInfoWrapper.class);

                String calculatedChecksum = calculateChecksum(
                        statisticsJson.get("content"));

                if (statisticsInfoWrapper.checksum == null
                        || !statisticsInfoWrapper.checksum
                                .equals(calculatedChecksum)) {
                    throw createStatsInvalidException();
                }

                return statisticsInfoWrapper.content;
            } else {
                return new StatisticsInfo(null, Collections.emptyMap(), null,
                        Collections.emptyMap());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Collaboration Engine failed to parse the statistics information from file '"
                            + statsFilePath + "'.",
                    e);
        }
    }

    private String calculateChecksum(JsonNode node)
            throws JsonProcessingException {
        return Base64.getEncoder().encodeToString(MessageDigestUtil
                .sha256(objectMapper.writeValueAsString(node)));
    }

    private Optional<JsonNode> readFileAsJson(Path filePath)
            throws JsonProcessingException {
        try {
            File file = filePath.toFile();
            if (!file.exists()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(file));
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to read the file at '"
                            + filePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    private RuntimeException createDataDirNotConfiguredException() {
        return new IllegalStateException(
                "Missing required configuration property '"
                        + DATA_DIR_PUBLIC_PROPERTY
                        + "'. Using Collaboration Engine in production requires having a valid license file "
                        + "and configuring the directory where that file is stored e.g. as a system property. "
                        + "Instructions can be found in the Vaadin documentation.");
    }

    private RuntimeException createLicenseNotFoundException() {
        return new IllegalStateException(
                "Collaboration Engine failed to find the license file at '"
                        + licenseFilePath
                        + ". Using Collaboration Engine in production requires a valid license file. "
                        + "Instructions for obtaining a license can be found in the Vaadin documentation. "
                        + "If you already have a license, make sure that the '"
                        + DATA_DIR_PUBLIC_PROPERTY
                        + "' property is pointing to the correct directory "
                        + "and that the directory contains the license file.");
    }

    private RuntimeException createLicenseInvalidException(Throwable cause) {
        return new IllegalStateException(
                "Collaboration Engine failed to parse the file '"
                        + licenseFilePath
                        + "'. The content of the license file is not valid. "
                        + "If you have made any changes to the file, please revert those changes. "
                        + "If that's not possible, contact Vaadin to get a new copy of the license file.",
                cause);
    }

    private RuntimeException createDataDirNotWritableException(
            Path dataDirFilePath) {
        return new IllegalStateException("Collaboration Engine doesn't have "
                + "write permissions for the data directory at '"
                + dataDirFilePath
                + "'. Collaboration Engine needs to be able to write files "
                + "into the folder to function. Make sure that the the system "
                + "user, running the Java environment, has write permissions "
                + "to the directory.");
    }

    private RuntimeException createStatsFileNotWritableException() {
        return new IllegalStateException("Collaboration Engine doesn't have "
                + "write permissions for the statistics file at '"
                + statsFilePath
                + "'. Collaboration Engine needs to be able to write into the "
                + "file to function. Make sure that the the system user, "
                + "running the Java environment, has write permissions to the "
                + "file.");
    }

    private RuntimeException createStatsInvalidException() {
        return new IllegalStateException(
                "Collaboration Engine failed to parse the file '"
                        + statsFilePath
                        + "'. The content of the statistics file is not valid. "
                        + "If you have made any changes to the file, please revert those changes. "
                        + "If that's not possible, contact Vaadin to get support.");
    }
}
