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
package app.komunumo.domain.core.image.boundary;

import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.test.BrowserTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrphanedImagesViewBT extends BrowserTest {

    private static final String ORPHANED_IMAGES_SELECTOR =
            "h3:has-text('Orphaned Images')";
    private static final String ORPHANED_IMAGES_MENU_ITEM_SELECTOR =
            "vaadin-context-menu-item[role='menuitem']:has-text('Search for orphaned images')";

    @Test
    void orphanedImagesViewCanBeOpenedFromAdminMenu() {
        login(getTestUser(UserRole.ADMIN));
        final var page = getPage();

        try {
            // navigate to events page
            page.navigate(getInstanceUrl() + "events");
            page.waitForURL("**/events");
            page.waitForSelector(getInstanceNameSelector());
            captureScreenshot("adminRole_eventPageAfterLoad");

            // open avatar menu
            page.click(AVATAR_SELECTOR);
            page.waitForSelector(CONTEXT_MENU_SELECTOR);
            captureScreenshot("adminRole_avatarMenuOpened");

            // open admin submenu
            final var adminItem = page.locator(ADMIN_MENU_ITEM_SELECTOR);
            adminItem.click();
            captureScreenshot("adminRole_adminMenuOpened");

            // navigate to orphaned images view
            final var orphanedImagesItem = page.locator(ORPHANED_IMAGES_MENU_ITEM_SELECTOR);
            assertThat(orphanedImagesItem.isVisible()).isTrue();
            orphanedImagesItem.click();

            page.waitForURL("**/admin/images/orphaned");
            page.waitForSelector(ORPHANED_IMAGES_SELECTOR);
            captureScreenshot("adminRole_orphanedImagesViewLoaded");
        } finally {
            logout();
        }
    }
}
