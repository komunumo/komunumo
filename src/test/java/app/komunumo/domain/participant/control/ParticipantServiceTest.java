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
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.i18n.TranslationProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ParticipantServiceTest {

    @Test
    void startConfirmationProcessBuildsAndStartsRequest() {
        final var participantStore = mock(ParticipantStore.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var service = createService(participantStore, confirmationService, translationProvider);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        when(translationProvider.getTranslation("participant.control.ParticipantService.actionText",
                Locale.GERMAN, event.title())).thenReturn("Jetzt anmelden");

        service.startConfirmationProcess(event, Locale.GERMAN);

        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).startConfirmationProcess(requestCaptor.capture());
        final var request = requestCaptor.getValue();
        assertThat(request.actionMessage()).isEqualTo("Jetzt anmelden");
        assertThat(request.locale()).isEqualTo(Locale.GERMAN);
        assertThat(request.actionContext().get(ParticipantService.CONTEXT_KEY_EVENT)).isEqualTo(event);
    }

    @Test
    void handleConfirmationResponseUsesExistingUserAndReturnsSuccess() {
        final var participantStore = mock(ParticipantStore.class);
        final var userService = mock(UserService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), translationProvider);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        final var context = ConfirmationContext.of(ParticipantService.CONTEXT_KEY_EVENT, event);
        final var participant = createParticipant(event.id(), user.id());
        when(userService.getUserByEmail("member@example.org")).thenReturn(Optional.of(user));
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.getParticipantCount(event)).thenReturn(3);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));
        when(translationProvider.getTranslation("participant.control.ParticipantService.registrationSuccessMessage",
                Locale.ENGLISH, event.title())).thenReturn("Registration succeeded");

        final var response = service.handleConfirmationResponse("member@example.org", context, Locale.ENGLISH);

        assertThat(response.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("Registration succeeded");
        assertThat(response.location()).isEqualTo("/events/" + event.id());
        verify(userService, never()).createAnonymousUserWithEmail(any());
    }

    @Test
    void handleConfirmationResponseCreatesAnonymousUserWhenMissing() {
        final var participantStore = mock(ParticipantStore.class);
        final var userService = mock(UserService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), translationProvider);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var newUser = createUser(UUID.randomUUID(), "new@example.org", "Alice");
        final var context = ConfirmationContext.of(ParticipantService.CONTEXT_KEY_EVENT, event);
        when(userService.getUserByEmail("new@example.org")).thenReturn(Optional.empty());
        when(userService.createAnonymousUserWithEmail("new@example.org")).thenReturn(newUser);
        when(participantStore.getParticipant(event, newUser)).thenReturn(Optional.empty());
        when(participantStore.getParticipantCount(event)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of());
        when(translationProvider.getTranslation("participant.control.ParticipantService.registrationSuccessMessage",
                Locale.ENGLISH, event.title())).thenReturn("Registration succeeded");

        final var response = service.handleConfirmationResponse("new@example.org", context, Locale.ENGLISH);

        assertThat(response.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        verify(userService).createAnonymousUserWithEmail("new@example.org");
    }

    @Test
    void handleConfirmationResponseReturnsWarningWhenRegistrationFails() {
        final var participantStore = mock(ParticipantStore.class);
        final var userService = mock(UserService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var service = createService(participantStore, mock(MailService.class), userService,
                mock(LoginService.class), mock(ConfirmationService.class), translationProvider);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var userWithoutId = createUser(null, "member@example.org", "Alice");
        final var context = ConfirmationContext.of(ParticipantService.CONTEXT_KEY_EVENT, event);
        when(userService.getUserByEmail("member@example.org")).thenReturn(Optional.of(userWithoutId));
        when(translationProvider.getTranslation("participant.control.ParticipantService.registrationFailedMessage",
                Locale.ENGLISH, event.title())).thenReturn("Registration failed");

        final var response = service.handleConfirmationResponse("member@example.org", context, Locale.ENGLISH);

        assertThat(response.confirmationStatus()).isEqualTo(ConfirmationStatus.WARNING);
        assertThat(response.message()).isEqualTo("Registration failed");
    }

    @Test
    void registerForEventReturnsFalseWhenEventIdIsNull() {
        final var participantStore = mock(ParticipantStore.class);
        final var eventService = mock(EventService.class);
        final var service = createService(participantStore, eventService);
        final var event = createEvent(null, UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(eventService.isRegistrationAllowed(event)).thenReturn(true);

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isFalse();
        verifyNoInteractions(participantStore);
    }

    @Test
    void registerForEventReturnsFalseWhenUserIdIsNull() {
        final var participantStore = mock(ParticipantStore.class);
        final var eventService = mock(EventService.class);
        final var service = createService(participantStore, eventService);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(null, "member@example.org", "Alice");
        when(eventService.isRegistrationAllowed(event)).thenReturn(true);

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isFalse();
        verifyNoInteractions(participantStore);
    }

    @Test
    void registerForEventReturnsFalseWhenRegistrationIsNotAllowed() {
        final var participantStore = mock(ParticipantStore.class);
        final var eventService = mock(EventService.class);
        final var service = createService(participantStore, eventService);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(eventService.isRegistrationAllowed(event)).thenReturn(false);

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isFalse();
        verifyNoInteractions(participantStore);
    }

    @Test
    void registerForEventStoresExistingParticipantAndSendsMails() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        final var participant = createParticipant(event.id(), user.id());
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.getParticipantCount(event)).thenReturn(4);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(participantStore).storeParticipant(participant);
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_SUCCESS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), eq("member@example.org"));

        final var mapCaptor = ArgumentCaptor.forClass(Map.class);
        final var recipientsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), mapCaptor.capture(), recipientsCaptor.capture());
        @SuppressWarnings("unchecked")
        final Map<String, String> managerMailVariables = (Map<String, String>) mapCaptor.getValue();
        assertThat(managerMailVariables).containsEntry("participantName", "Alice")
                .containsEntry("participantCount", "4");
        assertThat(recipientsCaptor.getValue()).containsExactly("owner@example.org");
    }

    @Test
    void registerForEventCreatesParticipantWhenMissing() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.empty());
        when(participantStore.getParticipantCount(event)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of());

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(participantStore).storeParticipant(argThat(participant ->
                participant.eventId().equals(event.id()) && participant.userId().equals(user.id())));
    }

    @Test
    void registerForEventSkipsSuccessMailWhenEmailBlank() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "   ", "Alice");
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.empty());
        when(participantStore.getParticipantCount(event)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(mailService, never()).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_SUCCESS), any(), any(), any(), any());
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), any(String[].class));
    }

    @Test
    void registerForEventSkipsSuccessMailWhenEmailNull() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), null, "Alice");
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.empty());
        when(participantStore.getParticipantCount(event)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));

        final var result = service.registerForEvent(event, user, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(mailService, never()).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_SUCCESS), any(), any(), any(), any());
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), any(String[].class));
    }

    @Test
    void registerForEventUsesAnonymousFallbackNameWhenUserNameIsBlank() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), translationProvider);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "  ");
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.empty());
        when(participantStore.getParticipantCount(event)).thenReturn(2);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));
        when(translationProvider.getTranslation("participant.control.ParticipantService.anonymousName",
                Locale.ENGLISH)).thenReturn("Someone");

        service.registerForEvent(event, user, Locale.ENGLISH);

        final var mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), mapCaptor.capture(), any(String[].class));
        @SuppressWarnings("unchecked")
        final Map<String, String> managerMailVariables = (Map<String, String>) mapCaptor.getValue();
        assertThat(managerMailVariables).containsEntry("participantName", "Someone");
    }

    @Test
    void storeParticipantDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var participant = createParticipant(UUID.randomUUID(), UUID.randomUUID());

        service.storeParticipant(participant);

        verify(participantStore).storeParticipant(participant);
    }

    @Test
    void getAllParticipantsDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var expected = List.of(createParticipant(UUID.randomUUID(), UUID.randomUUID()));
        when(participantStore.getAllParticipants()).thenReturn(expected);

        final var result = service.getAllParticipants();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getParticipantDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        final var expected = Optional.of(createParticipant(event.id(), user.id()));
        when(participantStore.getParticipant(event, user)).thenReturn(expected);

        final var result = service.getParticipant(event, user);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void deleteParticipantDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var participant = createParticipant(UUID.randomUUID(), UUID.randomUUID());
        when(participantStore.deleteParticipant(participant)).thenReturn(1);

        final var result = service.deleteParticipant(participant);

        assertThat(result).isTrue();
    }

    @Test
    void deleteParticipantReturnsFalseWhenStoreDeletesNothing() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var participant = createParticipant(UUID.randomUUID(), UUID.randomUUID());
        when(participantStore.deleteParticipant(participant)).thenReturn(0);

        final var result = service.deleteParticipant(participant);

        assertThat(result).isFalse();
    }

    @Test
    void getParticipantCountDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        when(participantStore.getParticipantCount()).thenReturn(12);

        final var result = service.getParticipantCount();

        assertThat(result).isEqualTo(12);
    }

    @Test
    void getParticipantCountByEventDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        when(participantStore.getParticipantCount(event)).thenReturn(5);

        final var result = service.getParticipantCount(event);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void isParticipantDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(participantStore.isParticipant(user, event)).thenReturn(true);

        final var result = service.isParticipant(user, event);

        assertThat(result).isTrue();
    }

    @Test
    void isLoggedInUserParticipantOfReturnsFalseWhenNoUserIsLoggedIn() {
        final var participantStore = mock(ParticipantStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(participantStore, mock(MailService.class), mock(UserService.class),
                loginService, mock(ConfirmationService.class), mock(TranslationProvider.class));
        when(loginService.getLoggedInUser()).thenReturn(Optional.empty());

        final var result = service.isLoggedInUserParticipantOf(createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup"));

        assertThat(result).isFalse();
        verifyNoInteractions(participantStore);
    }

    @Test
    void isLoggedInUserParticipantOfReturnsTrueWhenUserParticipates() {
        final var participantStore = mock(ParticipantStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(participantStore, mock(MailService.class), mock(UserService.class),
                loginService, mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(loginService.getLoggedInUser()).thenReturn(Optional.of(user));
        when(participantStore.isParticipant(user, event)).thenReturn(true);

        final var result = service.isLoggedInUserParticipantOf(event);

        assertThat(result).isTrue();
    }

    @Test
    void isLoggedInUserParticipantOfReturnsFalseWhenUserDoesNotParticipate() {
        final var participantStore = mock(ParticipantStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(participantStore, mock(MailService.class), mock(UserService.class),
                loginService, mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(loginService.getLoggedInUser()).thenReturn(Optional.of(user));
        when(participantStore.isParticipant(user, event)).thenReturn(false);

        final var result = service.isLoggedInUserParticipantOf(event);

        assertThat(result).isFalse();
    }

    @Test
    void unregisterFromEventReturnsFalseWhenParticipantIsMissing() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.empty());

        final var result = service.unregisterFromEvent(user, event, Locale.ENGLISH);

        assertThat(result).isFalse();
    }

    @Test
    void unregisterFromEventReturnsFalseWhenDeletionFails() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        final var participant = createParticipant(event.id(), user.id());
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.deleteParticipant(participant)).thenReturn(0);

        final var result = service.unregisterFromEvent(user, event, Locale.ENGLISH);

        assertThat(result).isFalse();
        verifyNoInteractions(mailService);
    }

    @Test
    void unregisterFromEventSendsMailsWhenDeletionSucceeds() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "member@example.org", "Alice");
        final var participant = createParticipant(event.id(), user.id());
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.deleteParticipant(participant)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));
        when(participantStore.getParticipantCount(event)).thenReturn(2);

        final var result = service.unregisterFromEvent(user, event, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_SUCCESS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), eq("member@example.org"));
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), any(String[].class));
    }

    @Test
    void unregisterFromEventSkipsUserMailWhenEmailIsBlank() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), "  ", "Alice");
        final var participant = createParticipant(event.id(), user.id());
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.deleteParticipant(participant)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));
        when(participantStore.getParticipantCount(event)).thenReturn(2);

        final var result = service.unregisterFromEvent(user, event, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(mailService, never()).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_SUCCESS), any(), any(), any(), any());
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), any(String[].class));
    }

    @Test
    void unregisterFromEventSkipsUserMailWhenEmailIsNull() {
        final var participantStore = mock(ParticipantStore.class);
        final var mailService = mock(MailService.class);
        final var service = createService(participantStore, mailService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var user = createUser(UUID.randomUUID(), null, "Alice");
        final var participant = createParticipant(event.id(), user.id());
        when(participantStore.getParticipant(event, user)).thenReturn(Optional.of(participant));
        when(participantStore.deleteParticipant(participant)).thenReturn(1);
        when(participantStore.getManagerEmailsForEvent(event)).thenReturn(List.of("owner@example.org"));
        when(participantStore.getParticipantCount(event)).thenReturn(2);

        final var result = service.unregisterFromEvent(user, event, Locale.ENGLISH);

        assertThat(result).isTrue();
        verify(mailService, never()).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_SUCCESS), any(), any(), any(), any());
        verify(mailService).sendMail(eq(MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS), eq(Locale.ENGLISH),
                eq(MailFormat.MARKDOWN), any(Map.class), any(String[].class));
    }

    @Test
    void getParticipantsDelegatesToStore() {
        final var participantStore = mock(ParticipantStore.class);
        final var service = createService(participantStore);
        final var event = createEvent(UUID.randomUUID(), UUID.randomUUID(), "Meetup");
        final var expected = List.of(new RegisteredParticipantDto(
                createUser(UUID.randomUUID(), "member@example.org", "Alice"),
                ZonedDateTime.now()
        ));
        when(participantStore.getParticipants(event)).thenReturn(expected);

        final var result = service.getParticipants(event);

        assertThat(result).isEqualTo(expected);
    }

    private static ParticipantService createService(final ParticipantStore participantStore) {
        return createService(participantStore, mock(EventService.class));
    }

    private static ParticipantService createService(final ParticipantStore participantStore,
                                                    final EventService eventService) {
        when(eventService.isRegistrationAllowed(any(EventDto.class))).thenReturn(true);
        return createService(participantStore, mock(MailService.class), eventService, mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
    }

    private static ParticipantService createService(final ParticipantStore participantStore,
                                                    final ConfirmationService confirmationService,
                                                    final TranslationProvider translationProvider) {
        final var eventService = mock(EventService.class);
        when(eventService.isRegistrationAllowed(any(EventDto.class))).thenReturn(true);
        return createService(participantStore, mock(MailService.class), eventService, mock(UserService.class),
                mock(LoginService.class), confirmationService, translationProvider);
    }

    private static ParticipantService createService(final ParticipantStore participantStore,
                                                    final MailService mailService,
                                                    final UserService userService,
                                                    final LoginService loginService,
                                                    final ConfirmationService confirmationService,
                                                    final TranslationProvider translationProvider) {
        final var eventService = mock(EventService.class);
        when(eventService.isRegistrationAllowed(any(EventDto.class))).thenReturn(true);
        return createService(participantStore, mailService, eventService, userService,
                loginService, confirmationService, translationProvider);
    }

    private static ParticipantService createService(final ParticipantStore participantStore,
                                                    final MailService mailService,
                                                    final EventService eventService,
                                                    final UserService userService,
                                                    final LoginService loginService,
                                                    final ConfirmationService confirmationService,
                                                    final TranslationProvider translationProvider) {
        when(eventService.isRegistrationAllowed(any(EventDto.class))).thenReturn(true);
        return new ParticipantService(participantStore, mailService, eventService, userService,
                loginService, confirmationService, translationProvider);
    }

    @SuppressWarnings("SameParameterValue")
    private static EventDto createEvent(final UUID eventId,
                                        final UUID communityId,
                                        final String title) {
        return new EventDto(
                eventId,
                communityId,
                null,
                null,
                title,
                "Description",
                "Location",
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(2),
                null,
                false,
                EventVisibility.PUBLIC,
                EventStatus.PUBLISHED
        );
    }

    private static UserDto createUser(final UUID userId,
                                      final String email,
                                      final String name) {
        return new UserDto(
                userId,
                null,
                null,
                "@user",
                email,
                name,
                "",
                null,
                UserRole.USER,
                UserType.LOCAL
        );
    }

    private static ParticipantDto createParticipant(final UUID eventId, final UUID userId) {
        return new ParticipantDto(eventId, userId, ZonedDateTime.now());
    }
}
