/*
 * Komunumo - Open Source Community Manager
 * Copyright (C) Marcus Fihlon and the individual contributors to Komunumo.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package app.komunumo.domain.community.control;

import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.community.entity.CommunityWithImageDto;
import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.ActorHandleDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides community-related business operations and delegates persistence to {@link CommunityStore}.</p>
 *
 * <p>This service forms the control-layer API for community use cases and keeps data access
 * concerns encapsulated in the store implementation.</p>
 */
@Service
public class CommunityService {

    /**
     * <p>Store responsible for all community persistence operations.</p>
     */
    private final @NotNull CommunityStore communityStore;

    /**
     * <p>Service responsible for persistence and validation of actor handles.</p>
     */
    private final @NotNull ActorHandleService actorHandleService;

    /**
     * <p>Creates a new community service.</p>
     *
     * @param communityStore the store used for community persistence access
     * @param actorHandleService the actor handle service used for community handle persistence
     */
    CommunityService(final @NotNull CommunityStore communityStore,
                     final @NotNull ActorHandleService actorHandleService) {
        this.communityStore = communityStore;
        this.actorHandleService = actorHandleService;
    }

    /**
     * <p>Creates or updates a community.</p>
     *
     * @param community the community data to persist
     * @return the persisted community
     */
    @Transactional
    public @NotNull CommunityDto storeCommunity(final @NotNull CommunityDto community) {
        final var handle = community.handle();
        final var communityId = communityStore.storeCommunity(community).id();
        if (communityId == null) {
            throw new IllegalStateException("Stored community must have a community ID.");
        }

        actorHandleService.storeActorHandle(new ActorHandleDto(handle, null, communityId));
        return communityStore.getCommunity(communityId).orElseThrow();
    }

    /**
     * <p>Loads a community by its unique identifier.</p>
     *
     * @param id the community ID
     * @return an optional containing the community if found; otherwise empty
     */
    public @NotNull Optional<CommunityDto> getCommunity(final @NotNull UUID id) {
        return communityStore.getCommunity(id);
    }

    /**
     * <p>Loads a community by profile and includes optional image data.</p>
     *
     * @param profile the community profile slug/name
     * @return an optional containing the community with optional image data if found; otherwise empty
     */
    public @NotNull Optional<CommunityWithImageDto> getCommunityWithImage(final @NotNull String profile) {
        return communityStore.getCommunityWithImage(profile);
    }

    /**
     * <p>Loads all communities ordered by name.</p>
     *
     * @return all communities
     */
    public @NotNull List<@NotNull CommunityDto> getCommunities() {
        return communityStore.getCommunities();
    }

    /**
     * <p>Loads all communities with optional image data ordered by name.</p>
     *
     * @return all communities including optional image projection
     */
    public @NotNull List<@NotNull CommunityWithImageDto> getCommunitiesWithImage() {
        return communityStore.getCommunitiesWithImage();
    }

    /**
     * <p>Loads all communities where the given user has manager permissions.</p>
     *
     * @param user the user whose manageable communities should be loaded
     * @return all communities the user can manage
     */
    public @NotNull List<@NotNull CommunityDto> getCommunitiesForManager(final @NotNull UserDto user) {
        return communityStore.getCommunitiesForManager(user);
    }

    /**
     * <p>Counts all persisted communities.</p>
     *
     * @return the total number of communities; never negative
     */
    public int getCommunityCount() {
        return communityStore.getCommunityCount();
    }

    /**
     * <p>Checks whether the given profile name is available for a new community.</p>
     *
     * @param profile the profile name to validate
     * @return {@code true} if the profile name is available; otherwise {@code false}
     */
    public boolean isProfileNameAvailable(final @NotNull String profile) {
        return communityStore.getCommunityCount(profile) == 0;
    }

    /**
     * <p>Deletes the given community.</p>
     *
     * @param community the community to delete
     * @return {@code true} if the community was deleted; otherwise {@code false}
     */
    @Transactional
    public boolean deleteCommunity(final @NotNull CommunityDto community) {
        if (community.id() == null) {
            throw new IllegalArgumentException("Community ID must not be null! Maybe the community is not stored yet?");
        }
        actorHandleService.deleteActorHandleByCommunityId(community.id());
        return communityStore.deleteCommunity(community) > 0;
    }

    /**
     * <p>Checks whether the given user is a manager in at least one community.</p>
     *
     * <p>Administrators always have permission. Other users need manager permissions
     * on at least one community.</p>
     *
     * @param user the user to check
     * @return {@code true} if the user has community manager permissions; otherwise {@code false}
     */
    public boolean isCommunityManager(final @NotNull UserDto user) {
        if (user.role() == UserRole.ADMIN) {
            return true;
        }

        return !communityStore.getCommunitiesForManager(user).isEmpty();
    }
}
