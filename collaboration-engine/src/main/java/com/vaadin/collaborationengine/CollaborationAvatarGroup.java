/*
 * Copyright 2020-2022 Vaadin Ltd.
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupI18n;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * Extension of the {@link AvatarGroup} component which integrates with the
 * {@link CollaborationEngine}. It updates the avatars in real time based on the
 * other attached avatar groups connected to the same topic.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class CollaborationAvatarGroup extends Composite<AvatarGroup>
        implements HasSize, HasStyle, HasTheme {

    /**
     * Callback for creating a stream resource with the image for a specific
     * user. This allows loading the user image from a dynamic location such as
     * a database.
     *
     * @see StreamResource
     * @see CollaborationAvatarGroup#setImageProvider(ImageProvider)
     * @since 1.0
     */
    @FunctionalInterface
    public interface ImageProvider {
        /**
         * Gets a stream resource that provides the avatar image for the given
         * user.
         *
         * @param user
         *            the user for which to get a stream resource with the
         *            image, not <code>null</code>
         * @return the stream resource to use for the image, or
         *         <code>null</code> to not show use any avatar image for the
         *         given user
         */
        AbstractStreamResource getImageResource(UserInfo user);
    }

    private final CollaborationEngine ce;

    private final UserInfo localUser;

    private final List<UserInfo> userInfoCache = new ArrayList<>();

    private PresenceManager presenceManager;

    private String topicId;

    private ImageProvider imageProvider;

    private boolean ownAvatarVisible;

    static {
        UsageStatistics.markAsUsed(
                CollaborationEngine.COLLABORATION_ENGINE_NAME
                        + "/CollaborationAvatarGroup",
                CollaborationEngine.COLLABORATION_ENGINE_VERSION);
    }

    /**
     * Creates a new collaboration avatar group component with the provided
     * local user and topic id.
     * <p>
     * The provided user information is used in the local user's avatar which is
     * displayed to the other users.
     * <p>
     * Whenever another collaboration avatar group with the same topic id is
     * attached to another user's UI, this avatar group is updated to include an
     * avatar with that user's information.
     * <p>
     * If a {@code null} topic id is provided, the component won't display any
     * avatars, until connecting to a non-null topic with
     * {@link #setTopic(String)}.
     *
     * @param localUser
     *            the information of the local user
     * @param topicId
     *            the id of the topic to connect to, or <code>null</code> to not
     *            connect the component to any topic
     * @since 1.0
     */
    public CollaborationAvatarGroup(UserInfo localUser, String topicId) {
        this(localUser, topicId, CollaborationEngine.getInstance());
    }

    CollaborationAvatarGroup(UserInfo localUser, String topicId,
            CollaborationEngine ce) {
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
        this.ce = ce;
        this.ownAvatarVisible = true;

        setTopic(topicId);
        refreshItems();
    }

    /**
     * Sets the topic to use with this component. The connection to the previous
     * topic (if any) and existing avatars are removed. Connection to the new
     * topic is opened and avatars of collaborating users in the new topic are
     * populated to this component.
     * <p>
     * If the topic id is {@code null}, no avatars will be displayed.
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     * @since 1.0
     */
    public void setTopic(String topicId) {
        if (Objects.equals(this.topicId, topicId)) {
            return;
        }

        if (this.presenceManager != null) {
            this.presenceManager.close();
            this.presenceManager = null;
        }

        this.topicId = topicId;

        if (topicId != null) {
            this.presenceManager = new PresenceManager(
                    new ComponentConnectionContext(this), localUser, topicId,
                    ce);
            this.presenceManager.markAsPresent(true);
            this.presenceManager.setNewUserHandler(userInfo -> {
                userInfoCache.add(userInfo);
                refreshItems();
                return () -> {
                    userInfoCache.remove(userInfo);
                    refreshItems();
                };
            });
        }
    }

    /**
     * Gets the maximum number of avatars to display, or {@code null} if no max
     * has been set.
     *
     * @return the max number of avatars
     * @see AvatarGroup#getMaxItemsVisible()
     * @since 1.0
     */
    public Integer getMaxItemsVisible() {
        return getContent().getMaxItemsVisible();
    }

    /**
     * Sets the the maximum number of avatars to display.
     * <p>
     * By default, all the avatars are displayed. When max is set, the
     * overflowing avatars are grouped into one avatar.
     *
     * @param max
     *            the max number of avatars, or {@code null} to remove the max
     * @see AvatarGroup#setMaxItemsVisible(Integer)
     * @since 1.0
     */
    public void setMaxItemsVisible(Integer max) {
        getContent().setMaxItemsVisible(max);
    }

    /**
     * Adds theme variants to the avatar group component.
     *
     * @param variants
     *            theme variants to add
     * @see AvatarGroup#addThemeVariants(AvatarGroupVariant...)
     * @since 1.0
     */
    public void addThemeVariants(AvatarGroupVariant... variants) {
        getContent().addThemeVariants(variants);
    }

    /**
     * Removes theme variants from the avatar group component.
     *
     * @param variants
     *            theme variants to remove
     * @see AvatarGroup#removeThemeVariants(AvatarGroupVariant...)
     * @since 1.0
     */
    public void removeThemeVariants(AvatarGroupVariant... variants) {
        getContent().removeThemeVariants(variants);
    }

    /**
     * Gets the internationalization object previously set for this component.
     * <p>
     * Note: updating the object content that is gotten from this method will
     * not update the lang on the component if not set back using
     * {@link CollaborationAvatarGroup#setI18n(AvatarGroupI18n)}
     *
     * @return the i18n object. It will be <code>null</code>, if the i18n
     *         properties haven't been set.
     * @since 1.0
     */
    public AvatarGroupI18n getI18n() {
        return getContent().getI18n();
    }

    /**
     * Sets the internationalization properties for this component.
     *
     * @param i18n
     *            the internationalized properties, not <code>null</code>
     * @since 1.0
     */
    public void setI18n(AvatarGroupI18n i18n) {
        getContent().setI18n(i18n);
    }

    private void refreshItems() {
        List<AvatarGroupItem> items = Stream
                .concat(Stream.of(localUser), userInfoCache.stream()).distinct()
                .filter(user -> ownAvatarVisible || isNotLocalUser(user))
                .map(this::userToAvatarGroupItem).collect(Collectors.toList());
        getContent().setItems(items);
    }

    private AvatarGroupItem userToAvatarGroupItem(UserInfo user) {
        AvatarGroupItem item = new AvatarGroupItem();
        item.setName(user.getName());
        item.setAbbreviation(user.getAbbreviation());

        if (imageProvider == null) {
            item.setImage(user.getImage());
        } else {
            item.setImageResource(imageProvider.getImageResource(user));
        }

        item.setColorIndex(ce.getUserColorIndex(user));
        return item;
    }

    private boolean isNotLocalUser(UserInfo user) {
        return !localUser.equals(user);
    }

    /**
     * Sets an image provider callback for dynamically loading avatar images for
     * a given user. The image can be loaded on-demand from a database or using
     * any other source of IO streams.
     * <p>
     * If no image callback is defined, then the image URL defined by
     * {@link UserInfo#getImage()} is directly passed to the browser. This means
     * that avatar images need to be available as static files or served
     * dynamically from a custom servlet. This is the default.
     * <p>
     *
     * Usage example:
     *
     * <pre>
     * collaborationAvatarGroup.setImageProvider(userInfo -> {
     *     StreamResource streamResource = new StreamResource(
     *             "avatar_" + userInfo.getId(), () -> {
     *                 User userEntity = userRepository
     *                         .findById(userInfo.getId());
     *                 byte[] profilePicture = userEntity.getProfilePicture();
     *                 return new ByteArrayInputStream(profilePicture);
     *             });
     *     streamResource.setContentType("image/png");
     *     return streamResource;
     * });
     * </pre>
     *
     * @param imageProvider
     *            the image provider to use, or <code>null</code> to use image
     *            URLs directly from the user info object
     * @since 1.0
     */
    public void setImageProvider(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
        refreshItems();
    }

    /**
     * Gets the currently used image provider callback.
     *
     * @see #setImageProvider(ImageProvider)
     *
     * @return the current image provider callback, or <code>null</code> if no
     *         callback is set
     * @since 1.0
     */
    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    /**
     * Gets whether the user's own avatar is displayed in the avatar group or
     * not.
     *
     * @return {@code true} if the user's own avatar is included in the group,
     *         {@code false} if not
     * @see #setOwnAvatarVisible(boolean)
     * @since 1.0
     */
    public boolean isOwnAvatarVisible() {
        return ownAvatarVisible;
    }

    /**
     * Sets whether to display user's own avatar in the avatar group or not. The
     * default value is {@code true}.
     * <p>
     * To display user's own avatar separately from other users, you can set
     * this to {@code false}, create a separate {@link Avatar} component and
     * place it anywhere you like in your view.
     *
     * @param ownAvatarVisible
     *            {@code true} to include user's own avatar, {@code false} to
     *            not include it
     * @since 1.0
     */
    public void setOwnAvatarVisible(boolean ownAvatarVisible) {
        this.ownAvatarVisible = ownAvatarVisible;
        refreshItems();
    }
}
