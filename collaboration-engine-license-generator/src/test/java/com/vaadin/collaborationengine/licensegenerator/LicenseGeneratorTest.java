/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine.licensegenerator;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

import com.vaadin.collaborationengine.licensegenerator.LicenseGenerator.LicenseInfo;

import static org.junit.Assert.assertEquals;

/**
 * Test suite for {@link LicenseGenerator}.
 */
public class LicenseGeneratorTest {

    private final LicenseGenerator generator = new LicenseGenerator();

    /**
     * Tests that the generator produces a correct JSON.
     */
    @Test
    public void generatesCorrectJson() {
        String key = "dee24b72-3b39-4794-a056-3ea547d80175";
        String owner = "Foo";
        int quota = 10;
        String endDate = "2022-12-31";
        String checksum = "cPC58R5AMayYIZBqq1Jo3Bn5L5rrD8BJyNceX8VcJeM=";

        LicenseInfo content = new LicenseInfo(UUID.fromString(key), owner,
                quota, LocalDate.parse(endDate));

        String referenceJson = "{\"content\":{\"key\":\"" + key
                + "\",\"owner\":\"" + owner + "\",\"quota\":" + quota
                + ",\"endDate\":" + "\"" + endDate + "\"},\"checksum\":\""
                + checksum + "\"}";
        String resultingJson = generator.generateLicense(content);

        assertEquals(referenceJson, resultingJson);
    }
}
