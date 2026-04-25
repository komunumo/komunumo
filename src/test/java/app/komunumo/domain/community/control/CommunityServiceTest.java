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
import app.komunumo.domain.user.entity.UserType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommunityServiceTest {

    @Test
    void storeCommunityDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var communityService = new CommunityService(communityStore, actorHandleService);
        final var community = createCommunity();
        when(communityStore.storeCommunity(community)).thenReturn(community.id());
        assertThat(community.id()).isNotNull();
        when(communityStore.getCommunity(community.id())).thenReturn(Optional.of(community));

        final var result = communityService.storeCommunity(community);

        assertThat(result).isEqualTo(community);
        verify(communityStore).storeCommunity(community);
        verify(actorHandleService).storeActorHandle(new ActorHandleDto(community.handle(), null, community.id()));
    }

    @Test
    void getCommunityDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var id = UUID.randomUUID();
        final var expected = Optional.of(createCommunity());
        when(communityStore.getCommunity(id)).thenReturn(expected);

        final var result = communityService.getCommunity(id);

        assertThat(result).isEqualTo(expected);
        verify(communityStore).getCommunity(id);
    }

    @Test
    void getCommunityWithImageDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var handle = "community-handle";
        final var expected = Optional.of(new CommunityWithImageDto(createCommunity(), null));
        when(communityStore.getCommunityWithImage(handle)).thenReturn(expected);

        final var result = communityService.getCommunityWithImage(handle);

        assertThat(result).isEqualTo(expected);
        verify(communityStore).getCommunityWithImage(handle);
    }

    @Test
    void getCommunitiesDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var expected = List.of(createCommunity());
        when(communityStore.getCommunities()).thenReturn(expected);

        final var result = communityService.getCommunities();

        assertThat(result).isEqualTo(expected);
        verify(communityStore).getCommunities();
    }

    @Test
    void getCommunitiesWithImageDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var expected = List.of(new CommunityWithImageDto(createCommunity(), null));
        when(communityStore.getCommunitiesWithImage()).thenReturn(expected);

        final var result = communityService.getCommunitiesWithImage();

        assertThat(result).isEqualTo(expected);
        verify(communityStore).getCommunitiesWithImage();
    }

    @Test
    void getCommunitiesForManagerDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var user = createUser();
        final var expected = List.of(createCommunity());
        when(communityStore.getCommunitiesForManager(user)).thenReturn(expected);

        final var result = communityService.getCommunitiesForManager(user);

        assertThat(result).isEqualTo(expected);
        verify(communityStore).getCommunitiesForManager(user);
    }

    @Test
    void getCommunityCountDelegatesToStore() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        when(communityStore.getCommunityCount()).thenReturn(42);

        final var result = communityService.getCommunityCount();

        assertThat(result).isEqualTo(42);
        verify(communityStore).getCommunityCount();
    }

    @Test
    void deleteCommunityReturnsTrueIfDeleteCountIsPositive() {
        final var communityStore = mock(CommunityStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var communityService = new CommunityService(communityStore, actorHandleService);
        final var community = createCommunity();
        when(communityStore.deleteCommunity(community)).thenReturn(1);

        final var result = communityService.deleteCommunity(community);

        assertThat(result).isTrue();
        verify(actorHandleService).deleteActorHandleByCommunityId(community.id());
        verify(communityStore).deleteCommunity(community);
    }

    @Test
    void deleteCommunityReturnsFalseIfDeleteCountIsZero() {
        final var communityStore = mock(CommunityStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var communityService = new CommunityService(communityStore, actorHandleService);
        final var community = createCommunity();
        when(communityStore.deleteCommunity(community)).thenReturn(0);

        final var result = communityService.deleteCommunity(community);

        assertThat(result).isFalse();
        verify(actorHandleService).deleteActorHandleByCommunityId(community.id());
        verify(communityStore).deleteCommunity(community);
    }

    @Test
    void deleteCommunityThrowsIllegalArgumentExceptionIfCommunityIdIsNull() {
        final var communityStore = mock(CommunityStore.class);
        final var actorHandleService = mock(ActorHandleService.class);
        final var communityService = new CommunityService(communityStore, actorHandleService);
        final var communityWithoutId = new CommunityDto(null, "test", null, null,
                "Test Community", "Test Description", null);

        assertThatThrownBy(() -> communityService.deleteCommunity(communityWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Community ID must not be null! Maybe the community is not stored yet?");

        verifyNoInteractions(actorHandleService, communityStore);
    }

    @Test
    void isCommunityManagerReturnsTrueIfUserHasManagerCommunity() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var user = createUser();
        when(communityStore.getCommunitiesForManager(user)).thenReturn(List.of(createCommunity()));

        final var result = communityService.isCommunityManager(user);

        assertThat(result).isTrue();
        verify(communityStore).getCommunitiesForManager(user);
    }

    @Test
    void isCommunityManagerReturnsFalseIfUserHasNoManagerCommunity() {
        final var communityStore = mock(CommunityStore.class);
        final var communityService = new CommunityService(communityStore, mock(ActorHandleService.class));
        final var user = createUser();
        when(communityStore.getCommunitiesForManager(user)).thenReturn(List.of());

        final var result = communityService.isCommunityManager(user);

        assertThat(result).isFalse();
        verify(communityStore).getCommunitiesForManager(user);
    }

    private static CommunityDto createCommunity() {
        return new CommunityDto(UUID.randomUUID(), "@test", null, null,
                "Test Community", "Test Description", null);
    }

    private static UserDto createUser() {
        return new UserDto(UUID.randomUUID(), null, null, "@user", "test@example.com",
                "Test User", "Bio", null, UserRole.USER, UserType.LOCAL);
    }
}
