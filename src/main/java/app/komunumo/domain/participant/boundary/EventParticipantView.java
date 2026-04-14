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
package app.komunumo.domain.participant.boundary;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.ParseUtil;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static app.komunumo.util.DateTimeUtil.getLocalizedDateTimeString;

@Route(value = "events/:eventId/participants", layout = WebsiteLayout.class)
@RolesAllowed("USER_LOCAL")
public final class EventParticipantView extends AbstractView implements BeforeEnterObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventParticipantView.class);

    private final @NotNull HtmlContainer pageContent = new Div();
    private final @NotNull EventService eventService;
    private final @NotNull ParticipantService participantService;
    private final @NotNull LoginService loginService;

    private @NotNull String pageTitle = "";

    public EventParticipantView(final @NotNull ConfigurationService configurationService,
                                final @NotNull EventService eventService,
                                final @NotNull ParticipantService participantService,
                                final @NotNull LoginService loginService) {
        super(configurationService);
        this.eventService = eventService;
        this.participantService = participantService;
        this.loginService = loginService;
        addClassName("event-participant-view");
        add(pageContent);
    }

    @Override
    protected @NotNull String getViewTitle() {
        return pageTitle;
    }

    @Override
    public void beforeEnter(final @NotNull BeforeEnterEvent beforeEnterEvent) {
        final var params = beforeEnterEvent.getRouteParameters();
        final var eventId = ParseUtil.parseUUID(params.get("eventId"))
                .orElseThrow(() -> {
                    LOGGER.warn("Invalid event ID '{}'!", params.get("eventId").orElse(""));
                    return new NotFoundException("");
                });

        eventService.getEvent(eventId).ifPresentOrElse(event -> {
            pageTitle = getTranslation("participant.boundary.EventParticipantView.pageTitle", event.title());
            showDetails(event);
        }, () -> {
            LOGGER.warn("Event not found with id '{}'!", eventId);
            beforeEnterEvent.rerouteToError(NotFoundException.class);
        });
    }

    private void showDetails(final @NotNull EventDto event) {
        final var loggedInUser = loginService.getLoggedInUser().orElseThrow(NotFoundException::new);
        if (!eventService.hasManagementPermission(event, loggedInUser)) {
            LOGGER.warn("User '{}' does not have permission to view participants of event '{}'!",
                    loggedInUser.id(), event.id());
            throw new NotFoundException();
        }

        pageContent.removeAll();
        pageContent.add(new H2(pageTitle));
        pageContent.add(new Anchor(LinkUtil.getLink(event),
                getTranslation("participant.boundary.EventParticipantView.backToEvent")));

        final var participantCounter = new AtomicInteger(0);

        final var participantsGrid = new Grid<>(RegisteredParticipantDto.class, false);
        participantsGrid.addClassName("participants-grid");
        participantsGrid.addColumn(_ -> participantCounter.incrementAndGet())
                .setHeader("#")
                .setAutoWidth(true)
                .setFlexGrow(0);
        participantsGrid.addColumn(item -> getUserName(item.user()))
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);
        participantsGrid.addColumn(item -> getLocalizedDateTimeString(item.registered(), getLocale()))
                .setHeader("Registration")
                .setAutoWidth(true)
                .setFlexGrow(0);
        participantsGrid.setItems(participantService.getParticipants(event));

        add(participantsGrid);

        final var participantCount = participantService.getParticipantCount(event);
        add(new Paragraph(getTranslation("participant.boundary.EventParticipantView.participantCount",
                participantCount)));
    }

    private String getUserName(final @NotNull UserDto user) {
        final var name = user.name();
        if (name.isBlank()) {
            return getTranslation("participant.boundary.EventParticipantView.anonymous");
        }
        return name;
    }

}
