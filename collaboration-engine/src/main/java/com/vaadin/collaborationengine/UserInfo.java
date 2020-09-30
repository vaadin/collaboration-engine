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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User information of a collaborating user, used with various features of the
 * collaboration engine.
 */
public class UserInfo {

    private String id;
    private String name;
    private String abbreviation;
    private String image;
    private int colorIndex;

    /**
     * Creates a new user info object from user id. The color index is
     * calculated based on the id.
     *
     * @param userId
     *            the user id, not {@code null}
     */
    @JsonCreator
    public UserInfo(@JsonProperty("id") String userId) {
        this(userId,
                CollaborationEngine.getInstance().getUserColorIndex(userId));
    }

    /*
     * This constructor is for SystemUserInfo so that userColors in CE won't be
     * messed up by this user.
     */
    protected UserInfo(String userId, int colorIndex) {
        Objects.requireNonNull(userId, "Null user id isn't supported");
        this.id = userId;
        this.colorIndex = colorIndex;
    }

    /**
     * Gets the user's unique identifier.
     *
     * @return the user's id, not {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the user's name.
     *
     * @return the user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name.
     *
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's abbreviation.
     * <p>
     * Note: This is not computed based on the user's name, but needs to be
     * explicitly set with {@link #setAbbreviation(String)}.
     *
     * @return the user's abbreviation
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Sets the user's abbreviation.
     *
     * @param abbreviation
     *            the abbreviation to set
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    /**
     * Gets the url of the user's image.
     *
     * @return the image url
     */
    public String getImage() {
        return image;
    }

    /**
     * Sets the url of the user's image.
     *
     * @param image
     *            the image url to set
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Gets the user's color index.
     * <p>
     * The color index defines the user specific color. In practice, color index
     * {@code n} means that the user color will be set as the CSS variable
     * {@code --vaadin-user-color-n}.
     *
     * @return the user's color index
     */
    public int getColorIndex() {
        return colorIndex;
    }

    /**
     * Sets the user's color index.
     *
     * @param colorIndex
     *            the color index to set
     * @see #getColorIndex()
     */
    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserInfo that = (UserInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
