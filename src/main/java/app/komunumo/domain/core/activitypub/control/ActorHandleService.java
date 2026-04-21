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
package app.komunumo.domain.core.activitypub.control;

import app.komunumo.domain.core.activitypub.entity.ActorHandleDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides actor handle business operations and delegates persistence to {@link ActorHandleStore}.</p>
 *
 * <p>This service ensures actor handles are valid before they are persisted.</p>
 */
@Service
public final class ActorHandleService {

    /**
     * <p>Store responsible for actor handle persistence and query operations.</p>
     */
    private final @NotNull ActorHandleStore actorHandleStore;

    /**
     * <p>Creates a new actor handle service.</p>
     *
     * @param actorHandleStore the store used for actor handle persistence access
     */
    ActorHandleService(final @NotNull ActorHandleStore actorHandleStore) {
        this.actorHandleStore = actorHandleStore;
    }

    /**
     * <p>Stores or updates an actor handle.</p>
     *
     * @param actorHandle the actor handle data to persist
     * @return the persisted actor handle
     * @throws IllegalArgumentException if the handle is blank or if neither/both actor references are set
     */
    public @NotNull ActorHandleDto storeActorHandle(final @NotNull ActorHandleDto actorHandle) {
        validateActorHandle(actorHandle);
        return actorHandleStore.storeActorHandle(actorHandle);
    }

    /**
     * <p>Loads all actor handles ordered by handle.</p>
     *
     * @return all persisted actor handles
     */
    public @NotNull List<@NotNull ActorHandleDto> getActorHandles() {
        return actorHandleStore.getActorHandles();
    }

    /**
     * <p>Loads an actor handle by federated handle.</p>
     *
     * @param handle the handle to look up
     * @return an optional containing the actor handle if found; otherwise empty
     */
    public @NotNull Optional<ActorHandleDto> getActorHandle(final @NotNull String handle) {
        return actorHandleStore.getActorHandle(handle);
    }

    /**
     * <p>Loads an actor handle by user ID.</p>
     *
     * @param userId the user ID to look up
     * @return an optional containing the actor handle if found; otherwise empty
     */
    public @NotNull Optional<ActorHandleDto> getActorHandleByUserId(final @NotNull UUID userId) {
        return actorHandleStore.getActorHandleByUserId(userId);
    }

    /**
     * <p>Loads an actor handle by community ID.</p>
     *
     * @param communityId the community ID to look up
     * @return an optional containing the actor handle if found; otherwise empty
     */
    public @NotNull Optional<ActorHandleDto> getActorHandleByCommunityId(final @NotNull UUID communityId) {
        return actorHandleStore.getActorHandleByCommunityId(communityId);
    }

    /**
     * <p>Deletes an actor handle.</p>
     *
     * @param actorHandle the actor handle to delete
     * @return {@code true} if the actor handle was deleted; otherwise {@code false}
     */
    public boolean deleteActorHandle(final @NotNull ActorHandleDto actorHandle) {
        return actorHandleStore.deleteActorHandle(actorHandle) > 0;
    }

    /**
     * <p>Validates an actor handle DTO before persistence.</p>
     *
     * <p>The handle must not be blank and exactly one actor reference must be present:
     * either {@code userId} or {@code communityId}.</p>
     *
     * @param actorHandle the actor handle to validate
     * @throws IllegalArgumentException if the handle is blank or if neither/both references are set
     */
    private void validateActorHandle(final @NotNull ActorHandleDto actorHandle) {
        if (actorHandle.handle().isBlank()) {
            throw new IllegalArgumentException("Actor handle must not be blank.");
        }

        final var hasUserReference = actorHandle.userId() != null;
        final var hasCommunityReference = actorHandle.communityId() != null;

        if (hasUserReference == hasCommunityReference) {
            throw new IllegalArgumentException("Exactly one of userId or communityId must be set.");
        }
    }
}
