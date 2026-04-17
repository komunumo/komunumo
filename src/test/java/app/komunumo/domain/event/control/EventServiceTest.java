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
package app.komunumo.domain.event.control;

import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.event.entity.EventWithImageDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventServiceTest {

    @Test
    void storeEventDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        when(eventStore.storeEvent(event)).thenReturn(event);

        final var result = eventService.storeEvent(event);

        assertThat(result).isEqualTo(event);
        verify(eventStore).storeEvent(event);
    }

    @Test
    void getEventDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var eventId = UUID.randomUUID();
        final var expected = Optional.of(createEvent());
        when(eventStore.getEvent(eventId)).thenReturn(expected);

        final var result = eventService.getEvent(eventId);

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getEvent(eventId);
    }

    @Test
    void getEventWithImageDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var eventId = UUID.randomUUID();
        final var expected = Optional.of(new EventWithImageDto(createEvent(), null));
        when(eventStore.getEventWithImage(eventId)).thenReturn(expected);

        final var result = eventService.getEventWithImage(eventId);

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getEventWithImage(eventId);
    }

    @Test
    void getEventsDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var expected = List.of(createEvent());
        when(eventStore.getEvents()).thenReturn(expected);

        final var result = eventService.getEvents();

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getEvents();
    }

    @Test
    void getUpcomingEventsWithImageWithoutCommunityDelegatesWithNull() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var expected = List.of(new EventWithImageDto(createEvent(), null));
        when(eventStore.getUpcomingEventsWithImage(null)).thenReturn(expected);

        final var result = eventService.getUpcomingEventsWithImage();

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getUpcomingEventsWithImage(null);
    }

    @Test
    void getUpcomingEventsWithImageWithCommunityDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var community = createCommunity();
        final var expected = List.of(new EventWithImageDto(createEvent(), null));
        when(eventStore.getUpcomingEventsWithImage(community)).thenReturn(expected);

        final var result = eventService.getUpcomingEventsWithImage(community);

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getUpcomingEventsWithImage(community);
    }

    @Test
    void getPastEventsWithImageWithoutCommunityDelegatesWithNull() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var expected = List.of(new EventWithImageDto(createEvent(), null));
        when(eventStore.getPastEventsWithImage(null)).thenReturn(expected);

        final var result = eventService.getPastEventsWithImage();

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getPastEventsWithImage(null);
    }

    @Test
    void getPastEventsWithImageWithCommunityDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var community = createCommunity();
        final var expected = List.of(new EventWithImageDto(createEvent(), null));
        when(eventStore.getPastEventsWithImage(community)).thenReturn(expected);

        final var result = eventService.getPastEventsWithImage(community);

        assertThat(result).isEqualTo(expected);
        verify(eventStore).getPastEventsWithImage(community);
    }

    @Test
    void getEventCountDelegatesToStore() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        when(eventStore.getEventCount()).thenReturn(42);

        final var result = eventService.getEventCount();

        assertThat(result).isEqualTo(42);
        verify(eventStore).getEventCount();
    }

    @Test
    void deleteEventReturnsTrueIfDeleteCountIsPositive() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        when(eventStore.deleteEvent(event)).thenReturn(1);

        final var result = eventService.deleteEvent(event);

        assertThat(result).isTrue();
        verify(eventStore).deleteEvent(event);
    }

    @Test
    void deleteEventReturnsFalseIfDeleteCountIsZero() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        when(eventStore.deleteEvent(event)).thenReturn(0);

        final var result = eventService.deleteEvent(event);

        assertThat(result).isFalse();
        verify(eventStore).deleteEvent(event);
    }

    @Test
    void hasManagementPermissionReturnsTrueForAdminWithoutStoreCall() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        final var admin = createUser(UserRole.ADMIN);

        final var result = eventService.hasManagementPermission(event, admin);

        assertThat(result).isTrue();
        verifyNoInteractions(eventStore);
    }

    @Test
    void hasManagementPermissionDelegatesForNonAdminAndReturnsTrue() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        final var user = createUser(UserRole.USER);
        when(eventStore.hasManagementPermission(event, user)).thenReturn(true);

        final var result = eventService.hasManagementPermission(event, user);

        assertThat(result).isTrue();
        verify(eventStore).hasManagementPermission(event, user);
    }

    @Test
    void hasManagementPermissionDelegatesForNonAdminAndReturnsFalse() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent();
        final var user = createUser(UserRole.USER);
        when(eventStore.hasManagementPermission(event, user)).thenReturn(false);

        final var result = eventService.hasManagementPermission(event, user);

        assertThat(result).isFalse();
        verify(eventStore).hasManagementPermission(event, user);
    }

    @Test
    void isRegistrationAllowedReturnsTrueForPublishedUpcomingEvent() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent(
                ZonedDateTime.now(ZoneOffset.UTC).plusHours(1),
                EventStatus.PUBLISHED);

        final var result = eventService.isRegistrationAllowed(event);

        assertThat(result).isTrue();
        verifyNoInteractions(eventStore);
    }

    @Test
    void isRegistrationAllowedReturnsFalseForEventWithoutBegin() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent(null, EventStatus.PUBLISHED);

        final var result = eventService.isRegistrationAllowed(event);

        assertThat(result).isFalse();
        verifyNoInteractions(eventStore);
    }

    @Test
    void isRegistrationAllowedReturnsFalseForNonPublishedEvent() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent(
                ZonedDateTime.now(ZoneOffset.UTC).plusHours(1),
                EventStatus.DRAFT);

        final var result = eventService.isRegistrationAllowed(event);

        assertThat(result).isFalse();
        verifyNoInteractions(eventStore);
    }

    @Test
    void isRegistrationAllowedReturnsFalseForPastEvent() {
        final var eventStore = mock(EventStore.class);
        final var eventService = new EventService(eventStore);
        final var event = createEvent(
                ZonedDateTime.now(ZoneOffset.UTC).minusHours(1),
                EventStatus.PUBLISHED);

        final var result = eventService.isRegistrationAllowed(event);

        assertThat(result).isFalse();
        verifyNoInteractions(eventStore);
    }

    private static EventDto createEvent() {
        return createEvent(null, EventStatus.PUBLISHED);
    }

    private static EventDto createEvent(final ZonedDateTime begin,
                                        final EventStatus status) {
        return new EventDto(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "Test Event", "Description", "Location", begin, null,
                null, true, EventVisibility.PUBLIC, status);
    }

    private static CommunityDto createCommunity() {
        return new CommunityDto(UUID.randomUUID(), "@community", null, null,
                "Test Community", "Description", null);
    }

    private static UserDto createUser(final UserRole role) {
        return new UserDto(UUID.randomUUID(), null, null, "@user", "test@example.com",
                "Test User", "Bio", null, role, UserType.LOCAL);
    }
}
