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
package app.komunumo;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.demo.control.DemoMode;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.infra.config.AppConfig;
import app.komunumo.infra.config.DemoConfig;
import app.komunumo.infra.config.FilesConfig;
import app.komunumo.infra.config.InstanceConfig;
import app.komunumo.infra.config.MailConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupHandlerTest {

    @Test
    @SuppressWarnings("DataFlowIssue")
    void shouldCreateAdminIfNoneExistsAndEmailIsSet() {
        final var appConfig = createAppConfig("admin@example.eu");
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mockUserService(0);
        final var demoMode = mock(DemoMode.class);

        final var startupHandler = new StartupHandler(appConfig, configurationService, userService, demoMode);
        startupHandler.onApplicationReady();

        verify(userService).storeUser(argThat(user ->
                user.email().equals("admin@example.eu") &&
                        user.role() == UserRole.ADMIN &&
                        user.handle().equals("admin")
        ));
    }

    @Test
    void shouldSkipCreationIfAdminAlreadyExists() {
        final var appConfig = createAppConfig("admin@example.eu");
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mockUserService(1);
        final var demoMode = mock(DemoMode.class);

        final var startupHandler = new StartupHandler(appConfig, configurationService, userService, demoMode);
        startupHandler.onApplicationReady();

        verify(userService, never()).storeUser(any());
    }

    @Test
    void shouldSkipCreationIfNoEmailSet() {
        final var appConfig = createAppConfig("");
        final var configurationService = mock(ConfigurationService.class);
        final var userService = mockUserService(0);
        final var demoMode = mock(DemoMode.class);

        final var startupHandler = new StartupHandler(appConfig, configurationService, userService, demoMode);
        startupHandler.onApplicationReady();

        verify(userService, never()).storeUser(any());
    }

    private UserService mockUserService(final int adminCount) {
        final var userService = mock(UserService.class);
        when(userService.getAdminCount()).thenReturn(adminCount);
        return userService;
    }

    private AppConfig createAppConfig(final @NotNull String email) {
        final var version = "0.0.0";
        final var demoConfig = new DemoConfig(false, "");
        final var filesConfig = new FilesConfig(Path.of("/tmp"));
        final var instanceConfig = new InstanceConfig(email);
        final var mailConfig = new MailConfig("", "");
        return new AppConfig(version, demoConfig, filesConfig, instanceConfig, mailConfig);
    }

}
