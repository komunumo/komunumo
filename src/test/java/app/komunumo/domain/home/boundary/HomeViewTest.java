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
package app.komunumo.domain.home.boundary;

import app.komunumo.domain.event.boundary.EventsView;
import com.vaadin.flow.router.BeforeEnterEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HomeViewTest {

    @Test
    void shouldForwardToEventGridView() {
        // Arrange
        var event = mock(BeforeEnterEvent.class);
        var view = new HomeView();

        // Act
        view.beforeEnter(event);

        // Assert
        verify(event).forwardTo(EventsView.class);
    }

}
