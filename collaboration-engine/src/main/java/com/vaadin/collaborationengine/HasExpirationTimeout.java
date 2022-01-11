/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
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
