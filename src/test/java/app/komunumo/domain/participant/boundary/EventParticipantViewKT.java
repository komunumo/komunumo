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

import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.boundary.LoginView;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.test.KaribuTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class EventParticipantViewKT extends KaribuTest {

    private static final @NotNull UUID eventId = UUID.fromString("03950951-39e1-429a-b702-5bf210960495");

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private EventService eventService;

    private EventDto testEvent;

    @BeforeEach
    void setUp() {
        testEvent = eventService.getEvent(eventId).orElseThrow();
    }

    @Test
    void accessView_withInvalidEventId_shouldShow404() {
        final var admin = userService.storeUser(new UserDto(null, null, null,
                "testAdmin", "test-admin@example.com", "Test Admin", "", null,
                UserRole.ADMIN, UserType.LOCAL));
        login(admin);

        UI.getCurrent().navigate("events/invalid-id/participants");

        final var title = _get(H2.class, spec -> spec.withText("Page not found"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_withEventIdNotFound_shouldShow404() {
        final var admin = userService.storeUser(new UserDto(null, null, null,
                "testAdmin", "test-admin@example.com", "Test Admin", "", null,
                UserRole.ADMIN, UserType.LOCAL));
        login(admin);

        UI.getCurrent().navigate("events/0212b3bd-5cf1-49d5-8737-8fa3c0d1c8d5/participants");

        final var title = _get(H2.class, spec -> spec.withText("Page not found"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_whenNotLogged_shouldRedirectToLoginView() {
        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var loginView = _get(LoginView.class);
        assertThat(loginView).isNotNull();
    }

    @Test
    void accessView_whenNonMember_shouldShow404() {
        final var nonMember = userService.storeUser(new UserDto(null, null, null,
                "testNonMember", "test-non-member@example.com", "Test User", "", null,
                UserRole.USER, UserType.LOCAL));
        login(nonMember);

        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var title = _get(H2.class, spec -> spec.withText("Page not found"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_whenMember_shouldShow404() {
        final var member = userService.storeUser(new UserDto(null, null, null,
                "testMember", "test-member@example.com", "Test Member", "", null,
                UserRole.USER, UserType.LOCAL));
        assertThat(member.id()).isNotNull();
        assertThat(testEvent.communityId()).isNotNull();
        memberService.storeMember(new MemberDto(member.id(), testEvent.communityId(), MemberRole.MEMBER, null));
        login(member);

        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var title = _get(H2.class, spec -> spec.withText("Page not found"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_whenOrganizer_shouldShowParticipants() {
        final var organizer = userService.storeUser(new UserDto(null, null, null,
                "testOrganizer", "test-organizer@example.com", "Test Organizer", "", null,
                UserRole.USER, UserType.LOCAL));
        assertThat(organizer.id()).isNotNull();
        assertThat(testEvent.communityId()).isNotNull();
        memberService.storeMember(new MemberDto(organizer.id(), testEvent.communityId(), MemberRole.ORGANIZER, null));
        login(organizer);

        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var title = _get(H2.class, spec -> spec.withText("Participants of Demo Event 2"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_whenOwner_shouldShowParticipants() {
        final var owner = userService.storeUser(new UserDto(null, null, null,
                "testOwner", "test-owner@example.com", "Test Owner", "", null,
                UserRole.USER, UserType.LOCAL));
        assertThat(owner.id()).isNotNull();
        assertThat(testEvent.communityId()).isNotNull();
        memberService.storeMember(new MemberDto(owner.id(), testEvent.communityId(), MemberRole.OWNER, null));
        login(owner);

        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var title = _get(H2.class, spec -> spec.withText("Participants of Demo Event 2"));
        assertThat(title).isNotNull();
    }

    @Test
    void accessView_whenAdmin_shouldShowParticipants() {
        final var admin = userService.storeUser(new UserDto(null, null, null,
                "testAmin", "test-admin@example.com", "Test Admin", "", null,
                UserRole.ADMIN, UserType.LOCAL));
        login(admin);

        assertThat(testEvent.id()).isNotNull();
        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        final var title = _get(H2.class, spec -> spec.withText("Participants of Demo Event 2"));
        assertThat(title).isNotNull();
    }

    @Test
    void checkPageContent() {
        final var anonymous = userService.storeUser(new UserDto(null, null, null,
                null, "test-anonymous@example.com", "", "", null,
                UserRole.USER, UserType.ANONYMOUS));
        assertThat(anonymous.id()).isNotNull();
        assertThat(testEvent.id()).isNotNull();
        participantService.storeParticipant(new ParticipantDto(testEvent.id(), anonymous.id(), null));

        final var admin = userService.storeUser(new UserDto(null, null, null,
                "testAdmin", "test-admin@example.com", "Test Admin", "", null,
                UserRole.ADMIN, UserType.LOCAL));
        login(admin);

        UI.getCurrent().navigate("events/" + testEvent.id() + "/participants");

        // check page title
        final var title = _get(H2.class, spec -> spec.withText("Participants of Demo Event 2"));
        assertThat(title).isNotNull();

        // check back link
        final var backLink = _get(Anchor.class, spec -> spec.withText("Back to Event"));
        assertThat(backLink).isNotNull();
        assertThat(backLink.getHref()).isEqualTo("/events/" + testEvent.id());

        // check participant grid
        @SuppressWarnings("unchecked") final var grid =
                (Grid<RegisteredParticipantDto>) _get(Grid.class, spec -> spec.withClasses("participants-grid"));
        assertThat(grid).isNotNull();

        final var items = grid.getListDataView().getItems().toList();
        assertThat(items).hasSize(4);

        assertThat(items)
                .extracting(item -> item.user().name())
                .containsExactly("Udo User", "", "Armin Admin", "Rita Remote");

        // check participant count
        final var participantCount = _get(Paragraph.class, spec -> spec.withText("Participant count: 4"));
        assertThat(participantCount).isNotNull();
    }
}
