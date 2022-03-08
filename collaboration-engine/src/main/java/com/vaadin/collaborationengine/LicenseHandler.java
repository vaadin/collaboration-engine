/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.collaborationengine.Backend.EventLog;
import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.flow.internal.MessageDigestUtil;

/**
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
class LicenseHandler {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LicenseInfo {
        final String key;
        final int quota;
        final LocalDate endDate;

        @JsonCreator
        LicenseInfo(@JsonProperty(value = "key", required = true) String key,
                @JsonProperty(value = "quota", required = true) int quota,
                @JsonProperty(value = "endDate", required = true) LocalDate endDate) {
            this.key = key;
            this.quota = quota;
            this.endDate = endDate;
        }
    }

    static class LicenseInfoWrapper {

        final LicenseInfo content;

        final String checksum;

        @JsonCreator
        LicenseInfoWrapper(
                @JsonProperty(value = "content", required = true) LicenseInfo content,
                @JsonProperty(value = "checksum", required = true) String checksum) {
            this.content = content;
            this.checksum = checksum;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StatisticsInfo {
        String licenseKey;
        Map<YearMonth, Set<String>> statistics;
        Map<LicenseEventType, LocalDate> licenseEvents;

        StatisticsInfo(
                @JsonProperty(value = "licenseKey", required = true) String licenseKey,
                @JsonProperty(value = "statistics", required = true) Map<YearMonth, List<String>> userIdsFromFile,
                @JsonProperty(value = "licenseEvents", required = true) Map<LicenseEventType, LocalDate> licenseEvents) {
            this.licenseKey = licenseKey;
            this.statistics = copyMap(userIdsFromFile);
            this.licenseEvents = new HashMap<>(licenseEvents);
        }

        Map<YearMonth, Set<String>> copyMap(
                Map<YearMonth, ? extends Collection<String>> map) {
            TreeMap<YearMonth, Set<String>> treeMap = new TreeMap<>();
            for (Map.Entry<YearMonth, ? extends Collection<String>> month : map
                    .entrySet()) {
                treeMap.put(month.getKey(),
                        new LinkedHashSet<>(month.getValue()));
            }
            return treeMap;
        }

        List<String> getUserEntries(YearMonth month) {
            Set<String> entries = statistics.getOrDefault(month,
                    Collections.emptySet());
            return new ArrayList<>(entries);
        }

        void addUserEntry(YearMonth month, String payload) {
            statistics.computeIfAbsent(month, key -> new LinkedHashSet<>())
                    .add(payload);
        }

        Map<String, LocalDate> getLatestLicenseEvents() {
            return licenseEvents.entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().name(), Map.Entry::getValue));
        }

        void setLicenseEvent(String eventName, LocalDate latestOccurrence) {
            licenseEvents.put(LicenseEventType.valueOf(eventName),
                    latestOccurrence);
        }
    }

    static class StatisticsInfoWrapper {

        final StatisticsInfo content;

        final String checksum;

        @JsonCreator
        StatisticsInfoWrapper(
                @JsonProperty(value = "content", required = true) StatisticsInfo content,
                @JsonProperty(value = "checksum", required = true) String checksum) {
            this.content = content;
            this.checksum = checksum;
        }
    }

    static class Snapshot {

        private final UUID latestChange;

        private final StatisticsInfo statistics;

        @JsonCreator
        public Snapshot(
                @JsonProperty(value = "latestChange", required = true) UUID latestChange,
                @JsonProperty(value = "statistics", required = true) StatisticsInfo statistics) {
            this.latestChange = latestChange;
            this.statistics = statistics;
        }

        public UUID getLatestChange() {
            return latestChange;
        }

        public StatisticsInfo getStatistics() {
            return statistics;
        }
    }

    private static final String EVENT_LOG_NAME = LicenseHandler.class.getName();
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    static final ObjectMapper MAPPER = createObjectMapper();

    private final CollaborationEngine ce;
    private final CollaborationEngineConfiguration configuration;
    private final Backend backend;
    final LicenseStorage licenseStorage;
    final LicenseInfo license;
    final EventLog licenseEventLog;
    private StatisticsInfo statisticsCache;
    private final List<UUID> backendNodes = new ArrayList<>();
    private boolean leader;

    LicenseHandler(CollaborationEngine collaborationEngine) {
        this.ce = collaborationEngine;
        this.configuration = collaborationEngine.getConfiguration();
        this.backend = configuration.getBackend();
        if (configuration.isLicenseCheckingEnabled()) {
            LicenseStorage configuredStorage = configuration
                    .getLicenseStorage();
            licenseStorage = configuredStorage != null ? configuredStorage
                    : new FileLicenseStorage(configuration);
            String licenseProperty = configuration.getLicenseProperty();
            if (licenseProperty != null) {
                license = parseLicense(getLicenseFromProperty(licenseProperty));
            } else {
                Path dataDirPath = configuration.getDataDirPath();
                if (dataDirPath == null) {
                    throw FileLicenseStorage
                            .createDataDirNotConfiguredException();
                }
                Path licenseFilePath = createLicenseFilePath(dataDirPath);
                license = parseLicense(getLicenseFromFile(licenseFilePath));
            }
            if (license.endDate.isBefore(getCurrentDate())) {
                CollaborationEngine.LOGGER
                        .warn("Your Collaboration Engine license has expired. "
                                + "Your application will still continue to "
                                + "work, but the collaborative features will be "
                                + "disabled. Please contact Vaadin about "
                                + "obtaining a new, up-to-date license for "
                                + "your application. "
                                + "https://vaadin.com/collaboration");
            }
            licenseEventLog = backend.openEventLog(EVENT_LOG_NAME);
            backend.getMembershipEventLog().subscribe(null,
                    (eventId, event) -> {
                        String type = event.get(JsonUtil.CHANGE_TYPE).asText();
                        if (JsonUtil.CHANGE_NODE_JOIN.equals(type)) {
                            handleNodeJoin(event);
                        } else if (JsonUtil.CHANGE_NODE_LEAVE.equals(type)) {
                            handleNodeLeave(event);
                        }
                    });
            backend.loadLatestSnapshot(EVENT_LOG_NAME)
                    .thenAccept(this::initializeFromSnapshot);
        } else {
            licenseEventLog = null;
            licenseStorage = null;
            license = null;
        }
    }

    boolean isLeader() {
        return leader;
    }

    private void becomeLeader() {
        leader = true;
    }

    private void handleNodeJoin(ObjectNode event) {
        UUID nodeId = UUID
                .fromString(event.get(JsonUtil.CHANGE_NODE_ID).asText());
        if (backendNodes.isEmpty() && backend.getNodeId().equals(nodeId)) {
            becomeLeader();
        }
        backendNodes.add(nodeId);
    }

    private void handleNodeLeave(ObjectNode event) {
        UUID nodeId = UUID
                .fromString(event.get(JsonUtil.CHANGE_NODE_ID).asText());
        backendNodes.remove(nodeId);
        if (!backendNodes.isEmpty()
                && backendNodes.get(0).equals(backend.getNodeId())) {
            becomeLeader();
        }
    }

    private void initializeFromSnapshot(ObjectNode snapshot) {
        if (snapshot != null) {
            UUID latestChange = loadSnapshot(snapshot).getLatestChange();
            licenseEventLog.subscribe(latestChange, this::handleChangeEvent);
        } else {
            loadFromStorage();
            licenseEventLog.subscribe(null, this::handleChangeEvent);
        }
    }

    private Snapshot loadSnapshot(ObjectNode node) {
        try {
            Snapshot snapshot = MAPPER.treeToValue(node, Snapshot.class);
            statisticsCache = snapshot.getStatistics();
            return snapshot;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Collaboration Engine failed to load license usage data.",
                    e);
        }
    }

    private void loadFromStorage() {
        statisticsCache = new StatisticsInfo(license.key,
                Collections.emptyMap(), Collections.emptyMap());

        YearMonth month = YearMonth.from(getCurrentDate());
        licenseStorage.getUserEntries(license.key, month)
                .forEach(userId -> statisticsCache.addUserEntry(month, userId));
        licenseStorage.getLatestLicenseEvents(license.key)
                .forEach(statisticsCache::setLicenseEvent);

        Snapshot snapshot = new Snapshot(UUID.randomUUID(), statisticsCache);
        backend.submitSnapshot(EVENT_LOG_NAME, MAPPER.valueToTree(snapshot));
    }

    private void handleChangeEvent(UUID eventId, ObjectNode event) {
        String changeType = event.get(JsonUtil.CHANGE_TYPE).asText();
        String licenseKey = event.get(JsonUtil.CHANGE_LICENSE_KEY).asText();
        if (JsonUtil.CHANGE_TYPE_LICENSE_USER.equals(changeType)) {
            YearMonth month = YearMonth
                    .parse(event.get(JsonUtil.CHANGE_YEAR_MONTH).asText());
            String userId = event.get(JsonUtil.CHANGE_USER_ID).asText();
            statisticsCache.addUserEntry(month, userId);
            if (leader) {
                licenseStorage.addUserEntry(licenseKey, month, userId);
            }
        } else if (JsonUtil.CHANGE_TYPE_LICENSE_EVENT.equals(changeType)) {
            String eventName = event.get(JsonUtil.CHANGE_EVENT_NAME).asText();
            LocalDate latestOccurrence = LocalDate.parse(
                    event.get(JsonUtil.CHANGE_EVENT_OCCURRENCE).asText());
            statisticsCache.setLicenseEvent(eventName, latestOccurrence);
            if (leader) {
                notifyLicenseEventHandler(eventName);
                licenseStorage.setLicenseEvent(licenseKey, eventName,
                        latestOccurrence);
            }
        }
    }

    private void notifyLicenseEventHandler(String eventName) {
        LicenseEventType type = LicenseEventType.valueOf(eventName);
        String message;
        switch (type) {
        case GRACE_PERIOD_STARTED:
            LocalDate gracePeriodEnd = getCurrentDate().plusDays(31);
            message = type.createMessage(gracePeriodEnd.format(DATE_FORMATTER));
            break;
        case LICENSE_EXPIRES_SOON:
            message = type
                    .createMessage(license.endDate.format(DATE_FORMATTER));
            break;
        default:
            message = type.createMessage();
        }
        configuration.getLicenseEventHandler()
                .handleLicenseEvent(new LicenseEvent(ce, type, message));
    }

    private Reader getLicenseFromProperty(String licenseProperty) {
        byte[] license = Base64.getDecoder().decode(licenseProperty);
        return new InputStreamReader(new ByteArrayInputStream(license));
    }

    private Reader getLicenseFromFile(Path licenseFilePath) {
        try {
            return Files.newBufferedReader(licenseFilePath);
        } catch (NoSuchFileException e) {
            throw createLicenseNotFoundException(licenseFilePath, e);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Collaboration Engine wasn't able to read the license file at '"
                            + licenseFilePath
                            + "'. Check that the file is readable by the app, and not locked.",
                    e);
        }
    }

    private LicenseInfo parseLicense(Reader licenseReader) {
        try {
            JsonNode licenseJson = MAPPER.readTree(licenseReader);

            LicenseInfoWrapper licenseInfoWrapper = MAPPER
                    .treeToValue(licenseJson, LicenseInfoWrapper.class);

            String calculatedChecksum = calculateChecksum(
                    licenseJson.get("content"));

            if (licenseInfoWrapper.checksum == null
                    || !licenseInfoWrapper.checksum
                            .equals(calculatedChecksum)) {
                throw createLicenseInvalidException(null);
            }

            return licenseInfoWrapper.content;

        } catch (IOException e) {
            throw createLicenseInvalidException(e);
        }
    }

    /**
     * Tries to register a seat for the user with the given id for the current
     * calendar month. Returns whether the user has a seat or not. If license
     * checking/statistics is not enabled, just returns {@code true}.
     *
     * @param userId
     *            the user id to register
     * @return {@code true} if the user can use Collaboration Engine,
     *         {@code false} otherwise
     */
    synchronized boolean registerUser(String userId) {
        LocalDate currentDate = getCurrentDate();

        if (isGracePeriodEnded(currentDate)) {
            fireLicenseEvent(LicenseEventType.GRACE_PERIOD_ENDED);
        }
        if (license.endDate.isBefore(currentDate)) {
            fireLicenseEvent(LicenseEventType.LICENSE_EXPIRED);
            return false;
        }
        if (license.endDate.minusDays(31).isBefore(currentDate)) {
            fireLicenseEvent(LicenseEventType.LICENSE_EXPIRES_SOON);
        }

        YearMonth month = YearMonth.from(currentDate);
        List<String> users = statisticsCache.getUserEntries(month);

        int effectiveQuota = isGracePeriodOngoing(currentDate)
                ? license.quota * 10
                : license.quota;

        boolean hasActiveSeat = users.size() <= effectiveQuota
                ? users.contains(userId)
                : users.stream().limit(effectiveQuota)
                        .anyMatch(user -> user.equals(userId));

        if (hasActiveSeat) {
            return true;
        }

        if (users.size() >= effectiveQuota) {
            if (getGracePeriodStarted() != null) {
                return false;
            }
            fireLicenseEvent(LicenseEventType.GRACE_PERIOD_STARTED);
        }

        ObjectNode entry = JsonUtil.createUserEntry(license.key, month, userId);
        licenseEventLog.submitEvent(UUID.randomUUID(), entry);
        return true;
    }

    LocalDate getGracePeriodStarted() {
        return statisticsCache.getLatestLicenseEvents()
                .get(LicenseEventType.GRACE_PERIOD_STARTED.name());
    }

    private boolean isGracePeriodOngoing(LocalDate currentDate) {
        return getGracePeriodStarted() != null
                && !isGracePeriodEnded(currentDate);
    }

    private boolean isGracePeriodEnded(LocalDate currentDate) {
        return getGracePeriodStarted() != null
                && currentDate.isAfter(getLastGracePeriodDate());
    }

    private LocalDate getLastGracePeriodDate() {
        return getGracePeriodStarted().plusDays(30);
    }

    private void fireLicenseEvent(LicenseEventType type) {
        String eventName = type.name();
        if (statisticsCache.getLatestLicenseEvents().get(eventName) != null) {
            // Event already fired, do nothing.
            return;
        }
        ObjectNode event = JsonUtil.createLicenseEvent(license.key, eventName,
                getCurrentDate());
        licenseEventLog.submitEvent(UUID.randomUUID(), event);
    }

    private LocalDate getCurrentDate() {
        return LocalDate.now(ce.getClock());
    }

    private RuntimeException createLicenseInvalidException(Throwable cause) {
        return new IllegalStateException(
                "The content of the license property or file is not valid. "
                        + "If you have made any changes to the file, please revert those changes. "
                        + "If that's not possible, contact Vaadin to get a new copy of the license file.",
                cause);
    }

    private RuntimeException createLicenseNotFoundException(
            Path licenseFilePath, Throwable cause) {
        return new IllegalStateException(
                "Collaboration Engine failed to find the license file at '"
                        + licenseFilePath
                        + ". Using Collaboration Engine in production requires a valid license property or file. "
                        + "Instructions for obtaining a license can be found in the Vaadin documentation. "
                        + "If you already have a license, make sure that the '"
                        + CollaborationEngineConfiguration.LICENSE_PUBLIC_PROPERTY
                        + "' property is set or, if you have a license file, the '"
                        + CollaborationEngineConfiguration.DATA_DIR_PUBLIC_PROPERTY
                        + "' property is pointing to the correct directory "
                        + "and that the directory contains the license file.",
                cause);
    }

    /*
     * For testing internal state of Statistics gathering
     */
    Map<YearMonth, Set<String>> getStatistics() {
        return statisticsCache.copyMap(statisticsCache.statistics);
    }

    static String calculateChecksum(JsonNode node)
            throws JsonProcessingException {
        return Base64.getEncoder().encodeToString(
                MessageDigestUtil.sha256(MAPPER.writeValueAsString(node)));
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.FIELD,
                Visibility.NON_PRIVATE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        return objectMapper;
    }

    static Path createLicenseFilePath(Path dirPath) {
        return Paths.get(dirPath.toString(), "ce-license.json");
    }
}
