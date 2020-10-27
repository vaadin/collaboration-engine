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

    private static final String STATISTICS_JSON_KEY = "statistics";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Path dataDirPath;
    private static Path statsFilePath;
    private static Path licenseFilePath;

    static {
        setDataDirectory(DEFAULT_DATA_DIR);
        MAPPER.registerModule(new JavaTimeModule());
    }

    private FileHandler() {
    }

    static void setDataDirectory(Path dataDirectory) {
        dataDirPath = dataDirectory;
        statsFilePath = Paths.get(dataDirPath.toString(), "ce-statistics.json");
        licenseFilePath = Paths.get(dataDirPath.toString(), "ce-license.json");
    }

    static Path getDataDirPath() {
        return dataDirPath;
    }

    static Path getStatsFilePath() {
        return statsFilePath;
    }

    static Path getLicenseFilePath() {
        return licenseFilePath;
    }

    static Map<YearMonth, List<String>> readStats() {
        JsonNode json = readFileAsJson(statsFilePath)
                .orElse(MAPPER.createObjectNode());
        JsonNode statisticsJson = json.get(STATISTICS_JSON_KEY);
        if (statisticsJson == null) {
            return Collections.emptyMap();
        }
        return MAPPER.convertValue(statisticsJson,
                new TypeReference<Map<YearMonth, List<String>>>() {
                });
    }

    static void writeStats(Map<YearMonth, Set<String>> userIdsPerMonth) {
        ObjectNode json = MAPPER.createObjectNode();
        json.set(STATISTICS_JSON_KEY, MAPPER.valueToTree(userIdsPerMonth));
        writeJsonToFile(json, statsFilePath);
    }

    static LicenseInfo readLicenseFile() {
        JsonNode licenseJson = readFileAsJson(licenseFilePath)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to read the license file at '" + licenseFilePath
                                + "'."));
        try {
            return MAPPER.treeToValue(licenseJson, LicenseInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse the license information from file '"
                            + licenseFilePath + "'.",
                    e);
        }
    }

    private static Optional<JsonNode> readFileAsJson(Path filePath) {
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

    private static void writeJsonToFile(JsonNode json, Path filePath) {
        try {
            MAPPER.writeValue(filePath.toFile(), json);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write to the file at '"
                            + filePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }
}
