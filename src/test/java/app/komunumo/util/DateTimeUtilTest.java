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

import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DateTimeUtilTest {

    private VaadinSession mockSession;

    @BeforeEach
    void setup() {
        mockSession = mock(VaadinSession.class);
        VaadinSession.setCurrent(mockSession);
    }

    @ParameterizedTest
    @MethodSource("provideTestData_getLocalizedDateTime")
    void getLocalizedDateTimeWithZoneId(final ZoneId targetTimeZone,
                                        final ZonedDateTime input,
                                        final ZonedDateTime expected) {
        final var actual = DateTimeUtil.getLocalizedDateTime(input, targetTimeZone);
        assertThat(actual)
                .as("Expected dateTime in zone %s", targetTimeZone)
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideTestData_getLocalizedDateTime")
    void getLocalizedDateTimeWithoutZoneId(final ZoneId targetTimeZone,
                                           final ZonedDateTime input,
                                           final ZonedDateTime expected) {
        try (var mockedTimeZoneUtil = mockStatic(TimeZoneUtil.class)) {
            mockedTimeZoneUtil.when(TimeZoneUtil::getClientTimeZone).thenAnswer(_ -> targetTimeZone);
            final var actual = DateTimeUtil.getLocalizedDateTime(input);
            assertThat(actual)
                    .as("Expected dateTime in zone %s", targetTimeZone)
                    .isEqualTo(expected);
        }
    }

    private static Stream<Arguments> provideTestData_getLocalizedDateTime() {
        return Stream.of(
                Arguments.of(
                        ZoneId.of("UTC"),
                        ZonedDateTime.of(2025, 6, 21, 10, 0, 0, 0, ZoneId.of("Europe/Zurich")),
                        ZonedDateTime.of(2025, 6, 21, 8, 0, 0, 0, ZoneId.of("UTC"))
                ),
                Arguments.of(
                        ZoneId.of("America/New_York"),
                        ZonedDateTime.of(2025, 12, 24, 18, 30, 0, 0, ZoneId.of("Europe/Berlin")),
                        ZonedDateTime.of(2025, 12, 24, 12, 30, 0, 0, ZoneId.of("America/New_York"))
                ),
                Arguments.of(
                        ZoneId.of("Asia/Tokyo"),
                        ZonedDateTime.of(2025, 3, 15, 8, 0, 0, 0, ZoneId.of("Europe/London")),
                        ZonedDateTime.of(2025, 3, 15, 17, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestData_getLocalizedDateTimeString")
    void getLocalizedDateTimeStringWithZoneId(final ZoneId zoneId,
                                              final Locale locale,
                                              final ZonedDateTime inputDateTime,
                                              final String expectedPart) {
        final var formatted = DateTimeUtil.getLocalizedDateTimeString(inputDateTime, zoneId, locale);
        assertThat(formatted)
                .as("Formatted string for zone %s and locale %s", zoneId, locale)
                .isEqualTo(expectedPart);
    }

    @ParameterizedTest
    @MethodSource("provideTestData_getLocalizedDateTimeString")
    void getLocalizedDateTimeStringWithoutZoneId(final ZoneId zoneId,
                                                 final Locale locale,
                                                 final ZonedDateTime inputDateTime,
                                                 final String expectedPart) {
        try (var mockedTimeZoneUtil = mockStatic(TimeZoneUtil.class)) {
            mockedTimeZoneUtil.when(TimeZoneUtil::getClientTimeZone).thenAnswer(_ -> zoneId);
            final var formatted = DateTimeUtil.getLocalizedDateTimeString(inputDateTime, locale);
            assertThat(formatted)
                    .as("Formatted string for zone %s and locale %s", zoneId, locale)
                    .isEqualTo(expectedPart);
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestData_getLocalizedDateTimeString")
    void getLocalizedDateTimeStringWithoutZoneIdAndLocale(final ZoneId zoneId,
                                                          final Locale locale,
                                                          final ZonedDateTime inputDateTime,
                                                          final String expectedPart) {
        when(mockSession.getAttribute("CLIENT_LOCALE")).thenReturn(locale);
        try (var mockedTimeZoneUtil = mockStatic(TimeZoneUtil.class)) {
            mockedTimeZoneUtil.when(TimeZoneUtil::getClientTimeZone).thenAnswer(_ -> zoneId);
            final var formatted = DateTimeUtil.getLocalizedDateTimeString(inputDateTime, locale);
            assertThat(formatted)
                    .as("Formatted string for zone %s and locale %s", zoneId, locale)
                    .isEqualTo(expectedPart);
        }
    }

    private static Stream<Arguments> provideTestData_getLocalizedDateTimeString() {
        final var baseTime = ZonedDateTime.of(2025, 6, 25, 20, 0, 0, 0, ZoneId.of("UTC"));

        return Stream.of(
                Arguments.of(
                        ZoneId.of("Europe/Berlin"),
                        Locale.GERMANY,
                        null,
                        ""),
                Arguments.of(
                        ZoneId.of("Europe/Zurich"),
                        Locale.GERMANY,
                        baseTime,
                        "Mittwoch, 25. Juni 2025, 22:00 MEZ"),
                Arguments.of(
                        ZoneId.of("America/New_York"),
                        Locale.US,
                        baseTime,
                        "Wednesday, June 25, 2025, 4:00 PM ET"),
                Arguments.of(
                        ZoneId.of("Asia/Tokyo"),
                        Locale.JAPAN,
                        baseTime,
                        "2025年6月26日木曜日 5:00 GMT+09:00")
        );
    }

}
