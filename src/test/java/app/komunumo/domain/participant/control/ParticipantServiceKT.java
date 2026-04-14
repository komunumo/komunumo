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

import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationContext;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.test.KaribuTest;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static app.komunumo.data.db.Tables.MEMBER;
import static app.komunumo.data.db.Tables.USER;
import static app.komunumo.domain.participant.control.ParticipantService.CONTEXT_KEY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ParticipantServiceKT extends KaribuTest {

    @Autowired
    private @NotNull ParticipantService participantService;

    @Autowired
    private @NotNull EventService eventService;

    @Autowired
    private @NotNull UserService userService;

    @Autowired
    private @NotNull DSLContext dsl;

    @Autowired
    private @NotNull LoginService loginService;

    @Autowired
    private @NotNull ConfirmationService confirmationService;

    @Autowired
    private @NotNull TranslationProvider translationProvider;

    @Test
    void joinEventExistingUser() {
        assertThat(participantService.getAllParticipants()).hasSize(6);

        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();
        assertThat(event.id()).isNotNull();

        assertThat(participantService.getParticipantCount(event)).isZero();

        final var email = "test@example.com";
        assertThat(userService.getUserByEmail(email)).isEmpty();

        userService.storeUser(new UserDto(null, null, null, null, email, "", "",
                null, UserRole.USER, UserType.LOCAL));
        final var user = userService.getUserByEmail(email).orElseThrow();
        assertThat(user).isNotNull();

        final var context = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var locale = Locale.ENGLISH;
        final var confirmationResponse = participantService.handleConfirmationResponse(email, context, locale);
        assertThat(confirmationResponse.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);

        assertThat(participantService.getParticipantCount(event)).isOne();
        final var participants = participantService.getAllParticipants();
        assertThat(participants).hasSize(7);

        final var participant = participants.stream()
                .filter(p -> p.userId().equals(user.id()))
                .findFirst()
                .orElseThrow();
        assertThat(participant).isNotNull().satisfies(testee -> {
            assertThat(testee.eventId()).isEqualTo(event.id());
            assertThat(testee.userId()).isEqualTo(user.id());
            assertThat(testee.registered()).isNotNull();
            assertThat(testee.registered()).isBeforeOrEqualTo(ZonedDateTime.now());
        });

        assertThat(participantService.deleteParticipant(participant)).isTrue();
        assertThat(participantService.deleteParticipant(participant)).isFalse();
        assertThat(participantService.getAllParticipants()).hasSize(6);
        assertThat(participantService.getParticipantCount(event)).isZero();

        assertThat(userService.deleteUser(user)).isTrue();
        assertThat(userService.deleteUser(user)).isFalse();
    }

    @Test
    void joinEventAnonymousUser() {
        assertThat(participantService.getAllParticipants()).hasSize(6);

        final var email = "test@example.com";
        assertThat(userService.getUserByEmail(email)).isEmpty();

        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var context = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var locale = Locale.ENGLISH;
        final var confirmationResponse1 = participantService.handleConfirmationResponse(email, context, locale);
        assertThat(confirmationResponse1.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);

        // try to join again with the same email
        final var confirmationResponse2 = participantService.handleConfirmationResponse(email, context, locale);
        assertThat(confirmationResponse2.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);

        final var participants = participantService.getAllParticipants();
        assertThat(participants).hasSize(7);

        final var user = userService.getUserByEmail(email).orElseThrow();
        assertThat(user.email()).isEqualTo(email);

        final var participant = participants.stream()
                .filter(p -> p.userId().equals(user.id()))
                .findFirst()
                .orElseThrow();
        assertThat(participant).isNotNull().satisfies(testee -> {
            assertThat(testee.eventId()).isEqualTo(event.id());
            assertThat(testee.userId()).isEqualTo(user.id());
            assertThat(testee.registered()).isNotNull();
            assertThat(testee.registered()).isBeforeOrEqualTo(ZonedDateTime.now());
        });

        assertThat(participantService.deleteParticipant(participant)).isTrue();
        assertThat(participantService.deleteParticipant(participant)).isFalse();
        assertThat(participantService.getAllParticipants()).hasSize(6);

        assertThat(userService.deleteUser(user)).isTrue();
        assertThat(userService.deleteUser(user)).isFalse();
    }

    @Test
    void joinEvent_withInvalidEventId_shouldFail() {
        assertThat(participantService.getAllParticipants()).hasSize(6);

        final var email = "test@example.com";
        assertThat(userService.getUserByEmail(email)).isEmpty();

        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var eventWithoutId = new EventDto(
                null,
                event.communityId(),
                event.created(),
                event.updated(),
                event.title(),
                event.description(),
                event.location(),
                event.begin(),
                event.end(),
                event.imageId(),
                event.anonymousParticipationAllowed(),
                event.visibility(),
                event.status()
        );

        final var context = ConfirmationContext.of(CONTEXT_KEY_EVENT, eventWithoutId);
        final var locale = Locale.ENGLISH;
        final var confirmationResponse = participantService.handleConfirmationResponse(email, context, locale);
        assertThat(confirmationResponse.confirmationStatus()).isEqualTo(ConfirmationStatus.WARNING);

        assertThat(participantService.getAllParticipants()).hasSize(6);
    }

    @Test
    void registerForEvent_withInvalidEventId_shouldFail() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var eventWithoutId = new EventDto(
                null,
                event.communityId(),
                event.created(),
                event.updated(),
                event.title(),
                event.description(),
                event.location(),
                event.begin(),
                event.end(),
                event.imageId(),
                event.anonymousParticipationAllowed(),
                event.visibility(),
                event.status()
        );

        final var user = getTestUser(UserRole.USER);
        final var locale = Locale.ENGLISH;

        final var result = participantService.registerForEvent(eventWithoutId, user, locale);

        assertThat(result).isFalse();
        assertThat(participantService.getAllParticipants()).hasSize(6);
    }

    @Test
    void registerForEvent_withInvalidUserId_shouldReturnFalse() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var user = getTestUser(UserRole.USER);
        final var userWithoutId = new UserDto(
                null,
                user.created(),
                user.updated(),
                user.profile(),
                user.email(),
                user.name(),
                user.bio(),
                user.imageId(),
                user.role(),
                user.type()
        );

        final var locale = Locale.ENGLISH;

        final var result = participantService.registerForEvent(event, userWithoutId, locale);

        assertThat(result).isFalse();
        assertThat(participantService.getAllParticipants()).hasSize(6);
    }

    @Test
    void registerForEvent_withUserWithEmailNull_shouldReturnTrue() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var user = userService.storeUser(new UserDto(
                null, null, null, "@test", null, "Test", "",
                null, UserRole.USER, UserType.REMOTE));

        final var locale = Locale.ENGLISH;

        final var result = participantService.registerForEvent(event, user, locale);

        assertThat(result).isTrue();
        assertThat(participantService.getAllParticipants()).hasSize(7);
    }

    @Test
    void registerForEvent_withUserWithEmailBlank_shouldReturnTrue() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        assertThat(event).isNotNull();

        final var user = userService.storeUser(new UserDto(
                null, null, null, "@test", "   ", "Test", "",
                null, UserRole.USER, UserType.REMOTE));

        final var locale = Locale.ENGLISH;

        final var result = participantService.registerForEvent(event, user, locale);

        assertThat(result).isTrue();
        assertThat(participantService.getAllParticipants()).hasSize(7);
    }

    @Test
    void handleConfirmationResponse_shouldNotifyEventManagers_withParticipantName() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        final var email = "participant-with-name@example.com";
        userService.storeUser(new UserDto(
                null, null, null, null, email, "Alice", "",
                null, UserRole.USER, UserType.LOCAL));

        final var expectedRecipientEmails = dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .filter(recipientEmail -> !recipientEmail.isBlank())
                .distinct()
                .toArray(String[]::new);

        final var mailServiceMock = mock(MailService.class);
        final var service = new ParticipantService(
                dsl,
                mailServiceMock,
                userService,
                loginService,
                confirmationService,
                translationProvider
        );

        final var context = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var locale = Locale.ENGLISH;
        final var confirmationResponse = service.handleConfirmationResponse(email, context, locale);

        assertThat(confirmationResponse.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        final var expectedParticipantCount = Integer.toString(service.getParticipantCount(event));
        assertManagerNotificationMail(mailServiceMock, MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS,
                locale, event.title(), "Alice",
                expectedParticipantCount, expectedRecipientEmails);
    }

    @Test
    void handleConfirmationResponse_shouldNotifyEventManagers_withSomeone_forAnonymousUser() {
        final var event = eventService.getUpcomingEventsWithImage().getFirst().event();
        final var email = "anonymous-participant@example.com";

        final var expectedRecipientEmails = dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .filter(recipientEmail -> !recipientEmail.isBlank())
                .distinct()
                .toArray(String[]::new);

        final var mailServiceMock = mock(MailService.class);
        final var service = new ParticipantService(
                dsl,
                mailServiceMock,
                userService,
                loginService,
                confirmationService,
                translationProvider
        );

        final var context = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var locale = Locale.ENGLISH;
        final var confirmationResponse = service.handleConfirmationResponse(email, context, locale);

        assertThat(confirmationResponse.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        final var expectedParticipantCount = Integer.toString(service.getParticipantCount(event));
        assertManagerNotificationMail(mailServiceMock, MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS,
                locale, event.title(), "Someone",
                expectedParticipantCount, expectedRecipientEmails);
    }

    private void assertManagerNotificationMail(final @NotNull MailService mailServiceMock,
                                               final @NotNull MailTemplateId mailTemplateId,
                                               final @NotNull Locale locale,
                                               final @NotNull String eventTitle,
                                               final @NotNull String participantName,
                                               final @NotNull String participantCount,
                                               final @NotNull String[] expectedRecipientEmails) {
        final var managerMailInvocation = mockingDetails(mailServiceMock).getInvocations().stream()
                .filter(invocation -> "sendMail".equals(invocation.getMethod().getName()))
                .filter(invocation -> invocation.getArguments().length >= 5)
                .filter(invocation -> invocation.getArguments()[0] == mailTemplateId)
                .findFirst()
                .orElseThrow();

        assertMailArguments(managerMailInvocation, locale, eventTitle, participantName,
                participantCount, expectedRecipientEmails);
    }

    private void assertNoMailWithTemplate(final @NotNull MailService mailServiceMock,
                                          final @NotNull MailTemplateId mailTemplateId) {
        final var templateMailSent = mockingDetails(mailServiceMock).getInvocations().stream()
                .filter(invocation -> "sendMail".equals(invocation.getMethod().getName()))
                .anyMatch(invocation -> invocation.getArguments().length > 0
                        && invocation.getArguments()[0] == mailTemplateId);
        assertThat(templateMailSent).isFalse();
    }

    @SuppressWarnings("unchecked")
    private void assertMailArguments(final @NotNull Invocation invocation,
                                     final @NotNull Locale locale,
                                     final @NotNull String eventTitle,
                                     final @NotNull String participantName,
                                     final @NotNull String participantCount,
                                     final @NotNull String[] expectedRecipientEmails) {
        final var arguments = invocation.getArguments();
        assertThat(arguments[1]).isEqualTo(locale);
        assertThat(arguments[2]).isEqualTo(MailFormat.MARKDOWN);

        final var variables = (java.util.Map<String, String>) arguments[3];
        assertThat(variables).containsEntry("eventTitle", eventTitle);
        assertThat(variables).containsEntry("participantName", participantName);
        assertThat(variables).containsEntry("participantCount", participantCount);

        final var actualRecipients = Arrays.stream(arguments)
                .skip(4)
                .map(String.class::cast)
                .toArray(String[]::new);
        assertThat(actualRecipients).containsExactlyInAnyOrder(expectedRecipientEmails);
    }

    /**
     * <p>Verifies the defensive behavior of {@link ParticipantService#unregisterFromEvent(UserDto, EventDto, Locale)}
     * in the unlikely case that the participant deletion fails.</p>
     *
     * <p>From a business perspective, this scenario cannot normally occur. The participant is retrieved immediately
     * before the deletion is executed, so the subsequent delete operation is expected to succeed under normal
     * conditions.</p>
     *
     * <p>This test therefore covers a purely technical safeguard. It simulates a failure of the underlying delete
     * operation to ensure that the service behaves correctly in such a situation, specifically that no confirmation
     * email is sent and the method returns {@code false}.</p>
     */
    @Test
    void unregisterFromEvent_shouldNotSendMail_whenDeletionFails() {
        final var participant = participantService.getAllParticipants().getFirst();
        final var event = eventService.getEvent(participant.eventId()).orElseThrow();
        final var user = userService.getUserById(participant.userId()).orElseThrow();
        final var locale = Locale.ENGLISH;

        final var mailServiceMock = mock(MailService.class);

        final var service = spy(new ParticipantService(
                dsl,
                mailServiceMock,
                userService,
                loginService,
                confirmationService,
                translationProvider
        ));

        doReturn(Optional.of(participant)).when(service).getParticipant(event, user);
        doReturn(false).when(service).deleteParticipant(participant);

        final var result = service.unregisterFromEvent(user, event, locale);

        assertThat(result).isFalse();
        verify(mailServiceMock, never()).sendMail(any(), any(), any(), any(), any());
    }

    @Test
    void unregisterFromEvent_shouldNotSendMail_whenEmailIsNull() {
        final var participant = participantService.getAllParticipants().getFirst();
        final var event = eventService.getEvent(participant.eventId()).orElseThrow();
        final var user = userService.getUserById(participant.userId()).orElseThrow();
        final var locale = Locale.ENGLISH;

        final var userWithoutEmail = new UserDto(
                user.id(),
                user.created(),
                user.updated(),
                user.profile(),
                null,
                user.name(),
                user.bio(),
                user.imageId(),
                user.role(),
                user.type()
        );

        final var mailServiceMock = mock(MailService.class);

        final var service = new ParticipantService(
                dsl,
                mailServiceMock,
                userService,
                loginService,
                confirmationService,
                translationProvider
        );

        final var result = service.unregisterFromEvent(userWithoutEmail, event, locale);

        assertThat(result).isTrue();
        assertNoMailWithTemplate(mailServiceMock, MailTemplateId.EVENT_UNREGISTRATION_SUCCESS);

        final var expectedRecipientEmails = dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .filter(recipientEmail -> !recipientEmail.isBlank())
                .distinct()
                .toArray(String[]::new);
        final var expectedParticipantCount = Integer.toString(service.getParticipantCount(event));
        assertManagerNotificationMail(mailServiceMock, MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS,
                locale, event.title(), userWithoutEmail.name(), expectedParticipantCount, expectedRecipientEmails);
    }

    @Test
    void unregisterFromEvent_shouldNotSendMail_whenEmailIsEmpty() {
        final var participant = participantService.getAllParticipants().getFirst();
        final var event = eventService.getEvent(participant.eventId()).orElseThrow();
        final var user = userService.getUserById(participant.userId()).orElseThrow();
        final var locale = Locale.ENGLISH;

        final var userWithoutEmail = new UserDto(
                user.id(),
                user.created(),
                user.updated(),
                user.profile(),
                "",
                user.name(),
                user.bio(),
                user.imageId(),
                user.role(),
                user.type()
        );

        final var mailServiceMock = mock(MailService.class);

        final var service = new ParticipantService(
                dsl,
                mailServiceMock,
                userService,
                loginService,
                confirmationService,
                translationProvider
        );

        final var result = service.unregisterFromEvent(userWithoutEmail, event, locale);

        assertThat(result).isTrue();
        assertNoMailWithTemplate(mailServiceMock, MailTemplateId.EVENT_UNREGISTRATION_SUCCESS);

        final var expectedRecipientEmails = dsl.select(USER.EMAIL)
                .from(MEMBER)
                .join(USER).on(MEMBER.USER_ID.eq(USER.ID))
                .where(MEMBER.COMMUNITY_ID.eq(event.communityId()))
                .and(MEMBER.ROLE.in(MemberRole.OWNER.name(), MemberRole.ORGANIZER.name()))
                .and(USER.EMAIL.isNotNull())
                .fetch(USER.EMAIL)
                .stream()
                .filter(recipientEmail -> !recipientEmail.isBlank())
                .distinct()
                .toArray(String[]::new);
        final var expectedParticipantCount = Integer.toString(service.getParticipantCount(event));
        assertManagerNotificationMail(mailServiceMock, MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS,
                locale, event.title(), userWithoutEmail.name(), expectedParticipantCount, expectedRecipientEmails);
    }

}
