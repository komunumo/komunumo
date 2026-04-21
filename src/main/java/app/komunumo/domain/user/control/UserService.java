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

import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.ActorHandleDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides user-related business operations and delegates persistence to {@link UserStore}.</p>
 *
 * <p>This service forms the control-layer API for user use cases while database access concerns
 * are encapsulated in the store implementation.</p>
 */
@Service
public class UserService {

    /**
     * <p>Store responsible for user persistence and query operations.</p>
     */
    private final @NotNull UserStore userStore;

    /**
     * <p>Service responsible for persistence and validation of actor handles.</p>
     */
    private final @NotNull ActorHandleService actorHandleService;

    /**
     * <p>Creates a new user service.</p>
     *
     * @param userStore the store used for user persistence access
     * @param actorHandleService the actor handle service used for user handle persistence
     */
    UserService(final @NotNull UserStore userStore,
                final @NotNull ActorHandleService actorHandleService) {
        this.userStore = userStore;
        this.actorHandleService = actorHandleService;
    }

    /**
     * <p>Creates or updates a user.</p>
     *
     * @param user the user data to persist
     * @return the persisted user
     */
    @Transactional
    public @NotNull UserDto storeUser(final @NotNull UserDto user) {
        final var handle = user.handle();
        final var storedUser = userStore.storeUser(user);
        final var userId = storedUser.id();
        if (userId == null) {
            throw new IllegalStateException("Stored user must have a user ID.");
        }

        if (storedUser.type() == UserType.LOCAL && handle != null && !handle.isBlank()) {
            actorHandleService.storeActorHandle(new ActorHandleDto(handle, userId, null));
        } else {
            actorHandleService.deleteActorHandleByUserId(userId);
        }

        return userStore.getUserById(userId).orElseThrow();
    }

    /**
     * <p>Loads all users.</p>
     *
     * @return all persisted users
     */
    public @NotNull List<@NotNull UserDto> getAllUsers() {
        return userStore.getAllUsers();
    }

    /**
     * <p>Counts all users with role {@code ADMIN}.</p>
     *
     * @return the number of admin users; never negative
     */
    public int getAdminCount() {
        return userStore.getAdminCount();
    }

    /**
     * <p>Counts all users with role {@code USER}.</p>
     *
     * @return the number of regular users; never negative
     */
    public int getUserCount() {
        return userStore.getUserCount();
    }

    /**
     * <p>Loads a user by unique identifier.</p>
     *
     * @param id the user ID
     * @return an optional containing the user if found; otherwise empty
     */
    public @NotNull Optional<UserDto> getUserById(final @NotNull UUID id) {
        return userStore.getUserById(id);
    }

    /**
     * <p>Loads a user by email address.</p>
     *
     * @param email the email address to lookup
     * @return an optional containing the user if found; otherwise empty
     */
    public @NotNull Optional<UserDto> getUserByEmail(final @NotNull String email) {
        return userStore.getUserByEmail(email);
    }

    /**
     * <p>Creates and stores an anonymous user for the given email address.</p>
     *
     * @param email the email address to assign to the anonymous user
     * @return the persisted anonymous user
     */
    public @NotNull UserDto createAnonymousUserWithEmail(final @NotNull String email) {
        final var user = new UserDto(null, null, null,
                null, email, "", "", null,
                UserRole.USER, UserType.ANONYMOUS);
        return storeUser(user);
    }

    /**
     * <p>Deletes a user.</p>
     *
     * @param user the user to delete
     * @return {@code true} if the user was deleted; otherwise {@code false}
     */
    @Transactional
    public boolean deleteUser(final @NotNull UserDto user) {
        if (user.id() == null) {
            throw new IllegalArgumentException("User ID must not be null! Maybe the user is not stored yet?");
        }
        actorHandleService.deleteActorHandleByUserId(user.id());
        return userStore.deleteUser(user) > 0;
    }

    /**
     * <p>Changes the type of existing user and returns the updated user.</p>
     *
     * @param user the user whose type should be changed
     * @param userType the new user type
     * @return the updated user after persistence
     */
    public @NotNull UserDto changeUserType(final @NotNull UserDto user, final @NotNull UserType userType) {
        if (user.id() == null) {
            throw new IllegalArgumentException("User ID must not be null! Maybe the user is not stored yet?");
        }
        userStore.changeUserType(user, userType);
        return getUserById(user.id()).orElseThrow();
    }

    /**
     * <p>Checks whether a user's profile is complete.</p>
     *
     * @param user the user to validate
     * @return {@code true} if profile-relevant fields are complete; otherwise {@code false}
     */
    public boolean isProfileComplete(final @NotNull UserDto user) {
        return !user.name().isBlank();
    }
}
