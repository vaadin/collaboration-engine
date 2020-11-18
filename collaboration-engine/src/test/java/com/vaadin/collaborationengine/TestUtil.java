package com.vaadin.collaborationengine;

import com.vaadin.collaborationengine.CollaborationEngine.CollaborationEngineConfig;

public class TestUtil {

    /**
     * Sets the required config that is normally set by the
     * VaadinServiceInitListener.
     */
    static void setDummyCollaborationEngineConfig() {
        setDummyCollaborationEngineConfig(CollaborationEngine.getInstance());
    }

    /**
     * Sets the required config that is normally set by the
     * VaadinServiceInitListener.
     */
    static void setDummyCollaborationEngineConfig(CollaborationEngine ce) {
        ce.setConfigProvider(
                () -> new CollaborationEngineConfig(false, false, null));
    }
}
