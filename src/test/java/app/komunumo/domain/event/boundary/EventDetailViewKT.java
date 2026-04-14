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

import app.komunumo.domain.core.confirmation.boundary.ConfirmationDialog;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventWithImageDto;
import app.komunumo.infra.ui.vaadin.components.KomunumoMessageBox;
import app.komunumo.test.KaribuTest;
import app.komunumo.util.DateTimeUtil;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.markdown.Markdown;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

import static app.komunumo.test.TestUtil.findComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class EventDetailViewKT extends KaribuTest {

    @Autowired
    private EventService eventService;

    @Test
    @SuppressWarnings("DataFlowIssue")
    void eventWithImage() {
        final var testEventWithImage = eventService.getUpcomingEventsWithImage()
                .stream()
                .filter(eventWithImage -> eventWithImage.image() != null)
                .findAny()
                .orElseThrow();
        final var testEvent = testEventWithImage.event();
        final var testImage = testEventWithImage.image();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var title = _get(H2.class, spec -> spec.withClasses("event-title"));
        assertThat(title).isNotNull();
        assertThat(title.getText()).isEqualTo(testEvent.title());

        final var location = _get(Paragraph.class, spec -> spec.withClasses("event-location"));
        assertThat(location).isNotNull();
        assertThat(location.getText()).isEqualTo("Location: " + testEvent.location());

        final var beginDate = _get(Paragraph.class, spec -> spec.withClasses("event-date-begin"));
        assertThat(beginDate).isNotNull();
        assertThat(beginDate.getText()).isEqualTo("Begin: " +
                DateTimeUtil.getLocalizedDateTimeString(testEvent.begin(), Locale.ENGLISH));

        final var endDate = _get(Paragraph.class, spec -> spec.withClasses("event-date-end"));
        assertThat(endDate).isNotNull();
        assertThat(endDate.getText()).isEqualTo("End: " +
                DateTimeUtil.getLocalizedDateTimeString(testEvent.end(), Locale.ENGLISH));

        final var description = _get(Markdown.class, spec -> spec.withClasses("event-description"));
        assertThat(description).isNotNull();
        assertThat(description.getContent()).isEqualTo(testEvent.description());

        final var participantCount = _get(Paragraph.class, spec -> spec.withClasses("event-participant-count"));
        assertThat(participantCount).isNotNull();
        assertThat(participantCount.getText()).isEqualTo("no participants");

        final var image = _get(Image.class, spec -> spec.withClasses("event-image"));
        assertThat(image).isNotNull();
        assertThat(image.getSrc()).isNotBlank().isEqualTo("/images/" + testImage.id() + ".svg");
        assertThat(image.getAlt().orElseThrow()).isEqualTo("Event image for: " + testEvent.title());
    }

    @Test
    void eventWithoutImage() {
        final var testEventWithImage = eventService.getUpcomingEventsWithImage()
                .stream()
                .filter(eventWithImage -> eventWithImage.image() == null)
                .findAny()
                .orElseThrow();
        final var testEvent = testEventWithImage.event();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var title = _get(H2.class, spec -> spec.withClasses("event-title"));
        assertThat(title).isNotNull();
        assertThat(title.getText()).isEqualTo(testEvent.title());

        final var image = _find(Image.class, spec -> spec.withClasses("event-image"));
        assertThat(image).isEmpty();
    }

    @Test
    void anonymousVisitorCanRegisterWhenAnonymousParticipationIsAllowed() {
        final var testEvent = eventService.getUpcomingEventsWithImage()
                .stream()
                .map(EventWithImageDto::event)
                .filter(EventDto::anonymousParticipationAllowed)
                .findAny()
                .orElseThrow();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var registerButton = _find(Button.class, spec -> spec.withClasses("registration-button"));
        assertThat(registerButton).hasSize(1);

        final var registrationHint = _find(KomunumoMessageBox.class, spec -> spec.withClasses("registration-required-hint"));
        assertThat(registrationHint).isEmpty();
    }

    @Test
    void anonymousVisitorSeesHintWhenAnonymousParticipationIsNotAllowed() {
        final var testEvent = eventService.getUpcomingEventsWithImage()
                .stream()
                .map(EventWithImageDto::event)
                .filter(event -> !event.anonymousParticipationAllowed())
                .findAny()
                .orElseThrow();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var registerButton = _find(Button.class, spec -> spec.withClasses("registration-button"));
        assertThat(registerButton).isEmpty();

        final var registrationHint = _get(KomunumoMessageBox.class, spec -> spec.withClasses("registration-required-hint"));
        final var markdown = _get(registrationHint, Markdown.class);
        assertThat(markdown.getContent())
                .isEqualTo("You need to [register](register) and [sign in](login) to participate in this event.");
    }

    @Test
    void nonExistingEvent() {
        UI.getCurrent().navigate("events/00000000-0000-0000-0000-000000000000");

        final var main = _get(Main.class);
        final var h2 = findComponent(main, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("Page not found");
    }

    @Test
    void invalidEventId() {
        UI.getCurrent().navigate("events/invalid");

        final var main = _get(Main.class);
        final var h2 = findComponent(main, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("Page not found");
    }

    @Test
    void registerDialogIsClosable() {
        final var testEvent = eventService.getUpcomingEventsWithImage()
                .stream()
                .map(EventWithImageDto::event)
                .filter(EventDto::anonymousParticipationAllowed)
                .findAny()
                .orElseThrow();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var registerButton = _get(Button.class, spec -> spec.withClasses("registration-button"));
        _click(registerButton);

        MockVaadin.clientRoundtrip(false);

        final var registerDialog = _get(ConfirmationDialog.class);
        assertThat(registerDialog).isNotNull();
        assertThat(registerDialog.isOpened()).isTrue();

        final var closeButton = _get(registerDialog, Button.class, spec -> spec.withClasses("close-dialog-button"));
        assertThat(closeButton).isNotNull();
        _click(closeButton);

        MockVaadin.clientRoundtrip(false);

        assertThat(registerDialog.isOpened()).isFalse();
    }

    @Test
    void loginDialogCanBeCanceled() {
        final var testEvent = eventService.getUpcomingEventsWithImage()
                .stream()
                .map(EventWithImageDto::event)
                .filter(EventDto::anonymousParticipationAllowed)
                .findAny()
                .orElseThrow();

        UI.getCurrent().navigate("events/" + testEvent.id());

        final var registerButton = _get(Button.class, spec -> spec.withClasses("registration-button"));
        _click(registerButton);

        MockVaadin.clientRoundtrip(false);

        final var registerDialog = _get(ConfirmationDialog.class);
        assertThat(registerDialog.isOpened()).isTrue();

        final var cancelButton = _get(registerDialog, Button.class, spec -> spec.withText("Cancel"));
        assertThat(cancelButton).isNotNull();
        _click(cancelButton);

        MockVaadin.clientRoundtrip(false);

        assertThat(registerDialog.isOpened()).isFalse();
    }
}
