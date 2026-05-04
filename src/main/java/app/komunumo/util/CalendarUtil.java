package app.komunumo.util;

import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.infra.config.AppConfig;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class CalendarUtil {

    private static final @NotNull Path RELATIVE_CALENDAR_PATH = Path.of("uploads", "calendars");


    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CalendarUtil.class);
    private static Path uploadCalendarPath;


    public static void initialize(final @NotNull AppConfig appConfig) {
        uploadCalendarPath = appConfig.files().basedir().resolve(RELATIVE_CALENDAR_PATH);
    }

    public static @Nullable String resolveCalendarUrl(final @Nullable EventDto event) {
        if (event == null || event.id() == null) {
            return null;
        }

        Path filePath = uploadCalendarPath.resolve(event.id() + ".ics");

        // Check if the file actually exists on the disk
        if (Files.exists(filePath)) {
            return filePath.toString();
        }

        return null;
    }

    public static void storeCalendar(final @NotNull EventDto event) {
        final UUID eventId = event.id();
        if (eventId == null) {
            throw new IllegalArgumentException("EventDto must have an ID!");
        }

        Calendar icsCalendar = createCalendar(event);

        try {
            Files.createDirectories(uploadCalendarPath);
            File file = uploadCalendarPath.resolve(eventId + ".ics").toFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(icsCalendar, fileOutputStream);

            LOGGER.info("Stored calendar in '{}'", uploadCalendarPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create calendar in '{}'", uploadCalendarPath.toAbsolutePath());
        }
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
