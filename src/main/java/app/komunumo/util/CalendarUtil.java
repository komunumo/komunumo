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
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import net.fortuna.ical4j.util.RandomUidGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class CalendarUtil {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CalendarUtil.class);

    public static Resource generateCalendarResource(final @NotNull EventDto event){
        var calendarBytes = generateCalendarBytes(event);
        return new ByteArrayResource(calendarBytes);
    }

    public static byte[] generateCalendarBytes(final @NotNull EventDto event) {
        Calendar icsCalendar = createCalendar(event);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        try {
            outputter.output(icsCalendar, out);
        } catch (IOException e) {
            LOGGER.error("Failed to generate calendar based on id: {}", event.id());
        }
        return out.toByteArray();
    }

    private static Calendar createCalendar(final EventDto event) {
        final var ug = new RandomUidGenerator();
        final var location = new Location(event.location());
        final var description = new Description(event.description());

        final var meeting = new VEvent(event.begin(), event.end(), event.title());
        meeting.add(ug.generateUid());
        meeting.add(location);
        meeting.add(description);

        Calendar icsCalendar = new Calendar();
        icsCalendar.add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.add(ImmutableVersion.VERSION_2_0);
        icsCalendar.add(ImmutableCalScale.GREGORIAN);
        icsCalendar.add(meeting);

        return icsCalendar;
    }
}
