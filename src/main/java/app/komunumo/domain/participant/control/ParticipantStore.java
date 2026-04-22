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
package app.komunumo.domain.participant.control;

import app.komunumo.data.db.tables.records.ParticipantRecord;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static app.komunumo.data.db.tables.Member.MEMBER;
import static app.komunumo.data.db.tables.Participant.PARTICIPANT;
import static app.komunumo.data.db.tables.User.USER;
import static app.komunumo.data.db.tables.ActorHandle.ACTOR_HANDLE;

/**
 * <p>Handles persistence operations for event participation and related read projections.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating, loading, counting,
 * deleting, and manager-recipient lookups for participant relations.</p>
 */
@Service
final class ParticipantStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new participant store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     */
    ParticipantStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Stores or updates a participant relation.</p>
     *
     * @param participant the participant relation to persist
     */
    public void storeParticipant(final @NotNull ParticipantDto participant) {
        final ParticipantRecord participantRecord = dsl.fetchOptional(PARTICIPANT,
                        PARTICIPANT.EVENT_ID.eq(participant.eventId())
                                .and(PARTICIPANT.USER_ID.eq(participant.userId())))
                .orElse(dsl.newRecord(PARTICIPANT));
        participantRecord.from(participant);

        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        if (participantRecord.getRegistered() == null) { // NOSONAR (false positive)
            participantRecord.setRegistered(now);
        }
        participantRecord.store();
    }

    /**
     * <p>Loads all participants.</p>
     *
     * @return all persisted participants
     */
    public @NotNull List<@NotNull ParticipantDto> getAllParticipants() {
        return dsl.selectFrom(PARTICIPANT)
                .fetchInto(ParticipantDto.class);
    }

    /**
     * <p>Loads a participant relation by event and user.</p>
     *
     * @param event the event context of the participant
     * @param user the user context of the participant
     * @return an optional containing the participant relation if found; otherwise empty
     */
    public @NotNull Optional<ParticipantDto> getParticipant(final @NotNull EventDto event,
                                                            final @NotNull UserDto user) {
        return dsl.selectFrom(PARTICIPANT)
                .where(PARTICIPANT.EVENT_ID.eq(event.id())
                        .and(PARTICIPANT.USER_ID.eq(user.id())))
                .fetchOptionalInto(ParticipantDto.class);
    }

    /**
     * <p>Deletes a participant relation.</p>
     *
     * @param participant the participant relation to delete
     * @return the number of deleted rows
     */
    public int deleteParticipant(final @NotNull ParticipantDto participant) {
        return dsl.delete(PARTICIPANT)
                .where(PARTICIPANT.EVENT_ID.eq(participant.eventId())
                        .and(PARTICIPANT.USER_ID.eq(participant.userId())))
                .execute();
    }

    /**
     * <p>Counts the total number of participants.</p>
     *
     * @return the total count of participants; never negative
     */
    public int getParticipantCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(PARTICIPANT)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Counts the total number of participants for the specified event.</p>
     *
     * @param event the event for which the participants should be counted
     * @return the total count of participants for the event; never negative
     */
    public int getParticipantCount(final @NotNull EventDto event) {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(PARTICIPANT)
                        .where(PARTICIPANT.EVENT_ID.eq(event.id()))
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Checks whether a user is participant of an event.</p>
     *
     * @param user the user to check
     * @param event the event to check
     * @return {@code true} if the user participates in the event; otherwise {@code false}
     */
    public boolean isParticipant(final @NotNull UserDto user,
                                 final @NotNull EventDto event) {
        return dsl.fetchExists(dsl.selectFrom(PARTICIPANT)
                .where(PARTICIPANT.USER_ID.eq(user.id())
                        .and(PARTICIPANT.EVENT_ID.eq(event.id()))));
    }

    /**
     * <p>Loads all participants of an event including user information and registration timestamp.</p>
     *
     * @param event the event whose participants should be loaded
     * @return ordered participants with user projection and registration timestamp
     */
    public @NotNull List<@NotNull RegisteredParticipantDto> getParticipants(final @NotNull EventDto event) {
        return dsl.select(USER.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .select(PARTICIPANT.REGISTERED)
                .from(PARTICIPANT)
                .join(USER).on(PARTICIPANT.USER_ID.eq(USER.ID))
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.USER_ID.eq(USER.ID))
                .where(PARTICIPANT.EVENT_ID.eq(event.id()))
                .orderBy(PARTICIPANT.REGISTERED.asc())
                .fetch(record -> new RegisteredParticipantDto(
                        new UserDto(
                                record.get(USER.ID),
                                record.get(USER.CREATED),
                                record.get(USER.UPDATED),
                                record.get(ACTOR_HANDLE.HANDLE),
                                record.get(USER.EMAIL),
                                record.get(USER.NAME),
                                record.get(USER.BIO),
                                record.get(USER.IMAGE_ID),
                                record.get(USER.ROLE, UserRole.class),
                                record.get(USER.TYPE, UserType.class)
                        ),
                        record.get(PARTICIPANT.REGISTERED, ZonedDateTime.class)
                ));
    }

    /**
     * <p>Loads distinct manager email addresses for an event's community.</p>
     *
     * <p>Managers are users with role {@code OWNER} or {@code ORGANIZER}.</p>
     *
     * @param event the event whose manager recipients should be loaded
     * @return distinct non-null manager email addresses
     */
    public @NotNull List<@NotNull String> getManagerEmailsForEvent(final @NotNull EventDto event) {
        return dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .distinct()
                .toList();
    }
}
