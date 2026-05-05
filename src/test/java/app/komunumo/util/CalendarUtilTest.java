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
package app.komunumo.util;

import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarUtilTest {

    @Test
    void testGenerateCalendarResource() throws IOException {
        ZoneId zoneId = ZoneId.systemDefault();
        final var beginDateTime = ZonedDateTime.of(LocalDateTime.now(), zoneId);
        final var endDateTime = ZonedDateTime.of(LocalDateTime.now().plusHours(1), zoneId);
        final var event = createEvent(beginDateTime, endDateTime);

        Resource resource = CalendarUtil.generateCalendarResource(event);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();

        // Convert resource content to string for validation
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Verify iCal format headers
        assertThat(content).contains("BEGIN:VCALENDAR");
        assertThat(content).contains("VERSION:2.0");
        assertThat(content).contains("PRODID:-//Events Calendar//iCal4j 1.0//EN");

        // Verify Event specific details
        assertThat(content).contains("BEGIN:VEVENT");
        assertThat(content).contains("SUMMARY:Test Event");
        assertThat(content).contains("DESCRIPTION:Test Description");
        assertThat(content).contains("LOCATION:Test Location");
        assertThat(content).contains("END:VEVENT");
        assertThat(content).contains("END:VCALENDAR");
    }

    @Test
    void testGenerateCalendarBytes() {
        final var event = createEvent(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1));

        byte[] bytes = CalendarUtil.generateCalendarBytes(event);

        assertThat(bytes).isNotEmpty();
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertThat(content).startsWith("BEGIN:VCALENDAR");
    }

    private static EventDto createEvent(final ZonedDateTime begin, final ZonedDateTime end) {
        return new EventDto(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "Test Event", "Test Description", "Test Location", begin, end,
                null, true, EventVisibility.PUBLIC, EventStatus.PUBLISHED);
    }
}
