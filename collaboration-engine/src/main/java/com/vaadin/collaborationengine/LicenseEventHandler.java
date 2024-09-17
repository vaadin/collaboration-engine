/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

/**
 * Event handler that gets notified on license events. The handler can be set
 * when creating an instance of {@link CollaborationEngineConfiguration}.
 *
 * @author Vaadin Ltd
 * @since 3.0
 * @deprecated any implementation of this interface won't receive any events,
 *             prefer using the default {@link CollaborationEngineConfiguration}
 *             constructor
 */
@Deprecated(since = "6.3", forRemoval = true)
@FunctionalInterface
public interface LicenseEventHandler {

    /**
     * Handles a license event.
     *
     * @param event
     *            the license event, not {@code null}
     */
    void handleLicenseEvent(LicenseEvent event);
}
