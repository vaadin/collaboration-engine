/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine.licensegenerator;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.flow.internal.MessageDigestUtil;

/**
 * A generator of Collaboration Engine licenses.
 * <p>
 * This generator produces licensing data from inputs such as license owner,
 * user quota and end-date. It also generates a unique license identifier and
 * calculates the checksum of the license content.
 */
public class LicenseGenerator {

    static class LicenseInfo {

        final UUID key;

        final String owner;

        final int quota;

        final LocalDate endDate;

        @JsonCreator
        LicenseInfo(@JsonProperty(value = "key", required = true) UUID key,
                @JsonProperty(value = "owner", required = true) String owner,
                @JsonProperty(value = "quota", required = true) int quota,
                @JsonProperty(value = "endDate", required = true) LocalDate endDate) {
            this.key = key;
            this.owner = owner;
            this.quota = quota;
            this.endDate = endDate;
        }

        /**
         * Gets the key.
         *
         * @return the key
         */
        public UUID getKey() {
            return key;
        }

        /**
         * Gets the owner.
         *
         * @return the owner
         */
        public String getOwner() {
            return owner;
        }

        /**
         * Gets the quota.
         *
         * @return the quota
         */
        public int getQuota() {
            return quota;
        }

        /**
         * Gets the end date.
         *
         * @return the end date
         */
        public LocalDate getEndDate() {
            return endDate;
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

        /**
         * Gets the content.
         *
         * @return the content
         */
        public LicenseInfo getContent() {
            return content;
        }

        /**
         * Gets the checksum.
         *
         * @return the checksum
         */
        public String getChecksum() {
            return checksum;
        }
    }

    private final ObjectMapper mapper;

    /**
     * Creates the license-generator.
     */
    public LicenseGenerator() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    }

    /**
     * Generates a license in {@code JSON} format.
     * <p>
     * It takes the owner name, the user quota and the license end-date.
     * <p>
     * A <i>checksum</i> of the license content will be calculated and appended
     * to the {@code JSON} object.
     *
     * @param owner
     *            the name of the license owner
     * @param quota
     *            the license user quota
     * @param endDate
     *            the license end-date
     * @return the license in {@code JSON} format
     */
    public String generateLicense(String owner, int quota, LocalDate endDate) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(endDate);
        UUID key = UUID.randomUUID();
        return generateLicense(key, owner, quota, endDate);
    }

    /**
     * Generates a license in {@code JSON} format.
     * <p>
     * It takes in a license key, the owner name, the user quota and the license
     * end-date.
     * <p>
     * A <i>checksum</i> of the license content will be calculated and appended
     * to the {@code JSON} object.
     *
     * @param key
     *            the license key
     * @param owner
     *            the name of the license owner
     * @param quota
     *            the license user quota
     * @param endDate
     *            the license end-date
     * @return the license in {@code JSON} format
     */
    public String generateLicense(UUID key, String owner, int quota,
            LocalDate endDate) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(owner);
        Objects.requireNonNull(endDate);
        LicenseInfo content = new LicenseInfo(key, owner, quota, endDate);
        return generateLicense(content);
    }

    String generateLicense(LicenseInfo content) {
        String checksum = calculateContentChecksum(content);
        LicenseInfoWrapper wrapper = new LicenseInfoWrapper(content, checksum);
        return serializeToJson(wrapper);
    }

    private String calculateContentChecksum(LicenseInfo content) {
        String json = serializeToJson(content);
        byte[] checksum = MessageDigestUtil.sha256(json);
        return Base64.getEncoder().encodeToString(checksum);
    }

    private String serializeToJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Cannot serialize the given argument", e);
        }
    }
}
