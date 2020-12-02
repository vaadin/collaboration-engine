package com.vaadin.collaborationengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.vaadin.collaborationengine.CollaborationEngine.CollaborationEngineConfig;
import com.vaadin.collaborationengine.licensegenerator.LicenseGenerator;
import com.vaadin.collaborationengine.util.TestUtils;

public abstract class AbstractLicenseTest {

    final static int QUOTA = 3;
    final static int GRACE_QUOTA = 10 * QUOTA;

    Path statsFilePath;
    Path licenseFilePath;

    CollaborationEngine ce;
    Path testDataDir = Paths.get(System.getProperty("user.home"), ".vaadin",
            "ce-tests");
    CollaborationEngineConfig config = new CollaborationEngineConfig(true,
            testDataDir);

    LicenseGenerator licenseGenerator = new LicenseGenerator();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() throws IOException {
        if (!testDataDir.toFile().exists()) {
            Files.createDirectories(testDataDir);
        }
        statsFilePath = FileHandler.createStatsFilePath(testDataDir);
        licenseFilePath = FileHandler.createLicenseFilePath(testDataDir);

        // Delete the stats file before each run to make sure we can test with a
        // clean state
        Files.deleteIfExists(statsFilePath);

        // Set quota to 3
        writeToLicenseFile(QUOTA, LocalDate.of(2222, 1, 1));

        ce = new CollaborationEngine();
        ce.setConfigProvider(() -> config);
    }

    void assertStatsFileContent(String expected) {
        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(statsFilePath));
        } catch (IOException e) {
            Assert.fail("Failed to read the file at " + statsFilePath);
        }
        Assert.assertEquals("Unexpected statistics file content", expected,
                fileContent);
    }

    void writeToStatsFile(String content) throws IOException {
        Files.write(statsFilePath, content.getBytes());
    }

    void writeToLicenseFile(String content) throws IOException {
        Files.write(licenseFilePath, content.getBytes());
    }

    void writeToLicenseFile(int quota, LocalDate endDate) throws IOException {
        String licenseJson = licenseGenerator.generateLicense("Test company",
                quota, endDate);
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
}
