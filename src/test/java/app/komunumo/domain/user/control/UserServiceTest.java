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
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void storeUserDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var service = new UserService(store, actorHandleService);
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        when(store.storeUser(user)).thenReturn(user);
        assertThat(user.id()).isNotNull();
        when(store.getUserById(user.id())).thenReturn(Optional.of(user));

        final var result = service.storeUser(user);

        assertThat(result).isEqualTo(user);
        verify(store).storeUser(user);
        verify(actorHandleService).deleteActorHandleByUserId(user.id());
    }

    @Test
    void getAllUsersDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        final var users = List.of(createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null));
        when(store.getAllUsers()).thenReturn(users);

        final var result = service.getAllUsers();

        assertThat(result).isEqualTo(users);
    }

    @Test
    void getAdminCountDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        when(store.getAdminCount()).thenReturn(2);

        final var result = service.getAdminCount();

        assertThat(result).isEqualTo(2);
    }

    @Test
    void getUserCountDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        when(store.getUserCount()).thenReturn(11);

        final var result = service.getUserCount();

        assertThat(result).isEqualTo(11);
    }

    @Test
    void getUserByIdDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        assertThat(user.id()).isNotNull();
        when(store.getUserById(user.id())).thenReturn(Optional.of(user));

        final var result = service.getUserById(user.id());

        assertThat(result).contains(user);
    }

    @Test
    void getUserByEmailDelegatesToStore() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        when(store.getUserByEmail("user@example.org")).thenReturn(Optional.of(user));

        final var result = service.getUserByEmail("user@example.org");

        assertThat(result).contains(user);
    }

    @Test
    void createAnonymousUserWithEmailBuildsAnonymousUserAndStoresIt() {
        final var store = mock(UserStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var service = new UserService(store, actorHandleService);
        final var storedUser = createUser(UUID.randomUUID(), "anonymous@example.org", "", UserRole.USER, UserType.ANONYMOUS, null);
        final var anonymousUser = createUser(null, "anonymous@example.org", "", UserRole.USER, UserType.ANONYMOUS, null);
        when(store.storeUser(anonymousUser))
                .thenReturn(storedUser);
        assertThat(storedUser.id()).isNotNull();
        when(store.getUserById(storedUser.id())).thenReturn(Optional.of(storedUser));

        final var result = service.createAnonymousUserWithEmail("anonymous@example.org");

        assertThat(result).isEqualTo(storedUser);
        verify(store).storeUser(anonymousUser);
        verify(actorHandleService).deleteActorHandleByUserId(storedUser.id());
    }

    @Test
    void deleteUserDelegatesToStoreAndReturnsTrue() {
        final var store = mock(UserStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var service = new UserService(store, actorHandleService);
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        when(store.deleteUser(user)).thenReturn(1);

        final var result = service.deleteUser(user);

        assertThat(result).isTrue();
        verify(actorHandleService).deleteActorHandleByUserId(user.id());
        verify(store).deleteUser(user);
    }

    @Test
    void deleteUserDelegatesToStoreAndReturnsFalse() {
        final var store = mock(UserStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var service = new UserService(store, actorHandleService);
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        when(store.deleteUser(user)).thenReturn(0);

        final var result = service.deleteUser(user);

        assertThat(result).isFalse();
        verify(actorHandleService).deleteActorHandleByUserId(user.id());
        verify(store).deleteUser(user);
    }

    @Test
    void changeUserTypeThrowsWhenUserIdIsNull() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        final var user = createUser(null, "user@example.org", "User", UserRole.USER, UserType.ANONYMOUS, null);

        assertThatThrownBy(() -> service.changeUserType(user, UserType.LOCAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must not be null! Maybe the user is not stored yet?");
    }

    @Test
    void changeUserTypeDelegatesToStoreAndReturnsReloadedUser() {
        final var store = mock(UserStore.class);
        final var service = new UserService(store, mock(ActorHandleService.class));
        final var userId = UUID.randomUUID();
        final var user = createUser(userId, "user@example.org", "User", UserRole.USER, UserType.ANONYMOUS, null);
        final var updatedUser = createUser(userId, "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);
        when(store.getUserById(userId)).thenReturn(Optional.of(updatedUser));

        final var result = service.changeUserType(user, UserType.LOCAL);

        assertThat(result).isEqualTo(updatedUser);
        verify(store).changeUserType(user, UserType.LOCAL);
        verify(store).getUserById(userId);
    }

    @Test
    void isProfileCompleteReturnsTrueForNonBlankName() {
        final var service = new UserService(mock(UserStore.class), mock(ActorHandleService.class));
        final var user = createUser(UUID.randomUUID(), "user@example.org", "User", UserRole.USER, UserType.LOCAL, null);

        final var result = service.isProfileComplete(user);

        assertThat(result).isTrue();
    }

    @Test
    void isProfileCompleteReturnsFalseForBlankName() {
        final var service = new UserService(mock(UserStore.class), mock(ActorHandleService.class));
        final var user = createUser(UUID.randomUUID(), "user@example.org", " ", UserRole.USER, UserType.LOCAL, null);

        final var result = service.isProfileComplete(user);

        assertThat(result).isFalse();
    }

    @SuppressWarnings("SameParameterValue")
    private static UserDto createUser(final UUID id,
                                      final String email,
                                      final String name,
                                      final UserRole role,
                                      final UserType type,
                                      final String handle) {
        return new UserDto(id, null, null, null, handle, email, name, "", null, role, type);
    }
}
