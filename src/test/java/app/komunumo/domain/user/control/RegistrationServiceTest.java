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
package app.komunumo.domain.user.control;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.i18n.TranslationProvider;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_REGISTRATION_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    @Test
    void startRegistrationProcessLogsAndSkipsWhenDisabled() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);
        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class))
                .thenReturn(false);

        try (var logCaptor = LogCaptor.forClass(RegistrationService.class)) {
            registrationService.startRegistrationProcess("Alice", "alice@example.org", Locale.ENGLISH);
            assertThat(logCaptor.getWarnLogs())
                    .contains("Registration attempt while registration is disabled.");
        }

        verifyNoInteractions(userService, loginService, mailService, confirmationService, translationProvider);
    }

    @Test
    void startRegistrationProcessBuildsAndSendsConfirmationRequestWhenEnabled() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);
        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class))
                .thenReturn(true);
        when(translationProvider.getTranslation("user.control.AccountService.registrationText", Locale.ENGLISH))
                .thenReturn("Please confirm registration");

        registrationService.startRegistrationProcess("Alice", "alice@example.org", Locale.ENGLISH);

        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).sendConfirmationMail(org.mockito.ArgumentMatchers.eq("alice@example.org"), requestCaptor.capture());
        final var request = requestCaptor.getValue();
        assertThat(request.actionMessage()).isEqualTo("Please confirm registration");
        assertThat(request.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(request.actionContext().get("name")).isEqualTo("Alice");
    }

    @Test
    void confirmationHandlerCreatesNewLocalUserWhenMissing() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);
        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class)).thenReturn(true);
        when(translationProvider.getTranslation("user.control.AccountService.registrationText", Locale.ENGLISH))
                .thenReturn("Please confirm registration");
        when(translationProvider.getTranslation("user.control.AccountService.successMessage", Locale.ENGLISH))
                .thenReturn("Registration successful");
        when(userService.getUserByEmail("alice@example.org")).thenReturn(Optional.empty());
        final var storedLocalUser = createUser(UUID.randomUUID(), "alice@example.org", "Alice", UserType.LOCAL);
        when(userService.storeUser(any(UserDto.class))).thenReturn(storedLocalUser);

        registrationService.startRegistrationProcess("Alice", "alice@example.org", Locale.ENGLISH);
        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).sendConfirmationMail(org.mockito.ArgumentMatchers.eq("alice@example.org"), requestCaptor.capture());
        final var response = requestCaptor.getValue().actionHandler()
                .handle("alice@example.org", requestCaptor.getValue().actionContext(), Locale.ENGLISH);

        assertThat(response.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("Registration successful");
        assertThat(response.location()).isEqualTo("/settings/profile");
        verify(userService, never()).changeUserType(any(), any());
        verify(mailService).sendMail(MailTemplateId.ACCOUNT_REGISTRATION_SUCCESS, Locale.ENGLISH, MailFormat.MARKDOWN,
                Map.of("name", "Alice"), "alice@example.org");
        verify(loginService).login("alice@example.org");
    }

    @Test
    void confirmationHandlerKeepsLocalUserWithoutTypeChange() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);
        final var localUser = createUser(UUID.randomUUID(), "alice@example.org", "Alice", UserType.LOCAL);

        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class)).thenReturn(true);
        when(translationProvider.getTranslation("user.control.AccountService.registrationText", Locale.ENGLISH))
                .thenReturn("Please confirm registration");
        when(translationProvider.getTranslation("user.control.AccountService.successMessage", Locale.ENGLISH))
                .thenReturn("Registration successful");
        when(userService.getUserByEmail("alice@example.org")).thenReturn(Optional.of(localUser));

        registrationService.startRegistrationProcess("Alice", "alice@example.org", Locale.ENGLISH);
        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).sendConfirmationMail(org.mockito.ArgumentMatchers.eq("alice@example.org"), requestCaptor.capture());
        requestCaptor.getValue().actionHandler().handle("alice@example.org",
                requestCaptor.getValue().actionContext(), Locale.ENGLISH);

        verify(userService, never()).changeUserType(any(), any());
        verify(userService, never()).storeUser(any());
    }

    @Test
    void confirmationHandlerUpgradesNonLocalUserToLocal() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);
        final var remoteUser = createUser(UUID.randomUUID(), "alice@example.org", "Alice", UserType.REMOTE);
        final var localUser = createUser(remoteUser.id(), remoteUser.email(), remoteUser.name(), UserType.LOCAL);

        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class)).thenReturn(true);
        when(translationProvider.getTranslation("user.control.AccountService.registrationText", Locale.ENGLISH))
                .thenReturn("Please confirm registration");
        when(translationProvider.getTranslation("user.control.AccountService.successMessage", Locale.ENGLISH))
                .thenReturn("Registration successful");
        when(userService.getUserByEmail("alice@example.org")).thenReturn(Optional.of(remoteUser));
        when(userService.changeUserType(remoteUser, UserType.LOCAL)).thenReturn(localUser);
        when(userService.storeUser(localUser)).thenReturn(localUser);

        registrationService.startRegistrationProcess("Alice", "alice@example.org", Locale.ENGLISH);
        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).sendConfirmationMail(org.mockito.ArgumentMatchers.eq("alice@example.org"), requestCaptor.capture());
        requestCaptor.getValue().actionHandler().handle("alice@example.org",
                requestCaptor.getValue().actionContext(), Locale.ENGLISH);

        verify(userService).changeUserType(remoteUser, UserType.LOCAL);
        verify(userService).storeUser(localUser);
    }

    private static UserDto createUser(final UUID id,
                                      final String email,
                                      final String name,
                                      final UserType type) {
        return new UserDto(id, null, null, null, email, name, "", null, UserRole.USER, type);
    }
}
