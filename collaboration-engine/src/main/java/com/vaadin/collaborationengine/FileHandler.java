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
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class FileHandler {

    static final Path DEFAULT_DATA_DIR = Paths.get(
            System.getProperty("user.home"), ".vaadin", "collaboration-engine");

    private static final String STATISTICS_JSON_KEY = "statistics";

    private static Path dataDirPath;
    private static Path statsFilePath;

    static {
        setDataDirectory(DEFAULT_DATA_DIR);
    }

    private FileHandler() {
    }

    static void setDataDirectory(Path dataDirectory) {
        dataDirPath = dataDirectory;
        statsFilePath = Paths.get(dataDirPath.toString(), "ce-statistics.json");
    }

    static Path getDataDirPath() {
        return dataDirPath;
    }

    static Path getStatsFilePath() {
        return statsFilePath;
    }

    static Map<YearMonth, List<String>> readStats() {
        try {
            if (!statsFilePath.toFile().exists()) {
                return Collections.emptyMap();
            }

            byte[] fileContent = Files.readAllBytes(statsFilePath);
            if (fileContent.length == 0) {
                return Collections.emptyMap();
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            JsonNode json = mapper.readTree(Files.readAllBytes(statsFilePath));
            JsonNode statisticsJson = json.get(STATISTICS_JSON_KEY);
            if (statisticsJson == null) {
                return Collections.emptyMap();
            }
            return mapper.convertValue(statisticsJson,
                    new TypeReference<Map<YearMonth, List<String>>>() {
                    });
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to read the statistics file at '"
                            + statsFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    static void writeStats(Map<YearMonth, Set<String>> userIdsPerMonth) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode json = mapper.createObjectNode();
        json.set(STATISTICS_JSON_KEY, mapper.valueToTree(userIdsPerMonth));
        try {
            if (!dataDirPath.toFile().exists()) {
                Files.createDirectories(dataDirPath);
            }
            mapper.writeValue(statsFilePath.toFile(), json);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to write to the statistics file at '"
                            + statsFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }
}
