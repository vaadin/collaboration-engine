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

/**
 * Thrown when unable to convert back and forth between a Java object instance
 * and a Jackson {@link com.fasterxml.jackson.databind.JsonNode} instance. The
 * conversion is necessary for sending the Java object over the network.
 *
 * @author Vaadin Ltd
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
