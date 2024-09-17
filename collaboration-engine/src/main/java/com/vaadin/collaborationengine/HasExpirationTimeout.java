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

import java.time.Duration;
import java.util.Optional;

/**
 * Common interface to be implemented by types holding data associated to a
 * topic, which provides methods to set an expiration timeout on the data which
 * will be cleared after the timeout has passed since the last connection to the
 * topic has been closed.
 *
 * @author Vaadin Ltd.
 * @since 3.1
 */
public interface HasExpirationTimeout {

    /**
     * Gets the optional expiration timeout of the data. An empty
     * {@link Optional} is returned if no timeout is set, which means data is
     * not cleared when there are no connected users to the related topic (this
     * is the default).
     *
     * @return the expiration timeout
     */
    Optional<Duration> getExpirationTimeout();

    /**
     * Sets the expiration timeout of the data held by the implementing object.
     * If set, data is cleared when {@code expirationTimeout} has passed after
     * the last connection to the related topic is closed. If set to
     * {@code null}, the timeout is cancelled.
     *
     * @param expirationTimeout
     *            the expiration timeout
     */
    void setExpirationTimeout(Duration expirationTimeout);

}
