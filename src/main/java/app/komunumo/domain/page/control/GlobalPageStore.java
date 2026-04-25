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
package app.komunumo.domain.page.control;

import app.komunumo.data.db.tables.records.GlobalPageRecord;
import app.komunumo.domain.page.entity.GlobalPageDto;
import app.komunumo.util.LocaleUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static app.komunumo.data.db.tables.GlobalPage.GLOBAL_PAGE;

/**
 * <p>Store layer for persisting and loading global pages.</p>
 */
@Service
final class GlobalPageStore {

    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new {@code GlobalPageStore} using the provided jOOQ {@link DSLContext}.</p>
     *
     * @param dsl the jOOQ DSL context used to interact with the database; must not be {@code null}
     */
    GlobalPageStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Creates or updates (upserts) a global page identified by its {@code slot} and {@code language}.</p>
     *
     * <p>If a record already exists for the given slot and language, it is updated and its
     * {@code updated} timestamp is set to the current UTC time. Otherwise, a new record is inserted
     * with both {@code created} and {@code updated} set to the current UTC time.</p>
     *
     * @param globalPage the global page DTO to persist; must not be {@code null}
     * @return the persisted global page as a DTO
     */
    @NotNull GlobalPageDto storeGlobalPage(final @NotNull GlobalPageDto globalPage) {
        final var slot = globalPage.slot();
        final var languageCode = LocaleUtil.getLanguageCode(globalPage.language());
        final GlobalPageRecord globalPageRecord = dsl.selectFrom(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.SLOT.eq(slot))
                .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode))
                .fetchOptional()
                .orElse(dsl.newRecord(GLOBAL_PAGE));
        globalPageRecord.from(globalPage);
        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        if (globalPageRecord.getCreated() == null) { // NOSONAR (false positive: date may be null for new global pages)
            globalPageRecord.setCreated(now);
            globalPageRecord.setUpdated(now);
        } else {
            globalPageRecord.setUpdated(now);
        }
        globalPageRecord.store();
        return globalPageRecord.into(GlobalPageDto.class);
    }

    /**
     * <p>Retrieves a global page for the given {@code slot} and language code.</p>
     *
     * @param slot the slot identifier of the page; must not be {@code null}
     * @param languageCode the language code to query; must not be {@code null}
     * @return an {@link Optional} containing the page if found, otherwise empty
     */
    @NotNull Optional<GlobalPageDto> getGlobalPage(final @NotNull String slot,
                                                           final @NotNull String languageCode) {
        return dsl.selectFrom(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.SLOT.eq(slot))
                .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode))
                .fetchOptionalInto(GlobalPageDto.class);
    }

    /**
     * <p>Retrieves global pages for the preferred and fallback language codes.</p>
     *
     * @param preferredLanguageCode the preferred language code; must not be {@code null}
     * @param fallbackLanguageCode the fallback language code; must not be {@code null}
     * @return a list of matching pages
     */
    @NotNull List<@NotNull GlobalPageDto> getGlobalPages(final @NotNull String preferredLanguageCode,
                                                                final @NotNull String fallbackLanguageCode) {
        return dsl.selectFrom(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.LANGUAGE.in(preferredLanguageCode, fallbackLanguageCode))
                .fetchInto(GlobalPageDto.class);
    }

    /**
     * <p>Retrieves all global pages regardless of slot or language.</p>
     *
     * @return a list of all global page DTOs
     */
    @NotNull List<@NotNull GlobalPageDto> getAllGlobalPages() {
        return dsl.selectFrom(GLOBAL_PAGE)
                .fetchInto(GlobalPageDto.class);
    }

    /**
     * <p>Counts the total number of global pages.</p>
     *
     * @return the total count of global pages; never negative
     */
    int getGlobalPageCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(GLOBAL_PAGE)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Updates the title and markdown of an existing global page identified by its {@code slot}
     * and {@code language}.</p>
     *
     * <p>The {@code updated} timestamp is set to the current UTC time.</p>
     *
     * @param globalPage the page whose slot and language identify the record to update; must not be {@code null}
     * @param title the new title to set; must not be {@code null}
     * @param markdown the new markdown content to set; must not be {@code null}
     * @return the number of updated rows
     */
    int updateGlobalPage(final @NotNull GlobalPageDto globalPage,
                                final @NotNull String title,
                                final @NotNull String markdown) {
        final var slot = globalPage.slot();
        final var languageCode = LocaleUtil.getLanguageCode(globalPage.language());
        return dsl.update(GLOBAL_PAGE)
                .set(GLOBAL_PAGE.TITLE, title)
                .set(GLOBAL_PAGE.MARKDOWN, markdown)
                .set(GLOBAL_PAGE.UPDATED, ZonedDateTime.now(ZoneOffset.UTC))
                .where(GLOBAL_PAGE.SLOT.eq(slot)
                        .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode)))
                .execute();
    }

    /**
     * <p>Deletes the specified global page identified by its {@code slot} and {@code language}.</p>
     *
     * @param globalPage the page whose slot and language identify the record(s) to delete; must not be {@code null}
     * @return the number of deleted rows
     */
    int deleteGlobalPage(final @NotNull GlobalPageDto globalPage) {
        final var slot = globalPage.slot();
        final var languageCode = LocaleUtil.getLanguageCode(globalPage.language());
        return dsl.delete(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.SLOT.eq(slot))
                .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode))
                .execute();
    }
}
