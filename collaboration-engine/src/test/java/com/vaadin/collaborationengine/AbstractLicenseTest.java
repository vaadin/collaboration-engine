package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.vaadin.collaborationengine.FileLicenseStorage.StatisticsInfo;
import com.vaadin.collaborationengine.LicenseEvent.LicenseEventType;
import com.vaadin.collaborationengine.TestUtil.MockConfiguration;
import com.vaadin.collaborationengine.licensegenerator.LicenseGenerator;
import com.vaadin.collaborationengine.util.MockService;
import com.vaadin.collaborationengine.util.TestUtils;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.server.VaadinService;

public abstract class AbstractLicenseTest {

    public static class SpyLicenseEventHandler implements LicenseEventHandler {

        private final Map<LicenseEventType, Integer> handledEvents = new HashMap<>();

        private final List<String> messages = new ArrayList<String>();

        @Override
        public void handleLicenseEvent(LicenseEvent event) {
            handledEvents.compute(event.getType(),
                    (k, v) -> v == null ? 1 : v + 1);
            messages.add(event.getMessage());
        }

        Map<LicenseEventType, Integer> getHandledEvents() {
            return handledEvents;
        }

        List<String> getMessages() {
            return messages;
        }
    }

    final static int QUOTA = 3;
    final static int GRACE_QUOTA = 10 * QUOTA;
    final static UUID LICENSE_KEY = UUID.randomUUID();

    ObjectMapper objectMapper = LicenseHandler.createObjectMapper();

    Path statsFilePath;
    Path licenseFilePath;

    MockConfiguration configuration;

    VaadinService service;
    CollaborationEngine ce;
    Path testDataDir = Paths.get(System.getProperty("user.home"), ".vaadin",
            "ce-tests");

    LicenseGenerator licenseGenerator = new LicenseGenerator();

    SpyLicenseEventHandler spyEventHandler = new SpyLicenseEventHandler();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() throws IOException {
        if (!testDataDir.toFile().exists()) {
            Files.createDirectories(testDataDir);
        }
        statsFilePath = FileLicenseStorage.createStatsFilePath(testDataDir);
        licenseFilePath = LicenseHandler.createLicenseFilePath(testDataDir);

        // Delete the stats file before each run to make sure we can test with a
        // clean state
        Files.deleteIfExists(statsFilePath);

        // Set quota to 3
        writeToLicenseFile(QUOTA, LocalDate.of(2222, 1, 1));

        service = new MockService();
        VaadinService.setCurrent(service);

        configuration = new MockConfiguration(spyEventHandler);
        configuration.setDataDirPath(testDataDir);
        configuration.setLicenseCheckingEnabled(true);

        ce = CollaborationEngine.configure(service, configuration,
                new TestUtil.TestCollaborationEngine(), true);
    }

    @After
    public void cleanUp() {
        VaadinService.setCurrent(null);
        System.clearProperty(
                CollaborationEngineConfiguration.LICENSE_PUBLIC_PROPERTY);
    }

    void assertStatsFileContent(String expected) {
        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(statsFilePath));
        } catch (IOException e) {
            Assert.fail("Failed to read the file at " + statsFilePath);
        }
        Assert.assertEquals("Unexpected statistics file content",
                getStatisticsWithChecksum(expected), fileContent);
    }

    FileLicenseStorage.StatisticsInfo readStatsFileContent()
            throws IOException {
        JsonNode statsJson = objectMapper.readTree(statsFilePath.toFile());
        return objectMapper.treeToValue(statsJson,
                FileLicenseStorage.StatisticsInfoWrapper.class).content;
    }

    void writeToStatsFile(StatisticsInfo stats) {
        try {
            writeToStatsFile(objectMapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json conversion failed.");
        }
    }

    void writeToStatsFile(String content) {
        String wrapper = getStatisticsWithChecksum(content);
        try {
            Files.write(statsFilePath, wrapper.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Writing to stats file failed.");
        }
    }

    String getStatisticsWithChecksum(String content) {
        String checksum = Base64.getEncoder()
                .encodeToString(MessageDigestUtil.sha256(content));
        return "{\"content\":" + content + ",\"checksum\":\"" + checksum
                + "\"}";
    }

    void writeToLicenseFile(String content) throws IOException {
        Files.write(licenseFilePath, content.getBytes());
    }

    void writeToLicenseFile(int quota, LocalDate endDate) throws IOException {
        String licenseJson = licenseGenerator.generateLicense(LICENSE_KEY,
                "Test company", quota, endDate);
        writeToLicenseFile(licenseJson);
    }

    List<String> generateIds(int count) {
        return IntStream.range(0, count).mapToObj(String::valueOf)
                .collect(Collectors.toList());
    }

    void fillGraceQuota() {
        generateIds(GRACE_QUOTA).forEach(i -> TestUtils.openEagerConnection(ce,
                "topic-id-to-fill-grace-quota", t -> {
                    // NO-OP
                }));
    }

    YearMonth getCurrentMonth() {
        return YearMonth.from(ce.getClock().instant().atZone(ZoneOffset.UTC));
    }

    void setCurrentDate(LocalDate currentDate) {
        Instant instant = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        ce.setClock(clock);
    }
}
