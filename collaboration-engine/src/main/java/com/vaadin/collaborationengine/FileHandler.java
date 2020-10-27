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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.collaborationengine.LicenseHandler.LicenseInfo;

class FileHandler {

    static final Path DEFAULT_DATA_DIR = Paths.get(
            System.getProperty("user.home"), ".vaadin", "collaboration-engine");

    static Path dataDirPath = DEFAULT_DATA_DIR;

    private static final String STATISTICS_JSON_KEY = "statistics";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Path statsFilePath;
    private final Path licenseFilePath;

    FileHandler() {
        objectMapper.registerModule(new JavaTimeModule());
        statsFilePath = createStatsFilePath(dataDirPath);
        licenseFilePath = createLicenseFilePath(dataDirPath);
    }

    static Path createStatsFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-statistics.json");
    }

    static Path createLicenseFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-license.json");
    }

    Map<YearMonth, List<String>> readStats() {
        JsonNode json = readFileAsJson(statsFilePath)
                .orElse(objectMapper.createObjectNode());
        JsonNode statisticsJson = json.get(STATISTICS_JSON_KEY);
        if (statisticsJson == null) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(statisticsJson,
                new TypeReference<Map<YearMonth, List<String>>>() {
                });
    }

    void writeStats(Map<YearMonth, Set<String>> userIdsPerMonth) {
        ObjectNode json = objectMapper.createObjectNode();
        json.set(STATISTICS_JSON_KEY,
                objectMapper.valueToTree(userIdsPerMonth));
        writeJsonToFile(json, statsFilePath);
    }

    LicenseInfo readLicenseFile() {
        JsonNode licenseJson = readFileAsJson(licenseFilePath)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to read the license file at '" + licenseFilePath
                                + "'."));
        try {
            return objectMapper.treeToValue(licenseJson, LicenseInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse the license information from file '"
                            + licenseFilePath + "'.",
                    e);
        }
    }

    private Optional<JsonNode> readFileAsJson(Path filePath) {
        try {
            if (!filePath.toFile().exists()) {
                return Optional.empty();
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            if (fileContent.length == 0) {
                return Optional.empty();
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            return Optional.of(mapper.readTree(Files.readAllBytes(filePath)));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to read the file at '"
                            + filePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    private void writeJsonToFile(JsonNode json, Path filePath) {
        try {
            objectMapper.writeValue(filePath.toFile(), json);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write to the file at '"
                            + filePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }
}
