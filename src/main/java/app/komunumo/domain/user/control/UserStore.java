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
package app.komunumo.domain.user.control;

import app.komunumo.data.db.tables.records.UserRecord;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.persistence.jooq.AbstractStore;
import app.komunumo.infra.persistence.jooq.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.tables.ActorHandle.ACTOR_HANDLE;
import static app.komunumo.data.db.tables.User.USER;

/**
 * <p>Handles persistence operations for users.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating, loading,
 * counting, and deleting users.</p>
 */
@Service
final class UserStore extends AbstractStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new user store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     * @param idGenerator the unique ID generator used by {@link AbstractStore}
     */
    UserStore(final @NotNull DSLContext dsl,
              final @NotNull UniqueIdGenerator idGenerator) {
        super(idGenerator);
        this.dsl = dsl;
    }

    /**
     * <p>Creates or updates a user record.</p>
     *
     * @param user the user data to persist
     * @return the persisted user
     */
    @NotNull UserDto storeUser(final @NotNull UserDto user) {
        final UserRecord userRecord = dsl.fetchOptional(USER, USER.ID.eq(user.id()))
                .orElse(dsl.newRecord(USER));
        createOrUpdate(USER, user, userRecord);
        return getUserById(userRecord.getId()).orElseThrow();
    }

    /**
     * <p>Loads all users.</p>
     *
     * @return all persisted users
     */
    @NotNull List<@NotNull UserDto> getAllUsers() {
        return dsl.select(USER.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(USER)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.USER_ID.eq(USER.ID))
                .fetch(this::toUserDto);
    }

    /**
     * <p>Counts all users with role {@code ADMIN}.</p>
     *
     * @return the number of admin users; never negative
     */
    int getAdminCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(USER)
                        .where(USER.ROLE.eq("admin"))
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Counts all users with role {@code USER}.</p>
     *
     * @return the number of regular users; never negative
     */
    int getUserCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(USER)
                        .where(USER.ROLE.eq("user"))
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Loads a user by unique identifier.</p>
     *
     * @param id the user ID
     * @return an optional containing the user if found; otherwise empty
     */
    @NotNull Optional<UserDto> getUserById(final @NotNull UUID id) {
        return dsl.select(USER.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(USER)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.USER_ID.eq(USER.ID))
                .where(USER.ID.eq(id))
                .fetchOptional(this::toUserDto);
    }

    /**
     * <p>Loads a user by email address.</p>
     *
     * @param email the email address to lookup
     * @return an optional containing the user if found; otherwise empty
     */
    @NotNull Optional<UserDto> getUserByEmail(final @NotNull String email) {
        return dsl.select(USER.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(USER)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.USER_ID.eq(USER.ID))
                .where(USER.EMAIL.eq(email))
                .fetchOptional(this::toUserDto);
    }

    /**
     * <p>Deletes a user.</p>
     *
     * @param user the user to delete
     * @return the number of deleted rows
     */
    int deleteUser(final @NotNull UserDto user) {
        return dsl.delete(USER)
                .where(USER.ID.eq(user.id()))
                .execute();
    }

    /**
     * <p>Changes the type of existing user.</p>
     *
     * @param user the user whose type should be changed
     * @param userType the new user type to persist
     */
    void changeUserType(final @NotNull UserDto user, final @NotNull UserType userType) {
        dsl.update(USER)
                .set(USER.TYPE, userType.name())
                .where(USER.ID.eq(user.id()))
                .execute();
    }

    private @NotNull UserDto toUserDto(final @NotNull Record record) {
        return new UserDto(
                record.get(USER.ID),
                record.get(USER.CREATED),
                record.get(USER.UPDATED),
                record.get(ACTOR_HANDLE.HANDLE),
                record.get(USER.EMAIL),
                record.get(USER.NAME),
                record.get(USER.BIO),
                record.get(USER.IMAGE_ID),
                record.get(USER.ROLE, app.komunumo.domain.user.entity.UserRole.class),
                record.get(USER.TYPE, UserType.class)
        );
    }
}
