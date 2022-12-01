/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.collaborationengine;

/**
 * Thrown when unable to convert back and forth between a Java object instance
 * and a Jackson {@link com.fasterxml.jackson.databind.JsonNode} instance. The
 * conversion is necessary for sending the Java object over the network.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JsonConversionException extends RuntimeException {

    /**
     * Constructs a new Json conversion exception
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause
     */
    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
