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
package app.komunumo.domain.core.config.control;

import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.util.LocaleUtil;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

import static app.komunumo.data.db.tables.Config.CONFIG;

/**
 * <p>Service for reading and writing instance configuration values.</p>
 *
 * <p>Values are stored in the {@code config} table and optionally cached in-memory using Caffeine.
 * Language-dependent lookups fall back to English and then to the neutral (language-independent)
 * value before returning the setting’s default.</p>
 */
@Service
public class ConfigurationService {

    /**
     * <p>jOOQ context used for database access.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>In-memory cache keyed by setting and language code, storing optional string values.</p>
     */
    private final @NotNull Cache<@NotNull CacheKey, @NotNull Optional<@NotNull String>> cache;

    /**
     * <p>Creates a new configuration service backed by the given jOOQ context.</p>
     *
     * @param dsl the jOOQ context used for database operations
     */
    public ConfigurationService(final @NotNull DSLContext dsl) {
        super();
        this.dsl = dsl;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .build();
        LinkUtil.initialize(this);
    }

    /**
     * <p>Counts the total number of configurations.</p>
     *
     * @return The total count of configurations; never negative.
     */
    public int getConfigurationCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(CONFIG)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Returns the configuration value as a string for a language-independent setting.</p>
     *
     * @param setting the configuration setting
     * @return the resolved value or the default if not present
     */
    public @NotNull String getConfiguration(final @NotNull ConfigurationSetting setting) {
        return getConfiguration(setting, null, String.class);
    }

    /**
     * <p>Returns the configuration value as a string for the given locale.</p>
     *
     * <p>If no locale-specific value exists, the method falls back to English and then to the
     * language-independent value. If none is found, the setting’s default value is returned.</p>
     *
     * @param setting the configuration setting
     * @param locale the desired locale, may be {@code null} for language-independent values
     * @return the resolved value or the default if not present
     */
    public @NotNull String getConfiguration(final @NotNull ConfigurationSetting setting,
                                            final @Nullable Locale locale) {
        return getConfiguration(setting, locale, String.class);
    }

    /**
     * <p>Returns the configuration value coerced to the requested type for a neutral (no-locale) key.</p>
     *
     * @param setting the configuration setting
     * @param type the expected return type (String or Boolean)
     * @param <T> the generic type parameter
     * @return the value converted to {@code type}, or the default if not present
     * @throws IllegalArgumentException if {@code type} is not supported
     */
    public @NotNull <T> T getConfiguration(final @NotNull ConfigurationSetting setting,
                                           final @NotNull Class<T> type) {
        return getConfiguration(setting, null, type);
    }

    /**
     * <p>Returns the configuration value coerced to the requested type for the given locale.</p>
     *
     * <p>Lookup order is: locale-specific → English → neutral → default value.</p>
     *
     * @param setting the configuration setting
     * @param locale the desired locale, may be {@code null} for language-independent values
     * @param type the expected return type (String or Boolean)
     * @param <T> the generic type parameter
     * @return the value converted to {@code type}, or the default if not present
     * @throws IllegalArgumentException if {@code type} is not supported
     */
    public @NotNull <T> T getConfiguration(final @NotNull ConfigurationSetting setting,
                                           final @Nullable Locale locale,
                                           final @NotNull Class<T> type) {
        checkLocale(setting, locale);
        final var languageCode = LocaleUtil.getLanguageCode(locale);
        final var value = locale == null
                ? getFromCacheOrDb(setting, "")
                        .orElse(setting.defaultValue())
                : getFromCacheOrDb(setting, languageCode)
                        .or(() -> getFromCacheOrDb(setting, "en"))
                        .or(() -> getFromCacheOrDb(setting, ""))
                        .orElse(setting.defaultValue());

        if (type == String.class) {
            return type.cast(value);
        } else if (type == Boolean.class) {
            return type.cast(Boolean.parseBoolean(value));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * <p>Returns the configuration value for the given setting and locale without applying any
     * fallback logic.</p>
     *
     * <p>This method retrieves only the exact language-specific or neutral value (depending on the
     * provided locale). If no matching entry exists in the database, the setting’s default value
     * is returned.</p>
     *
     * @param setting the configuration setting to retrieve
     * @param locale the locale for the requested value, or {@code null} for the language-independent value
     * @return the stored configuration value, or the setting’s default value if not present
     */
    public @NotNull String getConfigurationWithoutFallback(final @NotNull ConfigurationSetting setting,
                                                           final @Nullable Locale locale) {
        checkLocale(setting, locale);
        final var languageCode = LocaleUtil.getLanguageCode(locale);
        return getFromCacheOrDb(setting, languageCode)
                .orElse(setting.defaultValue());
    }

    /**
     * <p>Resolves a configuration value from the cache or loads it from the database.</p>
     *
     * <p>The result is cached as an {@code Optional<String>} (empty if no row exists) keyed by
     * {@link CacheKey}.</p>
     *
     * @param setting the configuration setting
     * @param language the ISO language code (e.g., {@code "EN"}) or empty string for neutral
     * @return an optional containing the value if present
     */
    private @NotNull Optional<String> getFromCacheOrDb(final @NotNull ConfigurationSetting setting,
                                                       final @NotNull String language) {
        return cache.get(new CacheKey(setting, language), cacheKey ->
                dsl.select(CONFIG.VALUE)
                        .from(CONFIG)
                        .where(CONFIG.SETTING.eq(cacheKey.setting.setting()))
                        .and(CONFIG.LANGUAGE.eq(language))
                        .fetchOptional(CONFIG.VALUE)
        );
    }

    /**
     * <p>Stores a language-independent configuration value.</p>
     *
     * <p>Only administrators are allowed to change configuration values.</p>
     *
     * @param setting the configuration setting
     * @param value the value to store, converted via {@code toString()}
     * @param <T> the value type
     */
    public <T> void setConfiguration(final @NotNull ConfigurationSetting setting,
                                     final @NotNull T value) {
        setConfiguration(setting, null, value);
    }

    /**
     * <p>Stores a configuration value for the given locale, inserting or updating the row.</p>
     *
     * <p>After write, all cache entries for the given setting are invalidated.</p>
     *
     * @param setting the configuration setting
     * @param locale the locale for the value, {@code null} for language-independent
     * @param value the value to store, converted via {@code toString()}
     * @param <T> the value type
     */
    public <T> void setConfiguration(final @NotNull ConfigurationSetting setting,
                                     final @Nullable Locale locale,
                                     final @NotNull T value) {
        checkLocale(setting, locale);
        final var dbValue = value.toString();
        final var languageCode = LocaleUtil.getLanguageCode(locale);

        dsl.insertInto(CONFIG)
                .set(CONFIG.SETTING, setting.setting())
                .set(CONFIG.LANGUAGE, languageCode)
                .set(CONFIG.VALUE, dbValue)
                .onDuplicateKeyUpdate()
                .set(CONFIG.VALUE, dbValue)
                .execute();

        cache.asMap().keySet().removeIf(cacheKey -> cacheKey.setting().equals(setting));
    }

    /**
     * <p>Clears all cached configuration entries.</p>
     */
    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * <p>Deletes the language-independent value for the given setting.</p>
     *
     * <p>Only administrators are allowed to delete configuration values.</p>
     *
     * @param setting the configuration setting to delete
     */
    public void deleteConfiguration(final @NotNull ConfigurationSetting setting) {
        deleteConfiguration(setting, null);
    }

    /**
     * <p>Deletes the configuration value for the given setting and locale.</p>
     *
     * <p>After deletion, all cache entries for the given setting are invalidated.</p>
     *
     * @param setting the configuration setting to delete
     * @param locale the locale of the value to delete, {@code null} for language-independent
     */
    public void deleteConfiguration(final @NotNull ConfigurationSetting setting,
                                    final @Nullable Locale locale) {
        checkLocale(setting, locale);
        final var languageCode = LocaleUtil.getLanguageCode(locale);

        dsl.deleteFrom(CONFIG)
                .where(CONFIG.SETTING.eq(setting.setting()))
                .and(CONFIG.LANGUAGE.eq(languageCode))
                .execute();

        cache.asMap().keySet().removeIf(cacheKey -> cacheKey.setting().equals(setting));
    }


    /**
     * <p>Deletes all configuration rows and clears the cache.</p>
     *
     * <p>Only administrators are allowed to perform full deletion.</p>
     */
    public void deleteAllConfigurations() {
        dsl.delete(CONFIG).execute();
        clearCache();
    }

    /**
     * <p>Validates the combination of configuration setting and locale for consistency.</p>
     *
     * <p>This method ensures that a locale is provided only for language-dependent settings
     * and that no locale is given for language-independent ones. Violations result in an
     * {@link IllegalArgumentException} being thrown with a descriptive message.</p>
     *
     * @param setting the configuration setting to validate
     * @param locale the locale to check, or {@code null} if no locale is specified
     * @throws IllegalArgumentException if the locale does not match the setting’s language dependency
     */
    private void checkLocale(final @NotNull ConfigurationSetting setting,
                             final @Nullable Locale locale) {
        if (setting.isLanguageDependent() && locale == null) {
            throw new IllegalArgumentException(
                    "Setting '%s' is language-dependent; you need to specify a locale!"
                            .formatted(setting.setting()));
        } else if (!setting.isLanguageDependent() && locale != null) {
            throw new IllegalArgumentException(
                    "Setting '%s' is not language-dependent; do not specify a locale!"
                            .formatted(setting.setting()));
        }
    }

    /**
     * <p>Cache key combining a configuration setting with a language code.</p>
     *
     * <p>The language code is an ISO code (e.g., {@code "EN"}) or an empty string to indicate the
     * neutral (language-independent) value.</p>
     *
     * @param setting the configuration setting
     * @param language the ISO language code or empty string for neutral
     */
    private record CacheKey(@NotNull ConfigurationSetting setting, @Nullable String language) { }

}
