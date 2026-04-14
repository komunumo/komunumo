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
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.infra.i18n.TranslationProvider;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_REGISTRATION_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    @Test
    void registrationDisabled() {
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mock(UserService.class);
        final var loginService = mock(LoginService.class);
        final var mailService = mock(MailService.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);

        when(configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class))
                .thenReturn(false);

        final var registrationService = new RegistrationService(configurationService, userService, loginService,
                mailService, confirmationService, translationProvider);

        try (var logCaptor = LogCaptor.forClass(RegistrationService.class)) {
            registrationService.startRegistrationProcess("test", "test@example.com", Locale.ENGLISH);
            assertThat(logCaptor.getWarnLogs())
                    .contains("Registration attempt while registration is disabled.");
        }

        verifyNoInteractions(userService, loginService, mailService, confirmationService, translationProvider);
    }
}
