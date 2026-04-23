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
package app.komunumo.domain.core.importer.boundary;

import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.test.BrowserTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class ImporterViewBT extends BrowserTest {

    private static final String TEST_BASE_URL =
            requireNonNull(System.getProperty("komunumo.test.base-url"),
                    "Missing system property: komunumo.test.base-url");
    private static final String IMPORTER_SELECTOR =
            "h2:has-text('Import Data')";
    private static final String IMPORTER_MENU_ITEM_SELECTOR =
            "vaadin-context-menu-item[role='menuitem']:has-text('Import Data')";
    private static final String IMPORT_LOG_TITLE_SELECTOR =
            "h3:has-text('Import Log')";
    private static final String IMPORT_LOG_LIST_SELECTOR =
            "ul.import-log";
    private static final String IMPORTER_FINISHED_SELECTOR =
            "li:has-text('...finished importing 2 global pages.')";
    private static final String DOWNLOAD_FAILED_SELECTOR =
            "li:has-text('Failed to download JSON data from URL')";
    private static final String INVALID_URL_SCHEMA_MESSAGE =
            "Only HTTPS URLs are allowed.";
    private static final String VAADIN_UPLOAD_INPUT_SELECTOR =
            "vaadin-upload input[type='file']";
    private static final String IMPORT_LOG_CONTAINER_SELECTOR =
            "#import-log-container";

    @Test
    void noImportForAnonymousVisitors() {
        final var page = getPage();

        // navigate to events page
        page.navigate(getInstanceUrl() + "events");
        page.waitForURL("**/events");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("anonymousVisitor_eventPageAfterLoad");

        // open avatar menu
        page.click(AVATAR_SELECTOR);
        page.waitForSelector(CONTEXT_MENU_SELECTOR);
        captureScreenshot("anonymousVisitor_avatarMenuOpened");

        // check that there is no admin editor menu item
        final var adminItem = page.locator(ADMIN_MENU_ITEM_SELECTOR);
        assertThat(adminItem.isVisible()).isFalse();

        // try to navigate directly to importer will start the authentication process
        page.navigate(getInstanceUrl() + "admin/import");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("anonymousVisitor_afterManualNavigation");
        assertThat(page.locator("h3:visible").allInnerTexts()).contains("Please log in");
    }

    @Test
    void noImportForUserRole() {
        login(getTestUser(UserRole.USER));
        final var page = getPage();

        try {
            // navigate to events page
            page.navigate(getInstanceUrl() + "events");
            page.waitForURL("**/events");
            page.waitForSelector(getInstanceNameSelector());
            captureScreenshot("userRole_eventPageAfterLoad");

            // open avatar menu
            page.click(AVATAR_SELECTOR);
            page.waitForSelector(CONTEXT_MENU_SELECTOR);
            captureScreenshot("userRole_avatarMenuOpened");

            // check that there is no admin editor menu item
            final var adminItem = page.locator(ADMIN_MENU_ITEM_SELECTOR);
            assertThat(adminItem.isVisible()).isFalse();

            // try to navigate directly to importer will show an error message
            page.navigate(getInstanceUrl() + "admin/import");
            page.waitForURL("**/admin/import");
            page.waitForSelector(getInstanceNameSelector());
            captureScreenshot("userRole_manualNavigation");
            assertThat(page.locator("h2:visible").allInnerTexts()).contains("Page not found");
        } finally {
            logout();
        }
    }

    @Test
    void importerWorksWithURL() throws InterruptedException {
        login(getTestUser(UserRole.ADMIN));
        final var page = getPage();

        try {
            // navigate to events page
            page.navigate(getInstanceUrl() + "events");
            page.waitForURL("**/events");
            page.waitForSelector(getInstanceNameSelector());
            captureScreenshot("importerWorksWithURL_eventPageAfterLoad");

            // open avatar menu
            page.click(AVATAR_SELECTOR);
            page.waitForSelector(CONTEXT_MENU_SELECTOR);
            captureScreenshot("importerWorksWithURL_avatarMenuOpened");

            // open admin menu
            final var adminItem = page.locator(ADMIN_MENU_ITEM_SELECTOR);
            adminItem.click();
            captureScreenshot("importerWorksWithURL_adminMenuOpened");

            // check that there is an importer menu item
            final var importerItem = page.locator(IMPORTER_MENU_ITEM_SELECTOR);
            assertThat(importerItem.isVisible()).isTrue();

            // click on importer menu item
            importerItem.click();
            page.waitForURL("**/admin/import");
            page.waitForSelector(IMPORTER_SELECTOR);
            captureScreenshot("importerWorksWithURL_importerViewLoaded");

            // URL field should be pre-filled with HTTPS prefix
            final var urlField = page.locator("vaadin-text-field.url-field");
            final var urlFieldInput = urlField.locator("input");
            assertThat(urlFieldInput.inputValue()).isEqualTo("https://");

            // start import button should be disabled
            final var startImportButton = page.locator("vaadin-button.start-import-button");
            assertThat(startImportButton.isEnabled()).isFalse();

            // import log container should not be visible
            final var importLogContainer = page.locator(IMPORT_LOG_CONTAINER_SELECTOR);
            assertThat(importLogContainer.isVisible()).isFalse();

            // there should be no logs visible
            final var logList = page.locator(IMPORT_LOG_LIST_SELECTOR);
            assertThat(logList.locator("li").count()).isZero();

            // fill in URL field
            urlFieldInput.fill(TEST_BASE_URL + "/import/data.json");
            Thread.sleep(100); // wait for all UI animations to finish

            // start import button should be enabled
            assertThat(startImportButton.isEnabled()).isTrue();

            // click start import button
            startImportButton.click();
            captureScreenshot("importerWorksWithURL_afterStartImportButtonClick");

            // log container should be visible
            assertThat(importLogContainer.isVisible()).isTrue();

            // there should be logs available
            assertThat(logList.locator("li").count()).isGreaterThan(0);

            // wait for import to finish
            page.waitForSelector(IMPORTER_FINISHED_SELECTOR);
            Thread.sleep(100); // wait for all UI animations to finish
            captureScreenshot("importerWorksWithURL_importerFinished");

            // empty URL field again
            urlFieldInput.fill("");
            Thread.sleep(100); // wait for all UI animations to finish

            // start import button should be disabled again
            assertThat(startImportButton.isEnabled()).isFalse();
        } finally {
            logout();
        }
    }

    @Test
    void importerFailsWithIncorrectURL() throws InterruptedException {
        login(getTestUser(UserRole.ADMIN));
        final var page = getPage();

        try {
            // navigate to importer page
            page.navigate(getInstanceUrl() + "admin/import");
            page.waitForURL("**/admin/import");
            page.waitForSelector(IMPORTER_SELECTOR);
            captureScreenshot("incorrectURL_importerViewLoaded");

            // fill in URL field
            final var urlField = page.locator("vaadin-text-field.url-field");
            final var urlFieldInput = urlField.locator("input");
            urlFieldInput.fill(TEST_BASE_URL + "/import/non-existing.json");
            Thread.sleep(100); // wait for all UI animations to finish

            // start import button should be enabled
            final var startImportButton = page.locator("vaadin-button.start-import-button");
            assertThat(startImportButton.isEnabled()).isTrue();

            // click start import button
            startImportButton.click();
            Thread.sleep(100); // wait for all UI animations to finish

            // wait for import to fail
            page.waitForSelector(DOWNLOAD_FAILED_SELECTOR);
            Thread.sleep(100); // wait for all UI animations to finish
            captureScreenshot("incorrectURL_importerFailed");

            // check logs
            final var logTitle = page.locator(IMPORT_LOG_TITLE_SELECTOR);
            assertThat(logTitle.isVisible()).isTrue();
            final var logList = page.locator(IMPORT_LOG_LIST_SELECTOR);
            assertThat(logList.isVisible()).isTrue();
            assertThat(logList.locator("li").count()).isGreaterThan(0);
        } finally {
            logout();
        }
    }

    @Test
    void importerRejectsURLWithInvalidSchema() throws InterruptedException {
        login(getTestUser(UserRole.ADMIN));
        final var page = getPage();

        try {
            // navigate to importer page
            page.navigate(getInstanceUrl() + "admin/import");
            page.waitForURL("**/admin/import");
            page.waitForSelector(IMPORTER_SELECTOR);
            captureScreenshot("invalidSchema_importerViewLoaded");

            // fill in URL field with invalid schema
            final var urlField = page.locator("vaadin-text-field.url-field");
            final var urlFieldInput = urlField.locator("input");
            //noinspection HttpUrlsUsage
            urlFieldInput.fill("http://example.com/import/data.json");
            Thread.sleep(100); // wait for all UI animations to finish

            // start import button should be disabled
            final var startImportButton = page.locator("vaadin-button.start-import-button");
            assertThat(startImportButton.isEnabled()).isFalse();

            // check english validation message
            final var errorMessage = urlField.locator("[slot='error-message']");
            assertThat(errorMessage.isVisible()).isTrue();
            assertThat(errorMessage.innerText()).isEqualTo(INVALID_URL_SCHEMA_MESSAGE);
            captureScreenshot("invalidSchema_validationError");
        } finally {
            logout();
        }
    }

    @Test
    void importerWorksWithUpload() throws InterruptedException {
        login(getTestUser(UserRole.ADMIN));
        final var page = getPage();

        try {
            // navigate to importer page
            page.navigate(getInstanceUrl() + "admin/import");
            page.waitForURL("**/admin/import");
            page.waitForSelector(IMPORTER_SELECTOR);
            captureScreenshot("importerWorksWithUpload_importerViewLoaded");

            // import log container should not be visible
            final var importLogContainer = page.locator(IMPORT_LOG_CONTAINER_SELECTOR);
            assertThat(importLogContainer.isVisible()).isFalse();

            // there should be no logs visible
            final var logList = page.locator(IMPORT_LOG_LIST_SELECTOR);
            assertThat(logList.locator("li").count()).isZero();

            // upload a file
            final var uploadInput = page.locator(VAADIN_UPLOAD_INPUT_SELECTOR);
            uploadInput.setInputFiles(Path.of("src/test/resources/import/data.json"));
            Thread.sleep(100); // wait for all UI animations to finish

            // log container should be visible
            assertThat(importLogContainer.isVisible()).isTrue();

            // there should be logs available
            assertThat(logList.locator("li").count()).isGreaterThan(0);

            // wait for import to finish
            page.waitForSelector(IMPORTER_FINISHED_SELECTOR);
            Thread.sleep(100); // wait for all UI animations to finish
            captureScreenshot("importerWorksWithUpload_importerFinished");
        } finally {
            logout();
        }
    }

}
