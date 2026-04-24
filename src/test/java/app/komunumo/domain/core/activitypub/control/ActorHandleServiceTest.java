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
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActorHandleServiceTest {

    @Test
    void storeActorHandleDelegatesToStoreForUserReference() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = createActorHandleForUser("alice_123");
        when(actorHandleStore.storeActorHandle(actorHandle)).thenReturn(actorHandle);

        final var result = actorHandleService.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(actorHandleStore).storeActorHandle(actorHandle);
    }

    @Test
    void storeActorHandleDelegatesToStoreForCommunityReference() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = createActorHandleForCommunity();
        when(actorHandleStore.storeActorHandle(actorHandle)).thenReturn(actorHandle);

        final var result = actorHandleService.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(actorHandleStore).storeActorHandle(actorHandle);
    }

    @Test
    void storeActorHandleThrowsForBlankHandle() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto(" ", UUID.randomUUID(), null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Actor handle must not be blank.");
    }

    @Test
    void storeActorHandleThrowsWhenBothReferencesAreSet() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("validHandle", UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly one of userId or communityId must be set.");
    }

    @Test
    void storeActorHandleThrowsWhenNoReferenceIsSet() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("validHandle", null, null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly one of userId or communityId must be set.");
    }

    @Test
    void storeActorHandleThrowsForHandleWithInvalidCharacters() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("invalid-handle", UUID.randomUUID(), null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Actor handle contains invalid characters.");
    }

    @Test
    void storeActorHandleThrowsForHandleThatIsTooShort() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("ab", UUID.randomUUID(), null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Actor handle length must be between 3 and 30 characters.");
    }

    @Test
    void storeActorHandleThrowsForHandleThatIsTooLong() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("a".repeat(ActorHandleService.HANDLE_MAX_LENGTH + 1),
                UUID.randomUUID(), null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Actor handle length must be between 3 and 30 characters.");
    }

    @Test
    void storeActorHandleAcceptsHandleWithMinimumLength() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("a".repeat(ActorHandleService.HANDLE_MIN_LENGTH),
                UUID.randomUUID(), null);
        when(actorHandleStore.storeActorHandle(actorHandle)).thenReturn(actorHandle);

        final var result = actorHandleService.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(actorHandleStore).storeActorHandle(actorHandle);
    }

    @Test
    void storeActorHandleAcceptsHandleWithMaximumLength() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("a".repeat(ActorHandleService.HANDLE_MAX_LENGTH),
                UUID.randomUUID(), null);
        when(actorHandleStore.storeActorHandle(actorHandle)).thenReturn(actorHandle);

        final var result = actorHandleService.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(actorHandleStore).storeActorHandle(actorHandle);
    }

    @Test
    void getActorHandleDelegatesToStore() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var handle = "@alice@example.org";
        final var expected = Optional.of(createActorHandleForUser(handle));
        when(actorHandleStore.getActorHandle(handle)).thenReturn(expected);

        final var result = actorHandleService.getActorHandle(handle);

        assertThat(result).isEqualTo(expected);
        verify(actorHandleStore).getActorHandle(handle);
    }

    @Test
    void isHandleAvailableReturnsTrueIfNoActorHandleExists() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var handle = "alice";
        when(actorHandleStore.getActorHandle(handle)).thenReturn(Optional.empty());

        final var result = actorHandleService.isHandleAvailable(handle);

        assertThat(result).isTrue();
        verify(actorHandleStore).getActorHandle(handle);
    }

    @Test
    void isHandleAvailableReturnsFalseIfActorHandleExists() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var handle = "alice";
        when(actorHandleStore.getActorHandle(handle)).thenReturn(Optional.of(createActorHandleForUser(handle)));

        final var result = actorHandleService.isHandleAvailable(handle);

        assertThat(result).isFalse();
        verify(actorHandleStore).getActorHandle(handle);
    }

    @Test
    void isHandleAvailableReturnsTrueIfHandleAlreadyBelongsToUser() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var handle = "alice";
        final var userId = UUID.randomUUID();
        when(actorHandleStore.getActorHandle(handle)).thenReturn(Optional.of(new ActorHandleDto(handle, userId, null)));

        final var result = actorHandleService.isHandleAvailable(handle, userId);

        assertThat(result).isTrue();
        verify(actorHandleStore).getActorHandle(handle);
    }

    @Test
    void isHandleAvailableReturnsFalseIfHandleBelongsToDifferentUser() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var handle = "alice";
        final var userId = UUID.randomUUID();
        when(actorHandleStore.getActorHandle(handle)).thenReturn(Optional.of(createActorHandleForUser(handle)));

        final var result = actorHandleService.isHandleAvailable(handle, userId);

        assertThat(result).isFalse();
        verify(actorHandleStore).getActorHandle(handle);
    }

    @Test
    void deleteActorHandleByCommunityIdReturnsTrueIfDeleteCountIsPositive() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var communityId = UUID.randomUUID();
        when(actorHandleStore.deleteActorHandleByCommunityId(communityId)).thenReturn(1);

        final var result = actorHandleService.deleteActorHandleByCommunityId(communityId);

        assertThat(result).isTrue();
        verify(actorHandleStore).deleteActorHandleByCommunityId(communityId);
    }

    @Test
    void deleteActorHandleByCommunityIdReturnsFalseIfDeleteCountIsZero() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var communityId = UUID.randomUUID();
        when(actorHandleStore.deleteActorHandleByCommunityId(communityId)).thenReturn(0);

        final var result = actorHandleService.deleteActorHandleByCommunityId(communityId);

        assertThat(result).isFalse();
        verify(actorHandleStore).deleteActorHandleByCommunityId(communityId);
    }

    private static ActorHandleDto createActorHandleForUser(final String handle) {
        return new ActorHandleDto(handle, UUID.randomUUID(), null);
    }

    private static ActorHandleDto createActorHandleForCommunity() {
        return new ActorHandleDto("testHandle", null, UUID.randomUUID());
    }
}
