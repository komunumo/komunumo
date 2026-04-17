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

import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.test.BrowserTest;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.icegreen.greenmail.util.GreenMailUtil.getBody;
import static org.assertj.core.api.Assertions.assertThat;

class EventCancelRegistrationFlowBT extends BrowserTest {

    protected static final UUID UUID_EVENT = UUID.fromString("3609c5c1-536a-46b6-8b6b-7834d9cc9669");
    protected static final UUID UUID_USER = UUID.fromString("c9fc8b0a-6ff7-4c00-a6f2-d85f5829edff");

    private static final String CANCEL_REGISTRATION_BUTTON_SELECTOR = "vaadin-button:has-text('Cancel Registration')";

    @Autowired
    private @NotNull EventService eventService;

    @Autowired
    private @NotNull UserService userService;

    @Autowired
    private @NotNull ParticipantService participantService;

    private EventDto demoEvent;
    private UserDto demoUser;

    @BeforeEach
    void setUp() {
        demoEvent = eventService.getEvent(UUID_EVENT).orElseThrow();
        demoUser = userService.getUserById(UUID_USER).orElseThrow();
        participantService.storeParticipant(new ParticipantDto(UUID_EVENT, UUID_USER, null));
    }

    @Test
    void cancelRegistrationWhenLoggedIn_confirmNo() {
        // login member
        login(demoUser);

        // navigate to events page
        final var page = getPage();
        page.navigate(getInstanceUrl() + "events/" + demoEvent.id());
        page.waitForURL("**/events/" + demoEvent.id());
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmNo_eventDetails");

        // user is a member
        assertThat(participantService.isParticipant(demoUser, demoEvent)).isTrue();
        assertThatLocator(page.locator(".event-participant-count")).hasText("one participant");

        // click on registration button
        page.click(CANCEL_REGISTRATION_BUTTON_SELECTOR);
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmNo_dialogOpened");

        // don't confirm the unregister dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("No")
        ).click();
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmNo_dialogConfirmed");

        // user is still a member
        assertThat(participantService.isParticipant(demoUser, demoEvent)).isTrue();
        assertThatLocator(page.locator(".event-participant-count")).hasText("one participant");

        // logout the test user
        logout();
    }

    @Test
    void cancelRegistrationWhenLoggedIn_confirmYes() {
        // login member
        login(demoUser);

        // navigate to events page
        final var page = getPage();
        page.navigate(getInstanceUrl() + "events/" + demoEvent.id());
        page.waitForURL("**/events/" + demoEvent.id());
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmYes_eventDetails");

        // user is a member
        assertThat(participantService.isParticipant(demoUser, demoEvent)).isTrue();
        assertThatLocator(page.locator(".event-participant-count")).hasText("one participant");

        // click on registration button
        page.click(CANCEL_REGISTRATION_BUTTON_SELECTOR);
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmYes_dialogOpened");

        // confirm the unregister dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).click();
        captureScreenshot("cancelRegistrationWhenLoggedIn_confirmYes_dialogConfirmed");

        // user is not a member anymore
        assertThat(participantService.isParticipant(demoUser, demoEvent)).isFalse();
        assertThatLocator(page.locator(".event-participant-count")).hasText("no participants");

        // wait for unregistration confirmation mail
        final var successMessage = getEmailBySubject("[Komunumo Test] You canceled your registration");
        assertThat(getBody(successMessage)).contains("You are no longer signed up for the event anymore.");

        // logout the test user
        logout();
    }

}
