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
package app.komunumo.domain.demo.control;

import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.demo.control.DemoMode;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.page.entity.GlobalPageDto;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.test.KaribuTest;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;
import java.util.UUID;

import static app.komunumo.data.db.Tables.MAIL_TEMPLATE;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class DemoModeKT extends KaribuTest {

    @Autowired
    private @NotNull ConfigurationService configurationService;

    @Autowired
    private @NotNull ImageService imageService;

    @Autowired
    private @NotNull UserService userService;

    @Autowired
    private @NotNull CommunityService communityService;

    @Autowired
    private @NotNull MemberService memberService;

    @Autowired
    private @NotNull EventService eventService;

    @Autowired
    private @NotNull ParticipantService participantService;

    @Autowired
    private @NotNull GlobalPageService globalPageService;

    @Autowired
    private @NotNull MailService mailService;

    @Autowired
    private @NotNull DSLContext dsl;

    @Autowired
    private @NotNull DemoMode demoMode;

    @Test
    void resetDemoData() {
        assertThat(configurationService.getConfigurationCount()).isEqualTo(4);
        assertThat(imageService.getImageCount()).isEqualTo(2);
        assertThat(userService.getAdminCount()).isOne();
        assertThat(userService.getUserCount()).isEqualTo(4);
        assertThat(communityService.getCommunityCount()).isEqualTo(6);
        assertThat(memberService.getMemberCount()).isEqualTo(24);
        assertThat(eventService.getEventCount()).isEqualTo(6);
        assertThat(participantService.getParticipantCount()).isEqualTo(6);
        assertThat(globalPageService.getGlobalPageCount()).isEqualTo(2);
        assertThat(mailService.getMailTemplateCount()).isEqualTo(20);

        imageService.storeImage(new ImageDto(null, ContentType.IMAGE_PNG));
        assertThat(imageService.getImageCount()).isEqualTo(3);

        userService.storeUser(new UserDto(null, null, null,
                "demoAdmin", "demo-admin@example.com", "Demo Admin", "", null,
                UserRole.ADMIN, UserType.LOCAL));
        assertThat(userService.getAdminCount()).isEqualTo(2);

        final var newUser = userService.storeUser(new UserDto(null, null, null,
                "demoUser", "demo-user@example.com", "Demo User", "", null,
                UserRole.USER, UserType.LOCAL));
        assertThat(userService.getUserCount()).isEqualTo(5);

        final var community = communityService.storeCommunity(new CommunityDto(null, "demoCommunity", null, null,
                "Demo Community", "", null));
        assertThat(communityService.getCommunityCount()).isEqualTo(7);

        memberService.getMembersByCommunityId(UUID.fromString("9a73690b-6dbd-456a-88e9-dc3f77b69aa0"))
                .forEach(member -> memberService.deleteMember(member));
        assertThat(memberService.getMemberCount()).isEqualTo(20);

        final var newEvent = eventService.storeEvent(new EventDto(null, community.id(), null, null,
                "Demo Event", "", "", null, null, null,
                true, EventVisibility.PUBLIC, EventStatus.DRAFT));
        assertThat(eventService.getEventCount()).isEqualTo(7);

        participantService.storeParticipant(
                new ParticipantDto(
                        requireNonNull(newEvent.id()),
                        requireNonNull(newUser.id()),
                        null));
        assertThat(participantService.getParticipantCount()).isEqualTo(7);

        globalPageService.storeGlobalPage(new GlobalPageDto("demo", Locale.ENGLISH, null, null,
                "Demo Page", "**Demo Page**"));
        assertThat(globalPageService.getGlobalPageCount()).isEqualTo(3);

        dsl.deleteFrom(MAIL_TEMPLATE)
                .where(MAIL_TEMPLATE.ID.eq("TEST"))
                .execute();
        assertThat(mailService.getMailTemplateCount()).isEqualTo(18);

        demoMode.resetDemoData();

        assertThat(configurationService.getConfigurationCount()).isEqualTo(4);
        assertThat(imageService.getImageCount()).isEqualTo(2);
        assertThat(userService.getAdminCount()).isOne();
        assertThat(userService.getUserCount()).isEqualTo(4);
        assertThat(communityService.getCommunityCount()).isEqualTo(6);
        assertThat(memberService.getMemberCount()).isEqualTo(24);
        assertThat(eventService.getEventCount()).isEqualTo(6);
        assertThat(participantService.getParticipantCount()).isEqualTo(6);
        assertThat(globalPageService.getGlobalPageCount()).isEqualTo(2);
        assertThat(mailService.getMailTemplateCount()).isEqualTo(20);
    }

}
