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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LocaleUtilTest {

    @Test
    void shouldReturnEmptyStringIfLocaleIsNull() {
        assertThat(LocaleUtil.getLanguageCode(null)).isEmpty();
    }

    @Test
    void shouldReturnLanguageCodeInUpperCaseForEnglish() {
        assertThat(LocaleUtil.getLanguageCode(Locale.ENGLISH)).isEqualTo("en");
    }

    @Test
    void shouldReturnLanguageCodeInUpperCaseForGerman() {
        assertThat(LocaleUtil.getLanguageCode(Locale.GERMAN)).isEqualTo("de");
    }

    @Test
    void shouldReturnLanguageCodeInUpperCaseForFrench() {
        assertThat(LocaleUtil.getLanguageCode(Locale.FRENCH)).isEqualTo("fr");
    }

    @Test
    void shouldNormalizeLowerCaseLanguage() {
        final var custom = Locale.of("fr");
        assertThat(LocaleUtil.getLanguageCode(custom)).isEqualTo("fr");
    }

    @Test
    void shouldIgnoreCountryAndVariant() {
        final var swissGerman = Locale.of("de", "CH");
        assertThat(LocaleUtil.getLanguageCode(swissGerman)).isEqualTo("de");
    }

}
