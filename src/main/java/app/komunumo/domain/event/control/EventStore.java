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

import app.komunumo.data.db.tables.Image;
import app.komunumo.data.db.tables.records.EventRecord;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.event.entity.EventWithImageDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.persistence.jooq.AbstractStore;
import app.komunumo.infra.persistence.jooq.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.tables.Community.COMMUNITY;
import static app.komunumo.data.db.tables.Event.EVENT;
import static app.komunumo.data.db.tables.Image.IMAGE;
import static app.komunumo.data.db.tables.Member.MEMBER;
import static org.jooq.impl.DSL.noCondition;

/**
 * <p>Handles persistence operations for events and related read projections.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating, loading,
 * counting, deleting, and permission checks for events.</p>
 */
@Service
final class EventStore extends AbstractStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new event store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     * @param idGenerator the unique ID generator used by {@link AbstractStore}
     */
    EventStore(final @NotNull DSLContext dsl,
               final @NotNull UniqueIdGenerator idGenerator) {
        super(idGenerator);
        this.dsl = dsl;
    }

    /**
     * <p>Stores an event in the database.</p>
     *
     * <p>If an event with the same ID already exists, it is updated.
     * Otherwise, a new record is created.</p>
     *
     * @param event a DTO representation of the event information
     * @return the persisted event information in DTO form
     */
    public @NotNull EventDto storeEvent(final @NotNull EventDto event) {
        final EventRecord eventRecord = dsl.fetchOptional(EVENT, EVENT.ID.eq(event.id()))
                .orElse(dsl.newRecord(EVENT));
        createOrUpdate(EVENT, event, eventRecord);
        return eventRecord.into(EventDto.class);
    }

    /**
     * <p>Loads an event by its unique identifier.</p>
     *
     * @param id the event ID
     * @return an optional containing the event if found; otherwise empty
     */
    public @NotNull Optional<EventDto> getEvent(final @NotNull UUID id) {
        return dsl.selectFrom(EVENT)
                .where(EVENT.ID.eq(id))
                .fetchOptionalInto(EventDto.class);
    }

    /**
     * <p>Loads a public event by ID and includes optional image data.</p>
     *
     * @param id the event ID
     * @return an optional containing the event and image projection if found; otherwise empty
     */
    public @NotNull Optional<EventWithImageDto> getEventWithImage(final @NotNull UUID id) {
        final var communityImage = IMAGE.as("COMMUNITY_IMAGE");
        return dsl.select()
                .from(EVENT)
                .leftJoin(IMAGE).on(EVENT.IMAGE_ID.eq(IMAGE.ID))
                .leftJoin(COMMUNITY).on(EVENT.COMMUNITY_ID.eq(COMMUNITY.ID))
                .leftJoin(communityImage).on(COMMUNITY.IMAGE_ID.eq(communityImage.ID))
                .where(EVENT.ID.eq(id)
                        .and(EVENT.VISIBILITY.eq(EventVisibility.PUBLIC))
                        .and(EVENT.STATUS.in(EventStatus.PUBLISHED, EventStatus.CANCELED)))
                .fetchOptional(record -> mapRecordToEventWithImage(record, communityImage));
    }

    /**
     * <p>Loads all events.</p>
     *
     * @return all events
     */
    public @NotNull List<@NotNull EventDto> getEvents() {
        return dsl.selectFrom(EVENT)
                .fetchInto(EventDto.class);
    }

    /**
     * <p>Loads upcoming public events with image data, optionally filtered by community.</p>
     *
     * @param community the community filter; if {@code null}, events from all communities are returned
     * @return upcoming events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getUpcomingEventsWithImage(final @Nullable CommunityDto community) {
        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        final var communityImage = IMAGE.as("COMMUNITY_IMAGE");
        return dsl.select()
                .from(EVENT)
                .leftJoin(IMAGE).on(EVENT.IMAGE_ID.eq(IMAGE.ID))
                .leftJoin(COMMUNITY).on(EVENT.COMMUNITY_ID.eq(COMMUNITY.ID))
                .leftJoin(communityImage).on(COMMUNITY.IMAGE_ID.eq(communityImage.ID))
                .where(
                        EVENT.BEGIN.isNotNull()
                                .and(EVENT.END.isNotNull())
                                .and(EVENT.END.gt(now))
                                .and(EVENT.VISIBILITY.eq(EventVisibility.PUBLIC))
                                .and(EVENT.STATUS.in(EventStatus.PUBLISHED, EventStatus.CANCELED))
                                .and(community != null ? EVENT.COMMUNITY_ID.eq(community.id()) : noCondition()))
                .orderBy(EVENT.BEGIN.asc())
                .fetch(record -> mapRecordToEventWithImage(record, communityImage));
    }

    /**
     * <p>Loads past public events with image data, optionally filtered by community.</p>
     *
     * @param community the community filter; if {@code null}, events from all communities are returned
     * @return past events with optional image projection
     */
    public @NotNull List<@NotNull EventWithImageDto> getPastEventsWithImage(final @Nullable CommunityDto community) {
        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        final var communityImage = IMAGE.as("COMMUNITY_IMAGE");
        return dsl.select()
                .from(EVENT)
                .leftJoin(IMAGE).on(EVENT.IMAGE_ID.eq(IMAGE.ID))
                .leftJoin(COMMUNITY).on(EVENT.COMMUNITY_ID.eq(COMMUNITY.ID))
                .leftJoin(communityImage).on(COMMUNITY.IMAGE_ID.eq(communityImage.ID))
                .where(
                        EVENT.BEGIN.isNotNull()
                                .and(EVENT.END.isNotNull())
                                .and(EVENT.END.lt(now))
                                .and(EVENT.VISIBILITY.eq(EventVisibility.PUBLIC))
                                .and(EVENT.STATUS.in(EventStatus.PUBLISHED, EventStatus.CANCELED))
                                .and(community != null ? EVENT.COMMUNITY_ID.eq(community.id()) : noCondition()))
                .orderBy(EVENT.BEGIN.desc())
                .fetch(record -> mapRecordToEventWithImage(record, communityImage));
    }

    /**
     * <p>Counts all persisted events.</p>
     *
     * @return the total count of events; never negative
     */
    public int getEventCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(EVENT)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Deletes the given event.</p>
     *
     * @param event the event to delete
     * @return the number of deleted event rows (typically {@code 0} or {@code 1})
     */
    public int deleteEvent(final @NotNull EventDto event) {
        return dsl.delete(EVENT)
                .where(EVENT.ID.eq(event.id()))
                .execute();
    }

    /**
     * <p>Checks whether the given user has management permission for the given event.</p>
     *
     * <p>Users need manager permissions ({@code OWNER} or {@code ORGANIZER})
     * on the event's community.</p>
     *
     * @param event the event to check against
     * @param user the user to check
     * @return {@code true} if the user can manage the event; otherwise {@code false}
     */
    public boolean hasManagementPermission(final @NotNull EventDto event, final @NotNull UserDto user) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(EVENT)
                        .join(MEMBER).on(MEMBER.COMMUNITY_ID.eq(EVENT.COMMUNITY_ID))
                        .where(EVENT.ID.eq(event.id()))
                        .and(MEMBER.USER_ID.eq(user.id()))
                        .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
        );
    }

    /**
     * <p>Maps a joined database record to an {@link EventWithImageDto}.</p>
     *
     * <p>If the event has no own image, the community image is used as fallback.</p>
     *
     * @param record the joined database record
     * @param communityImage the aliased community image table used for fallback image data
     * @return the mapped event projection with optional/fallback image
     */
    private @NotNull EventWithImageDto mapRecordToEventWithImage(final @NotNull Record record,
                                                                 final @NotNull Image communityImage) {
        final ImageDto image;
        if (record.get(IMAGE.ID) != null) {
            image = new ImageDto(
                    record.get(IMAGE.ID, UUID.class),
                    record.get(IMAGE.CONTENT_TYPE, ContentType.class)
            );
        } else if (record.get(communityImage.ID) != null) {
            image = new ImageDto(
                    record.get(communityImage.ID, UUID.class),
                    record.get(communityImage.CONTENT_TYPE, ContentType.class)
            );
        } else {
            image = null;
        }

        final var event = record.into(EVENT).into(EventDto.class);
        return new EventWithImageDto(event, image);
    }
}
