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

import app.komunumo.data.db.tables.records.ActorHandleRecord;
import app.komunumo.domain.core.activitypub.entity.ActorHandleDto;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.tables.ActorHandle.ACTOR_HANDLE;

/**
 * <p>Handles persistence operations for actor handles.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating,
 * loading, and deleting actor handle records.</p>
 */
@Service
final class ActorHandleStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new actor handle store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     */
    ActorHandleStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Stores the actor handle assigned to an owner.</p>
     *
     * <p>If the owner already has the same handle, the existing record is returned without
     * writing to the database. If the owner has a different handle, the previous record is
     * deleted first so that storing the new handle does not depend on updating the primary key in place.</p>
     *
     * @param actorHandle the actor handle to persist
     * @return the persisted actor handle DTO
     */
    @NotNull ActorHandleDto storeActorHandle(final @NotNull ActorHandleDto actorHandle) {
        final var existingActorHandle = fetchByActorReference(actorHandle);
        if (existingActorHandle.isPresent()) {
            final var existingActorHandleRecord = existingActorHandle.orElseThrow();
            if (actorHandle.handle().equals(existingActorHandleRecord.getHandle())) {
                return existingActorHandleRecord.into(ActorHandleDto.class);
            }
        }

        if (actorHandle.userId() != null) {
            deleteActorHandleByUserId(actorHandle.userId());
        } else if (actorHandle.communityId() != null) {
            deleteActorHandleByCommunityId(actorHandle.communityId());
        } else {
            throw new IllegalArgumentException("storeActorHandle requires a user or community reference.");
        }

        final var actorHandleRecord = dsl.newRecord(ACTOR_HANDLE);
        actorHandleRecord.from(actorHandle);
        actorHandleRecord.store();
        return actorHandleRecord.into(ActorHandleDto.class);
    }

    /**
     * <p>Loads an actor handle by its federated handle.</p>
     *
     * @param handle the federated handle to look up
     * @return an optional containing the actor handle if found; otherwise empty
     */
    @NotNull Optional<ActorHandleDto> getActorHandle(final @NotNull String handle) {
        return dsl.selectFrom(ACTOR_HANDLE)
                .where(ACTOR_HANDLE.HANDLE.eq(handle))
                .fetchOptionalInto(ActorHandleDto.class);
    }

    /**
     * <p>Deletes an actor handle by user ID.</p>
     *
     * @param userId the user ID whose actor handle should be deleted
     * @return the number of deleted rows
     */
    int deleteActorHandleByUserId(final @NotNull UUID userId) {
        return dsl.delete(ACTOR_HANDLE)
                .where(ACTOR_HANDLE.USER_ID.eq(userId))
                .execute();
    }

    /**
     * <p>Deletes an actor handle by community ID.</p>
     *
     * @param communityId the community ID whose actor handle should be deleted
     * @return the number of deleted rows
     */
    int deleteActorHandleByCommunityId(final @NotNull UUID communityId) {
        return dsl.delete(ACTOR_HANDLE)
                .where(ACTOR_HANDLE.COMMUNITY_ID.eq(communityId))
                .execute();
    }

    /**
     * <p>Loads an actor handle by its referenced owner.</p>
     *
     * @param actorHandle the actor handle containing either a user or community reference
     * @return an optional containing the matching actor handle record if found; otherwise empty
     */
    private @NotNull Optional<ActorHandleRecord> fetchByActorReference(final @NotNull ActorHandleDto actorHandle) {
        if (actorHandle.userId() != null) {
            return dsl.fetchOptional(ACTOR_HANDLE, ACTOR_HANDLE.USER_ID.eq(actorHandle.userId()));
        }
        if (actorHandle.communityId() != null) {
            return dsl.fetchOptional(ACTOR_HANDLE, ACTOR_HANDLE.COMMUNITY_ID.eq(actorHandle.communityId()));
        }
        return Optional.empty();
    }
}
