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
package app.komunumo.domain.user.boundary;

import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.test.BrowserTest;
import org.junit.jupiter.api.Test;

import static app.komunumo.test.TestUtil.extractLinkFromText;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class LoginFlowBT extends BrowserTest {

    @Test
    void clickLinkInAvatarMenu() {
        final var page = getPage();
        page.navigate(getInstanceUrl() + "events");
        page.waitForURL("**/events");
        page.waitForSelector(getInstanceNameSelector());

        page.click(AVATAR_SELECTOR);
        page.waitForSelector(CONTEXT_MENU_SELECTOR);
        page.click(LOGIN_MENU_ITEM_SELECTOR);
        page.waitForURL("**/login");

        assertThat(page.url()).contains("/login");
    }

    @Test
    @SuppressWarnings({"java:S2925", "java:S2699"})
    void loginAndLogoutWorks() {
        final var testUser = getTestUser(UserRole.USER);
        login(testUser);

        final var page = getPage();
        page.navigate(getInstanceUrl() + "events");
        page.waitForURL("**/events");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("loginWorks_event-page");

        logout();
    }

    @Test
    void sessionIdShouldChangeAfterSuccessfulLogin() {
        final var page = getPage();
        final var testUser = getTestUser(UserRole.USER);

        page.navigate(getInstanceUrl() + "login");
        page.waitForURL("**/login");
        page.waitForSelector(getInstanceNameSelector());

        final var emailInput = page.locator("vaadin-email-field input");
        emailInput.fill(testUser.email());
        page.locator("vaadin-button.email-button").click();

        final var sessionIdBeforeLogin = getBrowserContext().cookies(getInstanceUrl()).stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.name))
                .map(cookie -> cookie.value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing JSESSIONID before login confirmation"));

        final var instanceName = requireNonNull(getConfigurationService().getConfiguration(app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME));
        final var confirmationMessage = getEmailBySubject("[%s] Please confirm your login".formatted(instanceName));
        final var confirmationLink = extractLinkFromText(com.icegreen.greenmail.util.GreenMailUtil.getBody(confirmationMessage));
        assertThat(confirmationLink).isNotNull();

        page.navigate(confirmationLink);
        page.waitForSelector(getInstanceNameSelector());

        final var sessionIdAfterLogin = getBrowserContext().cookies(getInstanceUrl()).stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.name))
                .map(cookie -> cookie.value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing JSESSIONID after login confirmation"));

        assertThat(sessionIdAfterLogin).isNotEqualTo(sessionIdBeforeLogin);
    }

}
