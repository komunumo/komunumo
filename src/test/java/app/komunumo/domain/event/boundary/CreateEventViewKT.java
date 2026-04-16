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
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.infra.ui.vaadin.components.ImageUpload;
import app.komunumo.infra.ui.vaadin.components.MarkdownEditor;
import app.komunumo.test.KaribuTest;
import app.komunumo.test.TestConstants;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class CreateEventViewKT extends KaribuTest {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private UserService userService;

    @Test
    @SuppressWarnings("unchecked")
    void testView() {
        login(getTestUser(UserRole.USER));

        UI.getCurrent().navigate(CreateEventView.class);
        final var view = _get(VerticalLayout.class, spec -> spec.withClasses("create-event-view"));
        assertThat(view).isNotNull();

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        assertThat(communitySelector).isNotNull();

        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        assertThat(titleField).isNotNull();

        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        assertThat(descriptionField).isNotNull();

        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        assertThat(locationField).isNotNull();

        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        assertThat(beginDateTimeField).isNotNull();

        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        assertThat(endDateTimeField).isNotNull();

        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        assertThat(timeZoneSelector).isNotNull();

        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        assertThat(imageField).isNotNull();

        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        assertThat(visibilitySelector).isNotNull();

        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        assertThat(statusSelector).isNotNull();

        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));
        assertThat(createButton).isNotNull();
    }

    @Test
    void testCreate_invalidSubmission() {
        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateEventView.class);

        final var component = _get(CreateEventView.class);
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));
        createButton.click();

        assertThat(component.getBinder().isValid()).isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void testCreate_successfulSubmission_withoutImage() {
        final var testUser = getTestUser(UserRole.USER);
        final var communityWithoutImage = communityService.getCommunitiesForManager(testUser)
                .stream()
                .filter(community -> community.imageId() == null)
                .findFirst()
                .orElseThrow();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var view = _get(CreateEventView.class);

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        communitySelector.setValue(communityWithoutImage);
        titleField.setValue("Test Event");
        descriptionField.setValue("Test Event Description");
        locationField.setValue("Test Location");
        beginDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 8, 0));
        endDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 10, 0));
        timeZoneSelector.setValue(ZoneId.of("UTC"));
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        statusSelector.setValue(EventStatus.PUBLISHED);

        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .startsWith("events/");
        assertThat(_get(H2.class, spec -> spec.withClasses("event-title")).getText())
                .isEqualTo("Test Event");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-location")).getText())
                .isEqualTo("Location: Test Location");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-begin")).getText())
                .isEqualTo("Begin: Monday, July 1, 2030, 8:00 AM UTC");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-end")).getText())
                .isEqualTo("End: Monday, July 1, 2030, 10:00 AM UTC");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("event-description")).getContent())
                .isEqualTo("Test Event Description");
        assertThat(_find(Image.class, spec -> spec.withClasses("event-image")))
                .isEmpty();
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-participant-count"))
                .getChildren().findFirst().orElseThrow().getElement().getText())
                .isEqualTo("no participants");
    }

    @Test
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    void testCreate_successfulSubmission_withoutImageUsesCommunityImage() {
        final var testUser = getTestUser(UserRole.USER);
        final var communityWithImage = communityService.getCommunitiesForManager(testUser)
                .stream()
                .filter(community -> community.imageId() != null)
                .findFirst()
                .orElseThrow();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var view = _get(CreateEventView.class);

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        communitySelector.setValue(communityWithImage);
        titleField.setValue("Test Event");
        descriptionField.setValue("Test Event Description");
        locationField.setValue("Test Location");
        beginDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 8, 0));
        endDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 10, 0));
        timeZoneSelector.setValue(ZoneId.of("UTC"));
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        statusSelector.setValue(EventStatus.PUBLISHED);

        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .startsWith("events/");
        assertThat(_get(H2.class, spec -> spec.withClasses("event-title")).getText())
                .isEqualTo("Test Event");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-location")).getText())
                .isEqualTo("Location: Test Location");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-begin")).getText())
                .isEqualTo("Begin: Monday, July 1, 2030, 8:00 AM UTC");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-end")).getText())
                .isEqualTo("End: Monday, July 1, 2030, 10:00 AM UTC");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("event-description")).getContent())
                .isEqualTo("Test Event Description");
        assertThat(_get(Image.class, spec -> spec.withClasses("event-image")).getSrc())
                .isEqualTo("/images/%s.svg".formatted(communityWithImage.imageId().toString()));
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-participant-count"))
                .getChildren().findFirst().orElseThrow().getElement().getText())
                .isEqualTo("no participants");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreate_successfulSubmission_withImage() {
        final var imageId = imageService.getAllImageIds().getFirst();
        final var imageDto = imageService.getImage(imageId).orElseThrow();
        final var testUser = getTestUser(UserRole.USER);
        final var community = communityService.getCommunitiesForManager(testUser).getFirst();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var view = _get(CreateEventView.class);

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        communitySelector.setValue(community);
        titleField.setValue("Test Event");
        descriptionField.setValue("Test Event Description");
        locationField.setValue("Test Location");
        beginDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 8, 0));
        endDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 10, 0));
        timeZoneSelector.setValue(ZoneId.of("UTC"));
        imageField.setValue(imageDto);
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        statusSelector.setValue(EventStatus.PUBLISHED);

        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .startsWith("events/");
        assertThat(_get(H2.class, spec -> spec.withClasses("event-title")).getText())
                .isEqualTo("Test Event");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-location")).getText())
                .isEqualTo("Location: Test Location");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-begin")).getText())
                .isEqualTo("Begin: Monday, July 1, 2030, 8:00 AM UTC");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-end")).getText())
                .isEqualTo("End: Monday, July 1, 2030, 10:00 AM UTC");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("event-description")).getContent())
                .isEqualTo("Test Event Description");
        assertThat(_get(Image.class, spec -> spec.withClasses("event-image")).getSrc())
                .isEqualTo("/images/%s.svg".formatted(imageId));
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-participant-count"))
                .getChildren().findFirst().orElseThrow().getElement().getText())
                .isEqualTo("no participants");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreate_successfulSubmission_setAndUnsetImage() {
        final var imageId = imageService.getAllImageIds().getFirst();
        final var imageDto = imageService.getImage(imageId).orElseThrow();
        final var testUser = getTestUser(UserRole.USER);
        final var communityWithoutImage = communityService.getCommunitiesForManager(testUser)
                .stream()
                .filter(community -> community.imageId() == null)
                .findFirst()
                .orElseThrow();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var view = _get(CreateEventView.class);

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        communitySelector.setValue(communityWithoutImage);
        titleField.setValue("Test Event");
        descriptionField.setValue("Test Event Description");
        locationField.setValue("Test Location");
        beginDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 8, 0));
        endDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 10, 0));
        timeZoneSelector.setValue(ZoneId.of("UTC"));
        imageField.setValue(imageDto);
        imageField.setValue(null);
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        statusSelector.setValue(EventStatus.PUBLISHED);

        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .startsWith("events/");
        assertThat(_get(H2.class, spec -> spec.withClasses("event-title")).getText())
                .isEqualTo("Test Event");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-location")).getText())
                .isEqualTo("Location: Test Location");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-begin")).getText())
                .isEqualTo("Begin: Monday, July 1, 2030, 8:00 AM UTC");
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-date-end")).getText())
                .isEqualTo("End: Monday, July 1, 2030, 10:00 AM UTC");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("event-description")).getContent())
                .isEqualTo("Test Event Description");
        assertThat(_find(Image.class, spec -> spec.withClasses("event-image")))
                .isEmpty();
        assertThat(_get(Paragraph.class, spec -> spec.withClasses("event-participant-count"))
                .getChildren().findFirst().orElseThrow().getElement().getText())
                .isEqualTo("no participants");
    }

    @Test
    void testCreate_beginEndDateTimeValidation() {
        final var testUser = getTestUser(UserRole.USER);
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));

        // empty form = no date values
        assertThat(beginDateTimeField.getValue()).isNull();
        assertThat(endDateTimeField.getValue()).isNull();

        // set end to today should not modify begin
        final var today = LocalDateTime.now().withSecond(0).withNano(0);
        endDateTimeField.setValue(today);
        assertThat(beginDateTimeField.getValue()).isNull();
        assertThat(endDateTimeField.getValue()).isEqualTo(today);

        // set begin to tomorrow (after end) should update end to match begin
        final var tomorrow = today.plusDays(1);
        beginDateTimeField.setValue(tomorrow);
        assertThat(beginDateTimeField.getValue()).isEqualTo(tomorrow);
        assertThat(endDateTimeField.getValue()).isEqualTo(tomorrow);

        // set begin to date after end should update end to match begin
        final var dayAfterTomorrow = tomorrow.plusDays(1);
        beginDateTimeField.setValue(dayAfterTomorrow);
        assertThat(beginDateTimeField.getValue()).isEqualTo(dayAfterTomorrow);
        assertThat(endDateTimeField.getValue()).isEqualTo(dayAfterTomorrow);

        // set begin back to tomorrow should NOT update end
        beginDateTimeField.setValue(tomorrow);
        assertThat(beginDateTimeField.getValue()).isEqualTo(tomorrow);
        assertThat(endDateTimeField.getValue()).isEqualTo(dayAfterTomorrow);

        // set begin to null should NOT update end
        beginDateTimeField.setValue(null);
        assertThat(beginDateTimeField.getValue()).isNull();
        assertThat(endDateTimeField.getValue()).isEqualTo(dayAfterTomorrow);

        // set end to null
        endDateTimeField.setValue(null);
        assertThat(beginDateTimeField.getValue()).isNull();
        assertThat(endDateTimeField.getValue()).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    void testCreate_prefillCommunityFromUrl() {
        final var testUser = getTestUser(UserRole.USER);
        final var community = communityService.getCommunitiesForManager(testUser).getFirst();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class, QueryParameters.of("communityId", community.id().toString()));

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        assertThat(communitySelector.getValue()).isEqualTo(community);
    }

    @Test
    void userWithoutRights_showsPermissionError() {
        final var testUser = userService.getUserById(TestConstants.USER_ID_MEMBER).orElseThrow();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var errorTitle = _get(H2.class, spec -> spec.withClasses("error-title"));
        assertThat(errorTitle.getText()).isEqualTo("Access Denied");

        final var errorMessage = _get(Paragraph.class, spec -> spec.withClasses("error-message"));
        assertThat(errorMessage.getText()).startsWith("You do not have permission to create a new event for any community.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sessionTimeout() {
        final var testUser = getTestUser(UserRole.USER);
        final var communityWithoutImage = communityService.getCommunitiesForManager(testUser)
                .stream()
                .filter(community -> community.imageId() == null)
                .findFirst()
                .orElseThrow();
        login(testUser);
        UI.getCurrent().navigate(CreateEventView.class);

        final var view = _get(CreateEventView.class);

        final var communitySelector = (Select<CommunityDto>) _get(Select.class, spec -> spec.withClasses("community-field"));
        final var titleField = _get(TextField.class, spec -> spec.withClasses("title-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var locationField = _get(TextField.class, spec -> spec.withClasses("location-field"));
        final var beginDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("begin-field"));
        final var endDateTimeField = _get(DateTimePicker.class, spec -> spec.withClasses("end-field"));
        final var timeZoneSelector = (Select<ZoneId>) _get(Select.class, spec -> spec.withClasses("time-zone-field"));
        final var visibilitySelector = (Select<EventVisibility>) _get(Select.class, spec -> spec.withClasses("visibility-field"));
        final var statusSelector = (Select<EventStatus>) _get(Select.class, spec -> spec.withClasses("status-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        communitySelector.setValue(communityWithoutImage);
        titleField.setValue("Test Event");
        descriptionField.setValue("Test Event Description");
        locationField.setValue("Test Location");
        beginDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 8, 0));
        endDateTimeField.setValue(LocalDateTime.of(2030, 7, 1, 10, 0));
        timeZoneSelector.setValue(ZoneId.of("UTC"));
        visibilitySelector.setValue(EventVisibility.PUBLIC);
        statusSelector.setValue(EventStatus.PUBLISHED);

        assertThat(view.getBinder().isValid()).isTrue();

        logout(); // simulate session timeout by logging out

        createButton.click();

        final var notification = _get(Notification.class);
        assertThat(notification.isOpened()).isTrue();

        final var notificationDiv = _get(notification, Div.class);
        assertThat(notificationDiv.getText()).startsWith("You do not have permission to create a new event.");
    }

}
