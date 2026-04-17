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
import app.komunumo.test.BrowserTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventsViewBT extends BrowserTest {

    @Autowired
    private @NotNull EventService eventService;

    @Test
    void checkUpcomingEvents() {
        final var page = getPage();

        page.navigate(getInstanceUrl() + "events/");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("upcoming-events-view");
        assertThat(page.title()).isEqualTo("Events – Komunumo Test");

        assertThat(page.locator("a[href='events']").getAttribute("highlight")).isNotNull().isBlank();

        final var eventCards = page.locator(".upcoming-events-grid vaadin-card");
        assertThat(eventCards.count()).isEqualTo(3);

        for (var entry : Map.of(
                0, 3,
                1, 5,
                2, 6) // position and event number
                .entrySet()) {
            assertEventCard(eventCards.nth(entry.getKey()), entry.getValue());
        }
    }

    @Test
    void checkPastEvents() {
        final var page = getPage();

        page.navigate(getInstanceUrl() + "events");
        page.waitForSelector(getInstanceNameSelector());
        switchToPastEventsTab(page);
        captureScreenshot("past-events-view");
        assertThat(page.title()).isEqualTo("Events – Komunumo Test");

        assertThat(page.locator("a[href='events']").getAttribute("highlight")).isNotNull().isBlank();

        final var eventCards = page.locator(".past-events-grid vaadin-card");
        assertThat(eventCards.count()).isEqualTo(2);

        for (var entry : Map.of(
                0, 2,
                1, 1) // position and event number
                .entrySet()) {
            assertEventCard(eventCards.nth(entry.getKey()), entry.getValue());
        }
    }

    @Test
    void clickOnUpcomingEventCard() {
        final var testEvent = eventService.getUpcomingEventsWithImage()
                .stream()
                .filter(eventWithImage -> eventWithImage.event().title().equals("Demo Event 3"))
                .findAny()
                .orElseThrow()
                .event();

        final var page = getPage();

        page.navigate(getInstanceUrl() + "events");
        page.waitForSelector(getInstanceNameSelector());
        captureScreenshot("event-grid-view");

        final var eventCards = page.locator(".upcoming-events-grid vaadin-card");
        assertThat(eventCards.count()).isNotZero();

        final var eventCard = eventCards.first();
        final var title = eventCard.locator("div[slot='title']");
        assertThat(title.textContent()).isEqualTo("Demo Event 3");

        eventCard.click();
        page.waitForURL("**/events/" + testEvent.id());
        captureScreenshot("event-detail-view");

        assertThat(page.url()).contains("/events/" + testEvent.id());
        page.waitForSelector("h2:has-text('Demo Event 3')");
    }

    @Test
    void clickOnPastEventCard() {
        final var testEvent = eventService.getPastEventsWithImage()
                .stream()
                .filter(eventWithImage -> eventWithImage.event().title().equals("Demo Event 2"))
                .findAny()
                .orElseThrow()
                .event();

        final var page = getPage();

        page.navigate(getInstanceUrl() + "events");
        page.waitForSelector(getInstanceNameSelector());
        switchToPastEventsTab(page);
        captureScreenshot("past-event-grid-view");

        final var eventCards = page.locator(".past-events-grid vaadin-card");
        assertThat(eventCards.count()).isNotZero();

        final var eventCard = eventCards.first();
        final var title = eventCard.locator("div[slot='title']");
        assertThat(title.textContent()).isEqualTo("Demo Event 2");

        eventCard.click();
        page.waitForURL("**/events/" + testEvent.id());
        captureScreenshot("event-detail-view");

        assertThat(page.url()).contains("/events/" + testEvent.id());
        page.waitForSelector("h2:has-text('Demo Event 2')");
    }

    private void switchToPastEventsTab(final @NotNull Page page) {
        page.locator("vaadin-tab:has-text('Past Events')").click();
    }

    private void assertEventCard(final @NotNull Locator eventCard, final int number) {
        final var title = eventCard.locator("div[slot='title']");
        assertThat(title.textContent()).isEqualTo("Demo Event " + number);

        final var image = eventCard.locator("img[slot='media']");
        assertThat(image.count())
                .as("image should be set")
                .isEqualTo(1);

        if (number <= 5) {
            final var imageSrc = image.getAttribute("src");
            assertThat(imageSrc)
                    .as("expected to contain an image but was: " + imageSrc)
                    .startsWith("/images/")
                    .endsWith(".svg")
                    .doesNotContain("placeholder");

            final var imageAlt = image.getAttribute("alt");
            assertThat(imageAlt)
                    .as("expected to contain an alt text but was: " + imageSrc)
                    .isEqualTo("Demo Event " + number);
        } else { // demo event 6+ has a placeholder image
            final var imageSrc = image.getAttribute("src");
            assertThat(imageSrc)
                    .as("expected to contain an image but was: " + imageSrc)
                    .startsWith("/images/placeholder")
                    .endsWith(".svg");

            final var imageAlt = image.getAttribute("alt");
            assertThat(imageAlt)
                    .as("expected to contain an alt text but was: " + imageSrc)
                    .isEqualTo("Placeholder Image");
        }
    }

}
