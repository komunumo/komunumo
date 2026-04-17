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
import app.komunumo.domain.event.entity.EventWithImageDto;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.infra.ui.vaadin.components.KomunumoMessageBox;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.DateTimeUtil;
import app.komunumo.util.ImageUtil;
import app.komunumo.util.ParseUtil;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Locale;

@Route(value = "events/:eventId", layout = WebsiteLayout.class)
@AnonymousAllowed
public final class EventDetailView extends AbstractView implements BeforeEnterObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDetailView.class);

    private final @NotNull HtmlContainer pageContent = new Div();
    private final @NotNull EventService eventService;
    private final @NotNull ParticipantService participantService;
    private final @NotNull LoginService loginService;

    private @NotNull String pageTitle = "";

    public EventDetailView(final @NotNull ConfigurationService configurationService,
                           final @NotNull EventService eventService,
                           final @NotNull ParticipantService participantService,
                           final @NotNull LoginService loginService) {
        super(configurationService);
        this.eventService = eventService;
        this.participantService = participantService;
        this.loginService = loginService;
        addClassName("event-detail-view");
        add(pageContent);
    }

    @Override
    public void beforeEnter(final @NotNull BeforeEnterEvent beforeEnterEvent) {
        final var params = beforeEnterEvent.getRouteParameters();
        final var eventId = ParseUtil.parseUUID(params.get("eventId"))
                .orElseThrow(() -> new NotFoundException("Invalid event ID!"));
        final var ui = beforeEnterEvent.getUI();
        final var locale = ui.getLocale();

        eventService.getEventWithImage(eventId).ifPresentOrElse(eventWithImage -> {
            showDetails(eventWithImage, locale);
            pageTitle = eventWithImage.event().title();
        }, () -> {
            LOGGER.warn("Event not found with id '{}'!", eventId);
            beforeEnterEvent.rerouteToError(NotFoundException.class);
        });
    }

    private void showDetails(final @NotNull EventWithImageDto eventWithImage,
                             final @NotNull Locale locale) {
        pageContent.removeAll();

        final var event = eventWithImage.event();
        final var image = eventWithImage.image();

        if (image != null) {
            final var imageUrl = ImageUtil.resolveImageUrl(image);
            final var altText = getTranslation(locale, "event.boundary.EventDetailView.image", event.title());
            final var htmlImage = new Image(imageUrl, altText);
            htmlImage.addClassName("event-image");
            pageContent.add(htmlImage);
        }

        final var title = new H2(event.title());
        title.addClassName("event-title");
        pageContent.add(title);

        final var location = new Paragraph(
                getTranslation(locale, "event.boundary.EventDetailView.location") + ": " + event.location());
        location.addClassName("event-location");
        pageContent.add(location);

        addDateTimeText(event.begin(), locale, "event.boundary.EventDetailView.beginDate", "event-date-begin");
        addDateTimeText(event.end(), locale, "event.boundary.EventDetailView.endDate", "event-date-end");

        final var description = new Markdown(event.description());
        description.addClassName("event-description");
        pageContent.add(description);

        final var participantCount = this.participantService.getParticipantCount(event);
        final var loggedInUser = loginService.getLoggedInUser();
        if (loggedInUser.isPresent() && eventService.hasManagementPermission(event, loggedInUser.orElseThrow())) {
            final var participantLink = new Anchor(LinkUtil.getLink(event) + "/participants",
                    getTranslation("event.boundary.EventDetailView.participantCount", participantCount));
            final var participantParagraph = new Paragraph(participantLink);
            participantParagraph.addClassName("event-participant-count");
            pageContent.add(participantParagraph);
        } else {
            final var participantParagraph = new Paragraph(
                    getTranslation("event.boundary.EventDetailView.participantCount", participantCount));
            participantParagraph.addClassName("event-participant-count");
            pageContent.add(participantParagraph);
        }

        if (eventService.isRegistrationAllowed(event)) {
            createRegistrationButtons(eventWithImage, locale);
        }
    }

    private void createRegistrationButtons(final @NotNull EventWithImageDto eventWithImage, final @NonNull Locale locale) {
        final var event = eventWithImage.event();
        final var loggedInUser = loginService.getLoggedInUser();
        final var isParticipant = participantService.isLoggedInUserParticipantOf(event);

        if (isParticipant) {
            final var unregisterButton = new Button(getTranslation("event.boundary.EventDetailView.unregister"));
            unregisterButton.addClickListener(_ -> {
                final var confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(event.title());
                confirmDialog.setText(getTranslation("event.boundary.EventDetailView.unregisterConfirmation"));
                confirmDialog.setCancelable(true);
                confirmDialog.setCancelText(getTranslation("common.button.no"));
                confirmDialog.setConfirmButton(getTranslation("common.button.yes"), _ -> {
                    participantService.unregisterFromEvent(loggedInUser.orElseThrow(), event, locale);
                    showDetails(eventWithImage, locale); // update view
                });
                confirmDialog.open();
            });
            unregisterButton.addClassName("leave-button");
            pageContent.add(unregisterButton);
        } else if (loggedInUser.isEmpty() && !event.anonymousParticipationAllowed()) {
            final var registrationHint = new KomunumoMessageBox(
                    getTranslation("event.boundary.EventDetailView.registrationRequiresAccount"));
            registrationHint.addClassName("registration-required-hint");
            pageContent.add(registrationHint);
        } else {
            final var registerButton = new Button(getTranslation("event.boundary.EventDetailView.register"));
            registerButton.addClickListener(_ -> {
                if (loggedInUser.isPresent()) {
                    final var confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(event.title());
                    confirmDialog.setText(getTranslation("event.boundary.EventDetailView.registerConfirmation"));
                    confirmDialog.setCancelable(true);
                    confirmDialog.setCancelText(getTranslation("common.button.no"));
                    confirmDialog.setConfirmButton(getTranslation("common.button.yes"), _ -> {
                        participantService.registerForEvent(event, loggedInUser.orElseThrow(), locale);
                        showDetails(eventWithImage, locale); // update view
                    });
                    confirmDialog.open();
                } else {
                    participantService.startConfirmationProcess(event, locale);
                }
            });
            registerButton.addClassName("registration-button");
            pageContent.add(registerButton);
        }
    }

    private void addDateTimeText(final @Nullable ZonedDateTime dateTime,
                                 final @NotNull Locale locale,
                                 final @NotNull String translationKey,
                                 final @NotNull String className) {
        final var label = getTranslation(locale, translationKey);
        final var localizedEndDate = DateTimeUtil.getLocalizedDateTimeString(dateTime, locale);
        final var paragraph = new Paragraph(label + ": " + localizedEndDate);
        paragraph.addClassName(className);
        pageContent.add(paragraph);
    }

    @Override
    protected @NotNull String getViewTitle() {
        return pageTitle;
    }

}
