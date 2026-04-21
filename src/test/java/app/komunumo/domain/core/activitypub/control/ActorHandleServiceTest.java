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

import java.util.List;
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
        final var actorHandle = createActorHandleForUser("@alice@example.org");
        when(actorHandleStore.storeActorHandle(actorHandle)).thenReturn(actorHandle);

        final var result = actorHandleService.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(actorHandleStore).storeActorHandle(actorHandle);
    }

    @Test
    void storeActorHandleDelegatesToStoreForCommunityReference() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = createActorHandleForCommunity("@community@example.org");
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
        final var actorHandle = new ActorHandleDto("@invalid@example.org", UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly one of userId or communityId must be set.");
    }

    @Test
    void storeActorHandleThrowsWhenNoReferenceIsSet() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = new ActorHandleDto("@invalid@example.org", null, null);

        assertThatThrownBy(() -> actorHandleService.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly one of userId or communityId must be set.");
    }

    @Test
    void getActorHandlesDelegatesToStore() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var expected = List.of(createActorHandleForUser("@alice@example.org"));
        when(actorHandleStore.getActorHandles()).thenReturn(expected);

        final var result = actorHandleService.getActorHandles();

        assertThat(result).isEqualTo(expected);
        verify(actorHandleStore).getActorHandles();
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
    void getActorHandleByUserIdDelegatesToStore() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var userId = UUID.randomUUID();
        final var expected = Optional.of(new ActorHandleDto("@alice@example.org", userId, null));
        when(actorHandleStore.getActorHandleByUserId(userId)).thenReturn(expected);

        final var result = actorHandleService.getActorHandleByUserId(userId);

        assertThat(result).isEqualTo(expected);
        verify(actorHandleStore).getActorHandleByUserId(userId);
    }

    @Test
    void getActorHandleByCommunityIdDelegatesToStore() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var communityId = UUID.randomUUID();
        final var expected = Optional.of(new ActorHandleDto("@community@example.org", null, communityId));
        when(actorHandleStore.getActorHandleByCommunityId(communityId)).thenReturn(expected);

        final var result = actorHandleService.getActorHandleByCommunityId(communityId);

        assertThat(result).isEqualTo(expected);
        verify(actorHandleStore).getActorHandleByCommunityId(communityId);
    }

    @Test
    void deleteActorHandleReturnsTrueIfDeleteCountIsPositive() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = createActorHandleForCommunity("@community@example.org");
        when(actorHandleStore.deleteActorHandle(actorHandle)).thenReturn(1);

        final var result = actorHandleService.deleteActorHandle(actorHandle);

        assertThat(result).isTrue();
        verify(actorHandleStore).deleteActorHandle(actorHandle);
    }

    @Test
    void deleteActorHandleReturnsFalseIfDeleteCountIsZero() {
        final var actorHandleStore = mock(ActorHandleStore.class);
        final var actorHandleService = new ActorHandleService(actorHandleStore);
        final var actorHandle = createActorHandleForCommunity("@community@example.org");
        when(actorHandleStore.deleteActorHandle(actorHandle)).thenReturn(0);

        final var result = actorHandleService.deleteActorHandle(actorHandle);

        assertThat(result).isFalse();
        verify(actorHandleStore).deleteActorHandle(actorHandle);
    }

    private static ActorHandleDto createActorHandleForUser(final String handle) {
        return new ActorHandleDto(handle, UUID.randomUUID(), null);
    }

    private static ActorHandleDto createActorHandleForCommunity(final String handle) {
        return new ActorHandleDto(handle, null, UUID.randomUUID());
    }
}
