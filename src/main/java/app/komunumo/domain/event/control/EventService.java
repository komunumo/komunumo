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
import app.komunumo.domain.event.entity.EventWithImageDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides event-related business operations and delegates persistence to {@link EventStore}.</p>
 *
 * <p>This service forms the control-layer API for event use cases while keeping database
 * access details encapsulated in the store implementation.</p>
 */
@Service
public final class EventService {

    /**
     * <p>Store responsible for all event persistence operations.</p>
     */
    private final @NotNull EventStore eventStore;

    /**
     * <p>Creates a new event service.</p>
     *
     * @param eventStore the store used for event persistence access
     */
    EventService(final @NotNull EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * <p>Creates or updates an event.</p>
     *
     * @param event the event data to persist
     * @return the persisted event
     */
    public @NotNull EventDto storeEvent(final @NotNull EventDto event) {
        return eventStore.storeEvent(event);
    }

    /**
     * <p>Loads an event by its unique identifier.</p>
     *
     * @param id the event ID
     * @return an optional containing the event if found; otherwise empty
     */
    public @NotNull Optional<EventDto> getEvent(final @NotNull UUID id) {
        return eventStore.getEvent(id);
    }

    /**
     * <p>Loads an event by ID including optional image data.</p>
     *
     * @param id the event ID
     * @return an optional containing the event and image projection if found; otherwise empty
     */
    public @NotNull Optional<EventWithImageDto> getEventWithImage(final @NotNull UUID id) {
        return eventStore.getEventWithImage(id);
    }

    /**
     * <p>Loads all events.</p>
     *
     * @return all events
     */
    public @NotNull List<@NotNull EventDto> getEvents() {
        return eventStore.getEvents();
    }

    /**
     * <p>Loads upcoming public events with image data.</p>
     *
     * @return upcoming events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getUpcomingEventsWithImage() {
        return getUpcomingEventsWithImage(null);
    }

    /**
     * <p>Loads upcoming public events with image data, optionally filtered by community.</p>
     *
     * @param community the community filter; if {@code null}, events from all communities are returned
     * @return upcoming events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getUpcomingEventsWithImage(final @Nullable CommunityDto community) {
        return eventStore.getUpcomingEventsWithImage(community);
    }

    /**
     * <p>Loads past public events with image data.</p>
     *
     * @return past events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getPastEventsWithImage() {
        return getPastEventsWithImage(null);
    }

    /**
     * <p>Loads past public events with image data, optionally filtered by community.</p>
     *
     * @param community the community filter; if {@code null}, events from all communities are returned
     * @return past events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getPastEventsWithImage(final @Nullable CommunityDto community) {
        return eventStore.getPastEventsWithImage(community);
    }

    /**
     * <p>Counts all persisted events.</p>
     *
     * @return the total number of events; never negative
     */
    public int getEventCount() {
        return eventStore.getEventCount();
    }

    /**
     * <p>Deletes the given event.</p>
     *
     * @param event the event to delete
     * @return {@code true} if the event was deleted; otherwise {@code false}
     */
    public boolean deleteEvent(final @NotNull EventDto event) {
        return eventStore.deleteEvent(event) > 0;
    }

    /**
     * <p>Checks whether the given user has management permission for the given event.</p>
     *
     * <p>Administrators always have permission. Other users need manager permissions
     * on the event's community.</p>
     *
     * @param event the event to check against
     * @param user the user to check
     * @return {@code true} if the user can manage the event; otherwise {@code false}
     */
    public boolean hasManagementPermission(final @NotNull EventDto event, final @NotNull UserDto user) {
        if (user.role() == UserRole.ADMIN) {
            return true;
        }

        return eventStore.hasManagementPermission(event, user);
    }
}
