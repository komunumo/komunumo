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
import app.komunumo.infra.config.AppConfig;
import app.komunumo.infra.config.DemoConfig;
import app.komunumo.infra.config.FilesConfig;
import app.komunumo.infra.config.InstanceConfig;
import app.komunumo.infra.config.MailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

class CalendarUtilTest {
    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        final var demoConfig = new DemoConfig(false, "");
        final var filesConfig = new FilesConfig(tempDir);
        final var mailConfig = new MailConfig("noreply@foo.bar", "support@foo.bar");
        final var instanceConfig = new InstanceConfig("admin@foo.bar");
        final var appConfig = new AppConfig("0.0.0", demoConfig, filesConfig, instanceConfig, mailConfig);
        CalendarUtil.initialize(appConfig);
    }

    @Test
    void testICalCreation() {
        ZoneId zoneId = ZoneId.systemDefault();
        final var beginDateTime = ZonedDateTime.of(LocalDateTime.now(), zoneId);
        final var endDateTime = ZonedDateTime.of(LocalDateTime.now().plusHours(1), zoneId);
        final var event = createEvent(beginDateTime, endDateTime);

        CalendarUtil.storeCalendar(event);

        var calendarUrl = CalendarUtil.resolveCalendarUrl(event);
        System.out.println(calendarUrl);
    }

    private static EventDto createEvent(final ZonedDateTime begin, final ZonedDateTime end) {
        return new EventDto(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "Test Event", "Test Description", "Test Location", begin, end,
                null, true, EventVisibility.PUBLIC, EventStatus.PUBLISHED);
    }
}
