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
package app.komunumo.domain.user.boundary;

import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditProfileViewTest {

    @Test
    void noLoggedInUser_shouldThrowException() {
        final var configurationService = mock(ConfigurationService.class);
        final var loginService = mock(LoginService.class);
        final var userService = mock(UserService.class);
        final var actorHandleService = mock(ActorHandleService.class);

        when(loginService.getLoggedInUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                new EditProfileView(configurationService, loginService, userService, actorHandleService)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No logged-in user");
    }

}
