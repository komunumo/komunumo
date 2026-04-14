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
package app.komunumo.infra.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TranslationProviderTest {

    private TranslationProvider translationProvider;

    @BeforeEach
    void setUp() {
        this.translationProvider = new TranslationProvider();
    }

    @Test
    void testProviderLocales() {
        assertThat(translationProvider.getProvidedLocales())
                .containsExactlyInAnyOrder(ENGLISH, GERMAN);
    }

    @Test
    void testGetMessageInEnglish() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", ENGLISH))
                .isEqualTo("Upcoming Events");
    }

    @Test
    void testTranslationInGerman() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", GERMAN))
                .isEqualTo("Geplante Veranstaltungen");
    }

    @Test
    void testSwissGermanFallbackToGerman() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", Locale.forLanguageTag("de-CH")))
                .isEqualTo("Geplante Veranstaltungen");
    }

    @Test
    void testFallbackToEnglish() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", Locale.ITALIAN))
                .isEqualTo("Upcoming Events");
    }

    @Test
    void testFallbackWhenLocaleIsNull() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", null))
                .isEqualTo("Upcoming Events");
    }

    @Test
    void testWithPlaceholder() {
        assertThat(translationProvider.getTranslation("community.boundary.CommunityDetailView.profileImage", ENGLISH, "foobar"))
                .isEqualTo("Profile picture of foobar");
    }

    @ParameterizedTest
    @MethodSource("testWithNamedPlaceholderArguments")
    void testWithNamedPlaceholder(final int count, final Locale locale, final String expectedText) {
        final var params = Map.of("count", count);
        assertThat(translationProvider.getTranslation("community.boundary.CommunityDetailView.memberCount", locale, params))
                .isEqualTo(expectedText);
    }

    private static Stream<Arguments> testWithNamedPlaceholderArguments() {
        return Stream.of(
                arguments(0, ENGLISH, "no members"),
                arguments(1, ENGLISH, "one member"),
                arguments(2, ENGLISH, "2 members"),
                arguments(0, GERMAN, "keine Mitglieder"),
                arguments(1, GERMAN, "ein Mitglied"),
                arguments(2, GERMAN, "2 Mitglieder")
        );
    }

    @Test
    void testWithMissingPlaceholder() {
        assertThat(translationProvider.getTranslation("community.boundary.CommunityDetailView.profileImage", ENGLISH))
                .isEqualTo("Profile picture of {0}");
    }

    @Test
    void testMissingPlaceholder() {
        assertThat(translationProvider.getTranslation("event.boundary.UpcomingEventsView.title", ENGLISH, "foobar"))
                .isEqualTo("Upcoming Events");
    }

    @Test
    void testMissingTranslation() {
        assertThat(translationProvider.getTranslation("test.missing.translation", ENGLISH))
                .isEqualTo("!en: test.missing.translation");
        assertThat(translationProvider.getTranslation("test.missing.translation", GERMAN))
                .isEqualTo("!de: test.missing.translation");
    }

}
