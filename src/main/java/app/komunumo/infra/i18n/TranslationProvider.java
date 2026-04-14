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

import app.komunumo.util.LocaleUtil;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.vaadin.flow.i18n.I18NProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


@Component
public final class TranslationProvider implements I18NProvider {

    // src/main/resources/vaadin-i18n/translations*.properties
    private static final @NotNull String BUNDLE_BASENAME =
            com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FOLDER + "."
                    + com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FILENAME;

    private static final @NotNull List<Locale> PROVIDED_LOCALES = List.of(
            Locale.ENGLISH, Locale.GERMAN);

    public TranslationProvider() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Override
    public @NotNull List<Locale> getProvidedLocales() {
        return PROVIDED_LOCALES;
    }

    @Override
    public @NotNull String getTranslation(final @NotNull String key,
                                          final @Nullable Locale locale,
                                          final @NotNull Object... params) {
        final var effectiveLocale = locale != null ? locale : Locale.ENGLISH;

        final String pattern;
        try {
            final var bundle = ResourceBundle.getBundle(BUNDLE_BASENAME, effectiveLocale);
            pattern = bundle.getString(key);
        } catch (final MissingResourceException ex) {
            // Missing translation → return placeholder
            return "!" + LocaleUtil.getLanguageCode(effectiveLocale).toLowerCase(Locale.ENGLISH) + ": " + key;
        }

        // No placeholder → return directly
        if (params.length == 0) {
            return pattern;
        }

        final var uLocale = ULocale.forLocale(effectiveLocale);
        final var icuFormat = new MessageFormat(pattern, uLocale);

        // Optional: Support named arguments when a map is the first argument
        if (params.length == 1 && params[0] instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            final var namedArgs = (Map<String, Object>) map;
            return icuFormat.format(namedArgs);
        }

        // Standard: Position arguments {0}, {1}, ...
        return icuFormat.format(params);
    }

}
