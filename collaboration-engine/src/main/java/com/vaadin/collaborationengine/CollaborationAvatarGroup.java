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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupI18n;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;

import static com.vaadin.collaborationengine.JsonUtil.jsonToUsers;
import static com.vaadin.collaborationengine.JsonUtil.usersToJson;

/**
 * Extension of the {@link AvatarGroup} component which integrates with the
 * {@link CollaborationEngine}. It updates the avatars in real time based on the
 * other attached avatar groups connected to the same topic.
 *
 * @author Vaadin Ltd
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

    static final String MAP_NAME = CollaborationAvatarGroup.class.getName();
    static final String KEY = "users";

    private Registration topicRegistration;
    private CollaborationMap map;

    private final UserInfo localUser;

    private ImageProvider imageProvider;

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
     *
     * @param localUser
     *            the information of the local user
     * @param topicId
     *            the id of the topic to connect to, or <code>null</code> to not
     *            connect the component to any topic
     */
    public CollaborationAvatarGroup(UserInfo localUser, String topicId) {
        this.localUser = Objects.requireNonNull(localUser,
                "User cannot be null");
        setTopic(topicId);
    }

    /**
     * Creates a new collaboration avatar group component with the provided
     * local user. The component should be assigned with a topic via
     * {@link #setTopic(String)} in order to show avatars of collaborating users
     * of the topic.
     *
     * @param localUser
     *            the information of the local user
     */
    public CollaborationAvatarGroup(UserInfo localUser) {
        this(localUser, null);
    }

    /**
     * Sets the topic to use with this component. The connection to the previous
     * topic (if any) and existing avatars are removed. Connection to the new
     * topic is opened and avatars of collaborating users in the new topic are
     * populated to this component.
     *
     * @param topicId
     *            the topic id to use, or <code>null</code> to not use any topic
     */
    public void setTopic(String topicId) {
        if (topicRegistration != null) {
            topicRegistration.remove();
            topicRegistration = null;
        }
        if (topicId != null) {
            topicRegistration = CollaborationEngine.getInstance()
                    .openTopicConnection(getContent(), topicId,
                            this::onConnectionActivate);
        }
    }

    /**
     * Gets the maximum number of avatars to display, or {@code null} if no max
     * has been set.
     *
     * @return the max number of avatars
     * @see AvatarGroup#getMaxItemsVisible()
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
     */
    public AvatarGroupI18n getI18n() {
        return getContent().getI18n();
    }

    /**
     * Sets the internationalization properties for this component.
     *
     * @param i18n
     *            the internationalized properties, not <code>null</code>
     */
    public void setI18n(AvatarGroupI18n i18n) {
        getContent().setI18n(i18n);
    }

    private Registration onConnectionActivate(TopicConnection topicConnection) {
        map = topicConnection.getNamedMap(MAP_NAME);

        updateUsers(map,
                oldValue -> Stream.concat(oldValue, Stream.of(localUser)));

        map.subscribe(event -> refreshItems());

        return this::onConnectionDeactivate;
    }

    private void onConnectionDeactivate() {
        updateUsers(map, oldValue -> oldValue.filter(this::isNotLocalUser));
        getContent().setItems(Collections.emptyList());
        map = null;
    }

    private void updateUsers(CollaborationMap map,
            SerializableFunction<Stream<UserInfo>, Stream<UserInfo>> updater) {
        while (true) {
            String oldValue = (String) map.get(KEY);
            List<UserInfo> oldUsers = jsonToUsers(oldValue);
            List<UserInfo> newUsers = updater.apply(oldUsers.stream())
                    .collect(Collectors.toList());
            if (map.replace(KEY, oldValue, usersToJson(newUsers))) {
                break;
            }
        }
    }

    private void refreshItems() {
        List<UserInfo> users = jsonToUsers((String) map.get(KEY));

        List<AvatarGroupItem> items = users != null ? users.stream()
                .filter(this::isNotLocalUser).map(this::userToAvatarGroupItem)
                .collect(Collectors.toList()) : Collections.emptyList();
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

        item.setColorIndex(user.getColorIndex());
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
     */
    public void setImageProvider(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;

        if (map != null) {
            refreshItems();
        }
    }

    /**
     * Gets the currently used image provider callback.
     *
     * @see #setImageProvider(ImageProvider)
     *
     * @return the current image provider callback, or <code>null</code> if no
     *         callback is set
     */
    public ImageProvider getImageProvider() {
        return imageProvider;
    }
}
