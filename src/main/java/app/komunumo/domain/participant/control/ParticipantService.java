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
import app.komunumo.domain.core.confirmation.control.ConfirmationHandler;
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationContext;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.ui.i18n.TranslationProvider;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static app.komunumo.data.db.tables.Member.MEMBER;
import static app.komunumo.data.db.tables.Participant.PARTICIPANT;
import static app.komunumo.data.db.tables.User.USER;

@Service
public final class ParticipantService {

    @VisibleForTesting
    static final @NotNull String CONTEXT_KEY_EVENT = "event";

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

    private final @NotNull DSLContext dsl;
    private final @NotNull MailService mailService;
    private final @NotNull UserService userService;
    private final @NotNull LoginService loginService;
    private final @NotNull ConfirmationService confirmationService;
    private final @NotNull TranslationProvider translationProvider;

    public ParticipantService(final @NotNull DSLContext dsl,
                              final @NotNull MailService mailService,
                              final @NotNull UserService userService,
                              final @NotNull LoginService loginService,
                              final @NotNull ConfirmationService confirmationService,
                              final @NotNull TranslationProvider translationProvider) {
        super();
        this.dsl = dsl;
        this.mailService = mailService;
        this.userService = userService;
        this.loginService = loginService;
        this.confirmationService = confirmationService;
        this.translationProvider = translationProvider;
    }

    public void startConfirmationProcess(final @NotNull EventDto event,
                                         final @NotNull Locale locale) {
        final var actionMessage = translationProvider.getTranslation(
                "participant.control.ParticipantService.actionText", locale, event.title());
        final ConfirmationHandler actionHandler = this::handleConfirmationResponse;
        final var actionContext = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var confirmationRequest = new ConfirmationRequest(
                actionMessage,
                actionHandler,
                actionContext,
                locale
        );
        confirmationService.startConfirmationProcess(confirmationRequest);
    }

    @VisibleForTesting
    @NotNull ConfirmationResponse handleConfirmationResponse(final @NotNull String email,
                                                             final @NotNull ConfirmationContext context,
                                                             final @NotNull Locale locale) {
        final var event = (EventDto) context.get(CONTEXT_KEY_EVENT);
        final var user = userService.getUserByEmail(email)
                .orElseGet(() -> userService.createAnonymousUserWithEmail(email));

        final var eventTitle = event.title();
        final var eventLink = LinkUtil.getLink(event);

        if (registerForEvent(event, user, locale)) {
            final var status = ConfirmationStatus.SUCCESS;
            final var message = translationProvider.getTranslation(
                    "participant.control.ParticipantService.registrationSuccessMessage", locale, eventTitle);
            return new ConfirmationResponse(status, message, eventLink);
        } else {
            final var status = ConfirmationStatus.WARNING;
            final var message = translationProvider.getTranslation(
                    "participant.control.ParticipantService.registrationFailedMessage", locale, eventTitle);
            return new ConfirmationResponse(status, message, eventLink);
        }
    }

    public boolean registerForEvent(final @NotNull EventDto event,
                                             final @NotNull UserDto user,
                                             final @NotNull Locale locale) {
        if (event.id() == null) {
            LOGGER.warn("Attempted to register for an event where the event ID is NULL. Event: {}", event);
            return false;
        }

        if (user.id() == null) {
            LOGGER.warn("Attempted to register for event where the user ID is NULL. User: {}", user);
            return false;
        }

        final var participant = getParticipant(event, user) // try to get existing participant
                .orElseGet(() -> new ParticipantDto(event.id(), user.id(), null));
        storeParticipant(participant);

        final var eventTitle = event.title();
        final var eventLink = LinkUtil.getLink(event);

        final var email = user.email();
        if (email != null && !email.isBlank()) {
            final Map<String, String> mailVariables = Map.of("eventTitle", eventTitle, "eventLink", eventLink);
            mailService.sendMail(MailTemplateId.EVENT_REGISTRATION_SUCCESS, locale, MailFormat.MARKDOWN,
                    mailVariables, email);
        }

        notifyEventManagersAboutParticipationChange(event, user, locale,
                MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS);

        return true;
    }

    private void notifyEventManagersAboutParticipationChange(final @NotNull EventDto event,
                                                             final @NotNull UserDto user,
                                                             final @NotNull Locale locale,
                                                             final @NotNull MailTemplateId mailTemplateId) {
        final var recipientEmails = dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .distinct()
                .toArray(String[]::new);

        final Map<String, String> mailVariables = Map.of(
                "eventTitle", event.title(),
                "participantName", resolveParticipantName(user, locale),
                "participantCount", Integer.toString(getParticipantCount(event))
        );
        mailService.sendMail(mailTemplateId, locale, MailFormat.MARKDOWN, mailVariables, recipientEmails);
    }

    private @NotNull String resolveParticipantName(final @NotNull UserDto user,
                                                   final @NotNull Locale locale) {
        if (!user.name().isBlank()) {
            return user.name();
        }
        return translationProvider.getTranslation("participant.control.ParticipantService.anonymousName", locale);
    }

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

    public @NotNull List<@NotNull ParticipantDto> getAllParticipants() {
        return dsl.selectFrom(PARTICIPANT)
                .fetchInto(ParticipantDto.class);
    }

    public @NotNull Optional<ParticipantDto> getParticipant(final @NotNull EventDto event,
                                                            final @NotNull UserDto user) {
        return dsl.selectFrom(PARTICIPANT)
                .where(PARTICIPANT.EVENT_ID.eq(event.id())
                        .and(PARTICIPANT.USER_ID.eq(user.id())))
                .fetchOptionalInto(ParticipantDto.class);
    }

    public boolean deleteParticipant(final @NotNull ParticipantDto participant) {
        return dsl.delete(PARTICIPANT)
                .where(PARTICIPANT.EVENT_ID.eq(participant.eventId())
                        .and(PARTICIPANT.USER_ID.eq(participant.userId())))
                .execute() > 0;
    }

    /**
     * <p>Counts the total number of participants.</p>
     *
     * @return The total count of participants; never negative.
     */
    public int getParticipantCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(PARTICIPANT)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Counts the total number of participants of the specified event.</p>
     *
     * @param event The event for which the participants should be counted; must not be {@code null}.
     *
     * @return The total count of participants of the event; never negative.
     */
    public int getParticipantCount(final @NotNull EventDto event) {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(PARTICIPANT)
                        .where(PARTICIPANT.EVENT_ID.eq(event.id()))
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    public boolean isParticipant(final @NotNull UserDto user,
                                 final @NotNull EventDto event) {
        return dsl.fetchExists(dsl.selectFrom(PARTICIPANT)
                .where(PARTICIPANT.USER_ID.eq(user.id())
                        .and(PARTICIPANT.EVENT_ID.eq(event.id()))));
    }

    public boolean isLoggedInUserParticipantOf(final @NotNull EventDto event) {
        return loginService.getLoggedInUser()
                .map(user -> isParticipant(user, event))
                .orElse(false);
    }

    public boolean unregisterFromEvent(final @NotNull UserDto user,
                                       final @NotNull EventDto event,
                                       final @NotNull Locale locale) {
        return getParticipant(event, user)
                .map(participant -> {
                        final var success = deleteParticipant(participant);
                        if (success) {
                            final var email = user.email();
                            if (email != null && !email.isBlank()) {
                                final var eventTitle = event.title();
                                final var eventLink = LinkUtil.getLink(event);
                                final Map<String, String> mailVariables = Map.of(
                                        "eventTitle", eventTitle, "eventLink", eventLink);
                                mailService.sendMail(MailTemplateId.EVENT_UNREGISTRATION_SUCCESS, locale,
                                        MailFormat.MARKDOWN, mailVariables, email);
                            }
                            notifyEventManagersAboutParticipationChange(event, user, locale,
                                    MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS);
                        }
                        return success;
                })
                .orElse(false);
    }

    public @NotNull List<@NotNull RegisteredParticipantDto> getParticipants(final @NotNull EventDto event) {
        return dsl.select(USER.fields())
                .select(PARTICIPANT.REGISTERED)
                .from(PARTICIPANT)
                .join(USER).on(PARTICIPANT.USER_ID.eq(USER.ID))
                .where(PARTICIPANT.EVENT_ID.eq(event.id()))
                .orderBy(PARTICIPANT.REGISTERED.asc())
                .fetch(record -> new RegisteredParticipantDto(
                        record.into(USER).into(UserDto.class),
                        record.get(PARTICIPANT.REGISTERED, ZonedDateTime.class)
                ));
    }

}
