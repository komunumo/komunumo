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
import app.komunumo.domain.core.activitypub.entity.HandleOwnerContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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
     * <p>Minimum allowed handle length.</p>
     */
    public static final int HANDLE_MIN_LENGTH = 3;

    /**
     * <p>Maximum allowed handle length.</p>
     */
    public static final int HANDLE_MAX_LENGTH = 30;

    /**
     * <p>Regex fragment for a single allowed handle character.</p>
     */
    public static final String HANDLE_PATTERN = "[a-zA-Z0-9_]";

    /**
     * <p>Regex pattern that matches complete handle values with allowed characters.</p>
     */
    public static final String HANDLE_ALLOWED_CHARACTERS_PATTERN = "^%s+$".formatted(HANDLE_PATTERN);

    /**
     * <p>Regex pattern that matches complete handle values with allowed characters and valid length.</p>
     */
    public static final String HANDLE_ALLOWED_CHARACTERS_AND_LENGTH_PATTERN =
            "^%s{%d,%d}$".formatted(HANDLE_PATTERN, HANDLE_MIN_LENGTH, HANDLE_MAX_LENGTH);

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
     * <p>Stores the actor handle assigned to an owner.</p>
     *
     * @param actorHandle the actor handle data to persist
     * @return the persisted actor handle
     * @throws IllegalArgumentException if the handle is blank, contains invalid characters,
     *                                  has invalid length, or if the actor handle does not reference exactly one owner
     */
    public @NotNull ActorHandleDto storeActorHandle(final @NotNull ActorHandleDto actorHandle) {
        validateActorHandle(actorHandle);
        return actorHandleStore.storeActorHandle(actorHandle);
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
     * <p>Checks whether a handle is currently available.</p>
     *
     * @param handle the handle to check
     * @return {@code true} if no actor handle exists for the given value; otherwise {@code false}
     */
    public boolean isHandleAvailable(final @NotNull String handle) {
        return isHandleAvailable(handle, HandleOwnerContext.none());
    }

    /**
     * <p>Checks whether a handle is currently available for a specific owner.</p>
     *
     * @param handle the handle to check
     * @param ownerContext the owner requesting the handle
     * @return {@code true} if no actor handle exists for the given value, or if it already belongs to the owner;
     * otherwise {@code false}
     */
    public boolean isHandleAvailable(final @NotNull String handle, final @NotNull HandleOwnerContext ownerContext) {
        return actorHandleStore.getActorHandle(handle)
                .map(actorHandle -> ownerOwnsHandle(actorHandle, ownerContext))
                .orElse(true);
    }

    /**
     * <p>Checks whether the given actor handle belongs to the owner described by the context.</p>
     *
     * @param actorHandle the persisted actor handle to inspect
     * @param ownerContext the owner context to compare against
     * @return {@code true} if the handle belongs to the referenced user or community; otherwise {@code false}
     */
    private boolean ownerOwnsHandle(final @NotNull ActorHandleDto actorHandle,
                                    final @NotNull HandleOwnerContext ownerContext) {
        return ownerContext.userId() != null && ownerContext.userId().equals(actorHandle.userId())
                || ownerContext.communityId() != null && ownerContext.communityId().equals(actorHandle.communityId());
    }

    /**
     * <p>Deletes an actor handle by user ID.</p>
     *
     * @param userId the user ID whose actor handle should be deleted
     * @return {@code true} if an actor handle was deleted; otherwise {@code false}
     */
    public boolean deleteActorHandleByUserId(final @NotNull UUID userId) {
        return actorHandleStore.deleteActorHandleByUserId(userId) > 0;
    }

    /**
     * <p>Deletes an actor handle by community ID.</p>
     *
     * @param communityId the community ID whose actor handle should be deleted
     * @return {@code true} if an actor handle was deleted; otherwise {@code false}
     */
    public boolean deleteActorHandleByCommunityId(final @NotNull UUID communityId) {
        return actorHandleStore.deleteActorHandleByCommunityId(communityId) > 0;
    }

    /**
     * <p>Validates an actor handle DTO before persistence.</p>
     *
     * <p>The handle must not be blank and exactly one actor reference must be present:
     * either {@code userId} or {@code communityId}.</p>
     *
     * @param actorHandle the actor handle to validate
     * @throws IllegalArgumentException if the handle is blank, contains invalid characters,
     *                                  has invalid length, or if neither/both references are set
     */
    private void validateActorHandle(final @NotNull ActorHandleDto actorHandle) {
        final var handle = actorHandle.handle();

        if (handle.isBlank()) {
            throw new IllegalArgumentException("Actor handle must not be blank.");
        }

        if (!handle.matches(HANDLE_ALLOWED_CHARACTERS_AND_LENGTH_PATTERN)) {
            if (!handle.matches(HANDLE_ALLOWED_CHARACTERS_PATTERN)) {
                throw new IllegalArgumentException("Actor handle contains invalid characters.");
            }
            throw new IllegalArgumentException(
                    "Actor handle length must be between %d and %d characters."
                            .formatted(HANDLE_MIN_LENGTH, HANDLE_MAX_LENGTH));
        }

        final var hasUserReference = actorHandle.userId() != null;
        final var hasCommunityReference = actorHandle.communityId() != null;

        if (hasUserReference == hasCommunityReference) {
            throw new IllegalArgumentException("Exactly one of userId or communityId must be set.");
        }
    }
}
