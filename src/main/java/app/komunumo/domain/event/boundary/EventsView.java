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
package app.komunumo.domain.event.boundary;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jetbrains.annotations.NotNull;

import static app.komunumo.domain.event.boundary.EventsView.EVENTS_PATH;

@AnonymousAllowed
@Route(value = EVENTS_PATH, layout = WebsiteLayout.class)
public final class EventsView extends AbstractView {

    static final String EVENTS_PATH = "events";
    static final String PAST_EVENTS_PATH = "events/past";

    public EventsView(final @NotNull ConfigurationService configurationService,
                      final @NotNull EventService eventService) {
        super(configurationService);
        setId("events-view");

        final var upcomingEventsGrid = new EventGrid(eventService.getUpcomingEventsWithImage());
        upcomingEventsGrid.addClassName("upcoming-events-grid");

        final var pastEventsGrid = new EventGrid(eventService.getPastEventsWithImage());
        pastEventsGrid.addClassName("past-events-grid");

        final var eventsTabSheet = new TabSheet();
        final var upcomingEventsTab = eventsTabSheet.add(
                getTranslation("event.boundary.EventsView.upcomingEvents"),
                upcomingEventsGrid);
        final var pastEventsTab = eventsTabSheet.add(
                getTranslation("event.boundary.EventsView.pastEvents"),
                pastEventsGrid);

        eventsTabSheet.addSelectedChangeListener(event -> {
            final var path = event.getSelectedTab() == pastEventsTab ? PAST_EVENTS_PATH : EVENTS_PATH;
            getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null, path));
        });

        eventsTabSheet.setSelectedTab(upcomingEventsTab);
        eventsTabSheet.setWidthFull();
        add(eventsTabSheet);
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("event.boundary.EventsView.title");
    }

}
