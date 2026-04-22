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
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import jakarta.mail.MessagingException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static app.komunumo.test.TestUtil.extractLinkFromText;
import static com.icegreen.greenmail.util.GreenMailUtil.getBody;
import static org.assertj.core.api.Assertions.assertThat;

public class JoinCommunityFlowBT extends CommunityFlowBT {

    private static final String JOIN_BUTTON_SELECTOR = "vaadin-button:has-text('Join Community')";

    @Autowired
    private @NotNull CommunityService communityService;

    @Autowired
    private @NotNull UserService userService;

    private @NotNull CommunityDto demoCommunity;
    private @NotNull UserDto demoCommunityOwner;

    @BeforeEach
    void setup() {
        demoCommunity = communityService.getCommunity(UUID_COMMUNITY).orElseThrow();
        demoCommunityOwner = userService.getUserById(UUID_OWNER).orElseThrow();
    }

    @Test
    void joinCommunityAnonymously() throws MessagingException {
        final var emailAddressMember = getRandomEmailAddress();

        final var page = getPage();
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(DEMO_COMMUNITY_NAME_SELECTOR);
        captureScreenshot("joinCommunityAnonymously_detailViewLoaded");

        // check member count before joining
        assertThat(getMemberCount(page)).isEqualTo(4);

        // click the join button
        page.click(JOIN_BUTTON_SELECTOR);

        // wait for join dialog to appear
        // wait for email field to appear
        final var emailInput = page.locator("vaadin-email-field input");
        emailInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        captureScreenshot("joinCommunityAnonymously_dialog");

        // fill in email address
        emailInput.fill(emailAddressMember);
        captureScreenshot("joinCommunityAnonymously_dialogFilled");

        // click on the request email button
        page.locator("vaadin-button.email-button").click();

        // close the dialog
        final var closeButton = page.locator("vaadin-button.close-button:has-text(\"Close\")");
        closeButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        captureScreenshot("joinCommunityAnonymously_dialogAfterEmailRequested");
        closeButton.click();

        // wait for email confirmation mail
        final var confirmationMessage = getEmailBySubject("[Komunumo Test] Please confirm your email address");

        // extract the confirmation link
        final var mailBody = GreenMailUtil.getBody(confirmationMessage);
        final var confirmationLink = extractLinkFromText(mailBody);
        assertThat(confirmationLink).isNotNull();

        // open the confirmation link
        page.navigate(confirmationLink);
        page.waitForURL("**/confirm**");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("joinCommunityAnonymously_confirmationPage");

        // wait for join confirmation mail for the new member
        final var memberMessage = getEmailBySubject("[Komunumo Test] You have joined a community");
        assertThat(memberMessage.getAllRecipients()[0].toString()).isEqualTo(emailAddressMember);
        assertThat(getBody(memberMessage)).contains("You are now a member of the community \"%s\"."
                .formatted(demoCommunity.name()));

        // wait for join confirmation mail for the community owner
        final var ownerMessage = getEmailBySubject("[Komunumo Test] You have a new member");
        assertThat(ownerMessage.getAllRecipients()[0].toString()).isEqualTo(demoCommunityOwner.email());
        assertThat(getBody(ownerMessage)).contains("A new member joined your community \"%s\".\r\nYou now have 5 members."
                .formatted(demoCommunity.name()));

        // back to community detail page
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(DEMO_COMMUNITY_NAME_SELECTOR);
        captureScreenshot("joinCommunityAnonymously_detailViewLoaded");

        // check member count before joining
        assertThat(getMemberCount(page)).isEqualTo(5);
    }

    @Test
    void joinCommunityWhenLoggedIn() throws MessagingException {
        final var localUserEmail = getRandomEmailAddress();
        final var profile = localUserEmail.split("@")[0];
        final var localUser = userService.storeUser(new UserDto(null, null, null,
                profile, localUserEmail,"Join Community Test", "", null, UserRole.USER, UserType.LOCAL));
        login(localUser);

        final var demoCommunityNameSelector = "h2.community-name";

        final var page = getPage();
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(demoCommunityNameSelector);
        captureScreenshot("joinCommunityWhenLoggedIn_detailViewLoaded");

        // check member count before joining
        assertThat(getMemberCount(page)).isEqualTo(4);

        // click the join button
        page.click(JOIN_BUTTON_SELECTOR);

        // confirm the join dialog
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).click();
        captureScreenshot("joinCommunityWhenLoggedIn_dialogConfirmed");

        // wait for join confirmation mail for the new member
        final var memberMessage = getEmailBySubject("[Komunumo Test] You have joined a community");
        assertThat(memberMessage.getAllRecipients()[0].toString()).isEqualTo(localUserEmail);
        assertThat(getBody(memberMessage)).contains("You are now a member of the community \"%s\"."
                .formatted(demoCommunity.name()));

        // wait for join confirmation mail for the community owner
        final var ownerMessage = getEmailBySubject("[Komunumo Test] You have a new member");
        assertThat(ownerMessage.getAllRecipients()[0].toString()).isEqualTo(demoCommunityOwner.email());
        assertThat(getBody(ownerMessage)).contains("A new member joined your community \"%s\".\r\nYou now have 5 members."
                .formatted(demoCommunity.name()));

        // back to community detail page
        page.navigate(LinkUtil.getLink(demoCommunity, true));
        page.waitForURL("**/communities/@" + demoCommunity.handle());
        page.waitForSelector(demoCommunityNameSelector);
        captureScreenshot("joinCommunityAnonymously_detailViewLoaded");

        // check member count before joining
        assertThat(getMemberCount(page)).isEqualTo(5);
    }

}
