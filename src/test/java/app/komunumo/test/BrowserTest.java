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
package app.komunumo.test;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.APIResponseAssertions;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PageAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.test.TestUtil.extractLinkFromText;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BrowserTest extends IntegrationTest {

    protected static final String INSTANCE_NAME_SELECTOR_TEMPLATE = "h1:has-text('%s')";
    protected static final String AVATAR_SELECTOR = "vaadin-avatar";
    protected static final String CONTEXT_MENU_SELECTOR = "vaadin-context-menu-list-box[role='menu']";
    protected static final String LOGIN_MENU_ITEM_SELECTOR = "vaadin-context-menu-item[role='menuitem']:has-text('Login')";
    protected static final String LOGOUT_MENU_ITEM_SELECTOR = "vaadin-context-menu-item[role='menuitem']:has-text('Logout')";
    protected static final String ADMIN_MENU_ITEM_SELECTOR = "vaadin-context-menu-item[role='menuitem']:has-text('Administration')";
    protected static final String SETTINGS_MENU_ITEM_SELECTOR = "vaadin-context-menu-item[role='menuitem']:has-text('Settings')";

    private static final Path SCREENSHOT_DIR = Path.of("target/playwright-screenshots");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserTest.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    private Path screenshotDir;
    private Page.ScreenshotOptions screenshotOptions;

    @Autowired
    private @NotNull ConfigurationService configurationService;

    @Autowired
    private @NotNull UserService userService;

    @BeforeEach
    void startNewBrowser() {
        playwright = Playwright.create();

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        Browser.NewContextOptions ctxOptions = new Browser.NewContextOptions()
                .setReducedMotion(ReducedMotion.REDUCE)
                .setViewportSize(1920, 1080);
        browserContext = browser.newContext(ctxOptions);
        disableAnimationsGlobally();
        browserContext.clearCookies();
        page = browserContext.newPage();
        assert browserContext.cookies(getInstanceUrl()).isEmpty() : "Context unexpectedly has cookies";

        screenshotDir = SCREENSHOT_DIR.resolve(getClass().getName()).resolve(browser.browserType().name());
        screenshotOptions = new Page.ScreenshotOptions()
                .setType(ScreenshotType.PNG)
                .setFullPage(true);
    }

    protected void disableAnimationsGlobally() {
        browserContext.addInitScript("""
            (() => {
                if (document.querySelector('[data-test-disable-animations]')) {
                    return;
                }
                const css = `
                    *,:before,:after {
                        transition: none !important;
                        animation: none !important;
                        scroll-behavior: auto !important;
                    }
                    html {
                        scroll-behavior: auto !important;
                    }
                    ::view-transition {
                        display: none !important;
                    }
                `;
                const style = document.createElement('style');
                style.setAttribute('data-test-disable-animations','true');
                style.textContent = css;
                document.documentElement.appendChild(style);
            })();
        """);
    }

    @AfterEach
    void stopBrowser() {
        if (page != null) {
            page.close();
        }
        if (browserContext != null) {
            browserContext.close();
        }
        browser.close();
        playwright.close();
    }

    /**
     * <p>Returns the current Playwright {@link Page} instance used in the test.</p>
     *
     * <p>This page is created before each test and closed afterward.
     * It can be used to interact with the browser, navigate to URLs,
     * or assert the presence of elements on the page.</p>
     *
     * @return the current {@link Page} instance
     */
    protected Page getPage() {
        return page;
    }

    protected BrowserContext getBrowserContext() {
        return browserContext;
    }

    protected @NotNull ConfigurationService getConfigurationService() {
        return configurationService;
    }

    /**
     * <p>Captures a screenshot with a given name and prefixed timestamp.</p>
     *
     * @param baseName the base file name (e.g., "home", "login-error")
     */
    protected void captureScreenshot(final @NotNull String baseName) {
        try {
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }
            final var fileName = TIMESTAMP_FORMAT.format(LocalDateTime.now()) + "_" + baseName + ".png";
            final var path = screenshotDir.resolve(fileName);
            page.screenshot(screenshotOptions.setPath(path));
            LOGGER.info("Screenshot captured and saved to: {}", path.toAbsolutePath());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to capture screenshot", e);
        }
    }

    /**
     * Returns the predefined test user for the given role.
     *
     * <p>The UUIDs of the test users are fixed in {@link TestConstants} and
     * the corresponding records are inserted into the database by Flyway
     * test migrations. This method provides a convenient way for integration
     * tests to access those users without duplicating IDs.</p>
     *
     * @param role the {@link UserRole} to select
     * @return the {@link UserDto} for the requested role
     * @throws IllegalStateException if the user could not be found in the database
     */
    protected @NotNull UserDto getTestUser(final @NotNull UserRole role) {
        return switch (role) {
            case USER -> userService.getUserById(TestConstants.USER_ID_LOCAL)
                    .orElseThrow(() -> new IllegalStateException("Test USER not found in database"));
            case ADMIN -> userService.getUserById(TestConstants.USER_ID_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Test ADMIN not found in database"));
        };
    }

    /**
     * Returns the predefined test user for the given type.
     *
     * <p>The UUIDs of the test users are fixed in {@link TestConstants} and
     * the corresponding records are inserted into the database by Flyway
     * test migrations. This method provides a convenient way for integration
     * tests to access those users without duplicating IDs.</p>
     *
     * @param type the {@link UserType} to select
     * @return the {@link UserDto} for the requested type
     * @throws IllegalStateException if the user could not be found in the database
     */
    protected @NotNull UserDto getTestUser(final @NotNull UserType type) {
        return switch (type) {
            case LOCAL -> userService.getUserById(TestConstants.USER_ID_LOCAL)
                    .orElseThrow(() -> new IllegalStateException("LOCAL user not found in database"));
            case REMOTE -> userService.getUserById(TestConstants.USER_ID_REMOTE)
                    .orElseThrow(() -> new IllegalStateException("REMOTE user not found in database"));
            case ANONYMOUS -> userService.getUserById(TestConstants.USER_ID_ANONYMOUS)
                    .orElseThrow(() -> new IllegalStateException("ANONYMOUS user not found in database"));
        };
    }

    /**
     * <p>Builds and returns the CSS selector string for the current instance name.</p>
     *
     * @return the CSS selector string for the current instance name
     */
    protected @NotNull String getInstanceNameSelector() {
        final var instanceName = configurationService.getConfiguration(INSTANCE_NAME);
        return INSTANCE_NAME_SELECTOR_TEMPLATE.formatted(instanceName);
    }

    /**
     * Logs in a user for browser tests.
     *
     * @param user the user to log in
     */
    protected void login(final @NotNull UserDto user) {
        // navigate to login page
        page.navigate("http://localhost:%d/login".formatted(getPort()));
        page.waitForURL("**/login");
        page.waitForSelector(getInstanceNameSelector());

        // wait for email field to appear
        final var emailInput = page.locator("vaadin-email-field input");
        emailInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // fill in email address
        emailInput.fill(user.email());

        // click on the request email button
        page.locator("vaadin-button.email-button").click();

        // wait for the confirmation email
        final var instanceName = configurationService.getConfiguration(INSTANCE_NAME);
        final var confirmationMessage = getEmailBySubject("[%s] Please confirm your login".formatted(instanceName));

        // extract the confirmation link
        final var mailBody = GreenMailUtil.getBody(confirmationMessage);
        final var confirmationLink = extractLinkFromText(mailBody);
        assertThat(confirmationLink).isNotNull();

        // open the confirmation link
        page.navigate(confirmationLink);
        page.waitForSelector(getInstanceNameSelector());
    }

    protected void logout() {
        if (page.locator(LOGOUT_MENU_ITEM_SELECTOR).count() == 0) {
            page.click(AVATAR_SELECTOR);
            page.waitForSelector(CONTEXT_MENU_SELECTOR);
        }

        page.click(LOGOUT_MENU_ITEM_SELECTOR);
        page.waitForSelector("vaadin-context-menu-overlay",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(1500));
        } catch (PlaywrightException ignored) { }
    }

    /**
     * <p>Creates a Playwright assertion for the given {@link APIResponse}.</p>
     *
     * <p>This method provides access to Playwright's web-first assertions for API responses,
     * enabling automatic waiting and retrying until the expected condition is met or a timeout occurs.</p>
     *
     * @param response the API response to assert on
     * @return a Playwright {@link APIResponseAssertions} instance for fluent assertions
     */
    protected @NotNull APIResponseAssertions assertThatAPI(final @NotNull APIResponse response) {
        return PlaywrightAssertions.assertThat(response);
    }

    /**
     * <p>Creates a Playwright assertion for the given {@link Locator}.</p>
     *
     * <p>This method enables web-first assertions on DOM elements, automatically waiting and retrying
     * until the expected condition is satisfied or a timeout is reached.</p>
     *
     * @param locator the locator representing the target element
     * @return a Playwright {@link LocatorAssertions} instance for fluent assertions
     */
    protected @NotNull LocatorAssertions assertThatLocator(final @NotNull Locator locator) {
        return PlaywrightAssertions.assertThat(locator);
    }

    /**
     * <p>Creates a Playwright assertion for the given {@link Page}.</p>
     *
     * <p>This method provides access to Playwright's web-first assertions for pages, allowing checks
     * on navigation, URL, and other page-level conditions with built-in waiting and retry logic.</p>
     *
     * @param page the page to assert on
     * @return a Playwright {@link PageAssertions} instance for fluent assertions
     */
    protected @NotNull PageAssertions assertThatPage(final @NotNull Page page) {
        return PlaywrightAssertions.assertThat(page);
    }
}
