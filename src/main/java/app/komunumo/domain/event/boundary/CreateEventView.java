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

import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.infra.ui.vaadin.components.ImageUpload;
import app.komunumo.infra.ui.vaadin.components.MarkdownEditor;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.SecurityUtil;
import app.komunumo.util.TimeZoneUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static app.komunumo.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.notification.NotificationVariant.ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.WARNING;

@RolesAllowed("USER_LOCAL")
@Route(value = "events/new", layout = WebsiteLayout.class)
public final class CreateEventView extends AbstractView implements AfterNavigationObserver {

    private final @NotNull LoginService loginService;
    private final @NotNull CommunityService communityService;
    private final @NotNull EventService eventService;

    private final @NotNull Select<CommunityDto> communitySelector;
    private final @NotNull TextField titleField;
    private final @NotNull MarkdownEditor descriptionField;
    private final @NotNull TextField locationField;
    private final @NotNull DateTimePicker beginDateTimeField;
    private final @NotNull DateTimePicker endDateTimeField;
    private final @NotNull Select<ZoneId> timeZoneSelector;
    private final @NotNull ImageUpload imageField;
    private final @NotNull Checkbox anonymousParticipationAllowed;
    private final @NotNull Select<EventVisibility> visibilitySelector;
    private final @NotNull Select<EventStatus> statusSelector;
    private final @NotNull Button createEventbutton;
    private final @NotNull Binder<EventDto> binder;

    public CreateEventView(final @NotNull ConfigurationService configurationService,
                           final @NotNull LoginService loginService,
                           final @NotNull CommunityService communityService,
                           final @NotNull ImageService imageService,
                           final @NotNull EventService eventService) {
        super(configurationService);
        this.loginService = loginService;
        this.communityService = communityService;
        this.eventService = eventService;

        this.communitySelector = new Select<>();
        this.titleField = new TextField();
        this.descriptionField = new MarkdownEditor(getLocale());
        this.locationField = new TextField();
        this.beginDateTimeField = new DateTimePicker();
        this.endDateTimeField = new DateTimePicker();
        this.timeZoneSelector = new Select<>();
        this.imageField = new ImageUpload(imageService);
        this.anonymousParticipationAllowed = new Checkbox();
        this.visibilitySelector = new Select<>();
        this.statusSelector = new Select<>();
        this.createEventbutton = new Button();
        this.binder = new Binder<>(EventDto.class);

        addClassName("create-event-view");
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("event.boundary.CreateEventView.title");
    }

    @Override
    public void afterNavigation(final @NotNull AfterNavigationEvent afterNavigationEvent) {
        removeAll();
        final var loggedInUser = loginService.getLoggedInUser().orElseThrow();
        final var communities = communityService.getCommunitiesForManager(loggedInUser);
        if (communities.isEmpty()) {
            showAccessDeniedMessage();
        } else {
            final var communityId = getCommunityIdFromParam(afterNavigationEvent);
            showNewEventForm(communityId, communities);
        }
    }

    private @Nullable UUID getCommunityIdFromParam(final @NotNull AfterNavigationEvent afterNavigationEvent) {
        final @Nullable var paramContent = afterNavigationEvent
                .getLocation()
                .getQueryParameters()
                .getSingleParameter("communityId")
                .orElse(null);
        if (paramContent != null) {
            return UUID.fromString(paramContent);
        }
        return null;
    }

    private void showAccessDeniedMessage() {
        final var errorTitle = new H2();
        errorTitle.addClassName("error-title");
        errorTitle.setText(getTranslation("event.boundary.CreateEventView.error.access.title"));

        final var errorMessage = new Paragraph();
        errorMessage.addClassName("error-message");
        errorMessage.setText(getTranslation("event.boundary.CreateEventView.error.access.message"));

        add(errorTitle, errorMessage);
    }

    private void showNewEventForm(final @Nullable UUID communityId,
                                  final @NotNull List<@NotNull CommunityDto> communities) {
        add(new H2(getTranslation("event.boundary.CreateEventView.title")));

        communitySelector.addClassName("community-field");
        communitySelector.setItemLabelGenerator(CommunityDto::name);
        communitySelector.setItems(communities);
        communitySelector.setEmptySelectionAllowed(false);
        communitySelector.setRequiredIndicatorVisible(true);
        communitySelector.setLabel(getTranslation("event.boundary.CreateEventView.label.community"));
        communitySelector.setWidthFull();
        if (communityId != null) {
            communities.stream()
                    .filter(community -> communityId.equals(community.id()))
                    .findFirst()
                    .ifPresent(communitySelector::setValue);
        }
        add(communitySelector);

        titleField.addClassName("title-field");
        titleField.setValueChangeMode(ValueChangeMode.EAGER);
        titleField.setLabel(getTranslation("event.boundary.CreateEventView.label.title"));
        titleField.setRequired(true);
        titleField.setMaxLength(255);
        titleField.setWidthFull();
        add(titleField);

        descriptionField.addClassName("description-field");
        descriptionField.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionField.setLabel(getTranslation("event.boundary.CreateEventView.label.description"));
        descriptionField.setWidthFull();
        add(descriptionField);

        locationField.addClassName("location-field");
        locationField.setValueChangeMode(ValueChangeMode.EAGER);
        locationField.setLabel(getTranslation("event.boundary.CreateEventView.label.location"));
        locationField.setMaxLength(255);
        locationField.setWidthFull();
        add(locationField);

        beginDateTimeField.addClassName("begin-field");
        beginDateTimeField.setLabel(getTranslation("event.boundary.CreateEventView.label.beginDateTime"));
        beginDateTimeField.setMin(ZonedDateTime.now(TimeZoneUtil.getClientTimeZone()).toLocalDateTime());
        beginDateTimeField.addValueChangeListener(_ -> {
            final var beginDate = beginDateTimeField.getValue();
            if (beginDate != null) {
                endDateTimeField.setMin(beginDate);
                final var endDate = endDateTimeField.getValue();
                if (endDate != null && endDate.isBefore(beginDate)) {
                    endDateTimeField.setValue(beginDate);
                    showNotification(getTranslation("event.boundary.CreateEventView.warning.endDateTimeModified"), WARNING);
                }
            }
        });
        beginDateTimeField.setWidthFull();
        add(beginDateTimeField);

        endDateTimeField.addClassName("end-field");
        endDateTimeField.setLabel(getTranslation("event.boundary.CreateEventView.label.endDateTime"));
        endDateTimeField.setMin(beginDateTimeField.getMin());
        endDateTimeField.setWidthFull();
        add(endDateTimeField);

        timeZoneSelector.addClassName("time-zone-field");
        timeZoneSelector.setItemLabelGenerator(ZoneId::getId);
        timeZoneSelector.setItems(ZoneId.getAvailableZoneIds()
                .stream()
                .sorted()
                .map(ZoneId::of)
                .toList());
        timeZoneSelector.setValue(TimeZoneUtil.getClientTimeZone());
        timeZoneSelector.setEmptySelectionAllowed(false);
        timeZoneSelector.setLabel(getTranslation("event.boundary.CreateEventView.label.timeZone"));
        timeZoneSelector.setWidthFull();
        add(timeZoneSelector);

        imageField.addClassName("image-field");
        imageField.setLabel(getTranslation("event.boundary.CreateEventView.label.image"));
        imageField.setWidthFull();
        add(imageField);

        anonymousParticipationAllowed.addClassName("anonymous-participation-field");
        anonymousParticipationAllowed.setLabel(getTranslation("event.boundary.CreateEventView.label.anonymousParticipationAllowed"));
        anonymousParticipationAllowed.setValue(true);
        anonymousParticipationAllowed.setWidthFull();
        add(anonymousParticipationAllowed);

        visibilitySelector.addClassName("visibility-field");
        visibilitySelector.setItemLabelGenerator(EventVisibility::name);
        visibilitySelector.setItems(EventVisibility.values());
        visibilitySelector.setEmptySelectionAllowed(false);
        visibilitySelector.setLabel(getTranslation("event.boundary.CreateEventView.label.visibility"));
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        visibilitySelector.setRequiredIndicatorVisible(true);
        visibilitySelector.setWidthFull();
        add(visibilitySelector);

        statusSelector.addClassName("status-field");
        statusSelector.setItemLabelGenerator(EventStatus::name);
        statusSelector.setItems(EventStatus.values());
        statusSelector.setEmptySelectionAllowed(false);
        statusSelector.setLabel(getTranslation("event.boundary.CreateEventView.label.status"));
        statusSelector.setValue(EventStatus.DRAFT);
        statusSelector.setRequiredIndicatorVisible(true);
        statusSelector.setWidthFull();
        add(statusSelector);

        createEventbutton.addClassName("create-button");
        createEventbutton.setText(getTranslation("event.boundary.CreateEventView.button.createEvent"));
        createEventbutton.addThemeVariants(ButtonVariant.PRIMARY);
        createEventbutton.addClickListener(this::createEvent);
        add(createEventbutton);

        binder.forField(titleField)
                .asRequired(getTranslation("event.boundary.CreateEventView.validation.title.required"))
                .bind(EventDto::title, null);
    }

    private void createEvent(final @NotNull ClickEvent<Button> buttonClickEvent) {
        var userPrincipalOptional = SecurityUtil.getUserPrincipal();

        if (userPrincipalOptional.isEmpty()) {
            showNotification(getTranslation("event.boundary.CreateEventView.notification.permissionError"),
                    ERROR);
        } else if (binder.validate().isOk()) {
            final var communityId = communitySelector.getValue().id();
            final var title = titleField.getValue();
            final var description = descriptionField.getValue();
            final var location = locationField.getValue();
            final var beginDateTime = ZonedDateTime.of(beginDateTimeField.getValue(), timeZoneSelector.getValue());
            final var endDateTime = ZonedDateTime.of(endDateTimeField.getValue(), timeZoneSelector.getValue());
            final var image = imageField.getValue();
            final var imageId = image != null ? image.id() : null;
            final var anonymousParticipation = anonymousParticipationAllowed.getValue();
            final var visibility = visibilitySelector.getValue();
            final var status = statusSelector.getValue();

            final var newEvent = new EventDto(null, communityId, null, null, title, description,
                    location, beginDateTime, endDateTime, imageId, anonymousParticipation, visibility, status);
            final var event = eventService.storeEvent(newEvent);

            showNotification(getTranslation("event.boundary.CreateEventView.notification.success"), SUCCESS);
            UI.getCurrent().navigate("events/%s".formatted(event.id()));
        } else {
            showNotification(getTranslation("event.boundary.CreateEventView.notification.error"), ERROR);
        }
    }

    /**
     * <p>Getter method to retrieve the binder object for this component.</p>
     *
     * @return the binder object
     */
    @VisibleForTesting
    @NotNull Binder<EventDto> getBinder() {
        return binder;
    }

}
