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
package app.komunumo.domain.community.boundary;

import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class LeaveCommunityFlowBT extends CommunityFlowBT {

    @Autowired
    private @NotNull CommunityService communityService;

    @Autowired
    private @NotNull MemberService memberService;

    @Autowired
    private @NotNull UserService userService;

    private @NotNull CommunityDto demoCommunity;
    private @NotNull UserDto demoCommunityOwner;
    private @NotNull UserDto demoCommunityMember;

    @BeforeEach
    void setup() {
        demoCommunity = communityService.getCommunity(UUID_COMMUNITY).orElseThrow();
        demoCommunityOwner = userService.getUserById(UUID_OWNER).orElseThrow();
        demoCommunityMember = userService.getUserById(UUID_MEMBER).orElseThrow();
    }

    @Test
    void leaveCommunityWhenLoggedIn_confirmNo() {
        // login member
        login(demoCommunityMember);

        // navigate to community detail page
        final var page = getPage();
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(DEMO_COMMUNITY_NAME_SELECTOR);
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmNo_detailViewLoaded");

        // user is a member
        assertThat(memberService.getMember(demoCommunityMember, demoCommunity)).isNotEmpty();
        assertThat(getMemberCount(page)).isEqualTo(4);

        // click the leave button
        page.click(LEAVE_BUTTON_SELECTOR);

        // don't confirm the join dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("No")
        ).click();
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmNo_dialogConfirmed");

        // user is still a member
        assertThat(memberService.getMember(demoCommunityMember, demoCommunity)).isNotEmpty();
        assertThat(getMemberCount(page)).isEqualTo(4);
    }

    @Test
    void leaveCommunityWhenLoggedIn_confirmYes() {
        // login member
        login(demoCommunityMember);

        // navigate to community detail page
        final var page = getPage();
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(DEMO_COMMUNITY_NAME_SELECTOR);
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmYes_detailViewLoaded");

        // user is a member
        assertThat(memberService.getMember(demoCommunityMember, demoCommunity)).isNotEmpty();
        assertThat(getMemberCount(page)).isEqualTo(4);

        // click the leave button
        page.click(LEAVE_BUTTON_SELECTOR);

        // confirm the join dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).click();
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmYes_dialogConfirmed");

        // user is NOT a member
        assertThat(memberService.getMember(demoCommunityMember, demoCommunity)).isEmpty();
        assertThat(getMemberCount(page)).isEqualTo(3);
    }

    @Test
    void ownerCantLeaveCommunity() {
        // login owner
        login(demoCommunityOwner);

        // navigate to community detail page
        final var page = getPage();
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(DEMO_COMMUNITY_NAME_SELECTOR);
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmYes_detailViewLoaded");

        // owner is a member
        assertThat(memberService.getMember(demoCommunityOwner, demoCommunity)).isNotEmpty();
        assertThat(getMemberCount(page)).isEqualTo(4);

        // click the leave button
        page.click(LEAVE_BUTTON_SELECTOR);

        // confirm the join dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).click();
        captureScreenshot("leaveCommunityWhenLoggedIn_confirmYes_dialogConfirmed");

        // check for warning message
        final var message = page.locator("vaadin-notification-card").first();
        assertThat(message.isVisible()).isTrue();
        assertThat(message.textContent()).startsWith("You can not leave this community.");

        // owner is still a member
        assertThat(memberService.getMember(demoCommunityOwner, demoCommunity)).isNotEmpty();
        assertThat(getMemberCount(page)).isEqualTo(4);
    }

}
