package com.vaadin.collaborationengine.util;

import com.vaadin.flow.server.StreamResource;

public class TestStreamResource extends StreamResource {
    public TestStreamResource(String name) {
        super(name, () -> null);
    }
}
