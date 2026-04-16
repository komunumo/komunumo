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
package app.komunumo.domain.core.confirmation.boundary;

import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.event.boundary.UpcomingEventsView;
import app.komunumo.infra.ui.vaadin.components.PersistentNotification;
import app.komunumo.test.KaribuTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.QueryParameters;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static app.komunumo.test.TestUtil.findComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ConfirmationViewKT extends KaribuTest {

    @MockitoBean
    private ConfirmationService confirmationService;

    @ParameterizedTest
    @ValueSource(strings = {"", "/events"}) // test with and without redirection location
    void testSuccessResponse(final @NotNull String redirectLocation) {
        when(confirmationService.confirm(anyString(), any()))
                .thenReturn(new ConfirmationResponse(ConfirmationStatus.SUCCESS,
                        "This is a success message.", redirectLocation));

        UI.getCurrent().navigate(
                ConfirmationView.class,
                new QueryParameters(Map.of("id", List.of("12345")))
        );

        MockVaadin.clientRoundtrip(false);

        if (redirectLocation.isBlank()) {
            assertThat(currentViewClass()).isEqualTo(ConfirmationView.class);
            final var main = _get(Main.class);
            final var h2 = _get(main, H2.class);
            assertThat(h2.getText()).isEqualTo("Confirmation of your email address");
        } else {
            assertThat(currentViewClass()).isEqualTo(UpcomingEventsView.class);
        }

        expectNotifications("This is a success message.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/events"}) // test with and without redirection location
    void testWarningResponse(final @NotNull String redirectLocation) {
        when(confirmationService.confirm(anyString(), any()))
                .thenReturn(new ConfirmationResponse(ConfirmationStatus.WARNING,
                        "This is a warning message.", redirectLocation));

        UI.getCurrent().navigate(
                ConfirmationView.class,
                new QueryParameters(Map.of("id", List.of("12345")))
        );

        MockVaadin.clientRoundtrip(false);

        if (redirectLocation.isBlank()) {
            assertThat(currentViewClass()).isEqualTo(ConfirmationView.class);
            final var main = _get(Main.class);
            final var h2 = _get(main, H2.class);
            assertThat(h2.getText()).isEqualTo("Confirmation of your email address");
        } else {
            assertThat(currentViewClass()).isEqualTo(UpcomingEventsView.class);
        }

        final var notification = _get(PersistentNotification.class);
        assertThat(notification.isOpened()).isTrue();

        final var div = _get(notification, Div.class);
        assertThat(div.getText()).isEqualTo("This is a warning message.");

        final var closeButton = _get(notification, Button.class);
        _click(closeButton);

        MockVaadin.clientRoundtrip(false);

        assertThat(notification.isOpened()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/events"}) // redirection location should be ignored for error status
    void testErrorResponse(final @NotNull String redirectLocation) {
        when(confirmationService.confirm(anyString(), any()))
                .thenReturn(new ConfirmationResponse(ConfirmationStatus.ERROR,
                        "This is an error message.", redirectLocation));

        UI.getCurrent().navigate(
                ConfirmationView.class,
                new QueryParameters(Map.of("id", List.of("12345")))
        );

        MockVaadin.clientRoundtrip(false);

        assertThat(currentViewClass()).isEqualTo(ConfirmationView.class);
        final var main = _get(Main.class);
        final var h2 = _get(main, H2.class);
        assertThat(h2.getText()).isEqualTo("Confirmation of your email address");

        final var notification = _get(PersistentNotification.class);
        assertThat(notification.isOpened()).isTrue();

        final var div = _get(notification, Div.class);
        assertThat(div.getText()).isEqualTo("This is an error message.");

        final var closeButton = _get(notification, Button.class);
        _click(closeButton);

        MockVaadin.clientRoundtrip(false);

        assertThat(notification.isOpened()).isFalse();
    }

    @Test
    void missingConfirmationIdLeadsTo404NotFound() {
        UI.getCurrent().navigate(ConfirmationView.class);

        final var main = _get(Main.class);
        final var h2 = findComponent(main, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("Page not found");
    }

}
