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
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static app.komunumo.data.db.tables.Config.CONFIG;

/**
 * <p>Handles persistence operations for application configuration values.</p>
 *
 * <p>This store encapsulates all jOOQ database access for counting, loading, upserting,
 * deleting, and bulk-deleting values from the {@code config} table.</p>
 */
@Service
final class ConfigurationStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new configuration store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     */
    ConfigurationStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Counts the total number of stored configuration rows.</p>
     *
     * @return the total row count; never negative
     */
    int getConfigurationCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(CONFIG)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Loads a single configuration value by setting key and language code.</p>
     *
     * @param setting the configuration setting to load
     * @param language the language code (for example {@code "en"}) or empty string for neutral values
     * @return the stored value if present; otherwise empty
     */
    @NotNull Optional<String> getConfigurationValue(final @NotNull ConfigurationSetting setting,
                                                           final @NotNull String language) {
        return dsl.select(CONFIG.VALUE)
                .from(CONFIG)
                .where(CONFIG.SETTING.eq(setting.setting()))
                .and(CONFIG.LANGUAGE.eq(language))
                .fetchOptional(CONFIG.VALUE);
    }

    /**
     * <p>Stores or updates a configuration value for a specific language code.</p>
     *
     * @param setting the configuration setting to write
     * @param language the language code (for example {@code "en"}) or empty string for neutral values
     * @param value the value to persist
     */
    void upsertConfigurationValue(final @NotNull ConfigurationSetting setting,
                                         final @NotNull String language,
                                         final @NotNull String value) {
        dsl.insertInto(CONFIG)
                .set(CONFIG.SETTING, setting.setting())
                .set(CONFIG.LANGUAGE, language)
                .set(CONFIG.VALUE, value)
                .onDuplicateKeyUpdate()
                .set(CONFIG.VALUE, value)
                .execute();
    }

    /**
     * <p>Deletes a configuration value by setting key and language code.</p>
     *
     * @param setting the configuration setting to delete
     * @param language the language code (for example {@code "en"}) or empty string for neutral values
     * @return the number of deleted rows
     */
    int deleteConfigurationValue(final @NotNull ConfigurationSetting setting,
                                        final @NotNull String language) {
        return dsl.deleteFrom(CONFIG)
                .where(CONFIG.SETTING.eq(setting.setting()))
                .and(CONFIG.LANGUAGE.eq(language))
                .execute();
    }

    /**
     * <p>Deletes all configuration rows.</p>
     *
     * @return the number of deleted rows
     */
    int deleteAllConfigurations() {
        return dsl.delete(CONFIG).execute();
    }

}
