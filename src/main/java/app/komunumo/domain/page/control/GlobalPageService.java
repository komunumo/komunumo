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
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static app.komunumo.data.db.tables.GlobalPage.GLOBAL_PAGE;

/**
 * <p>Service layer for creating, reading, updating, and deleting global pages backed by the database.</p>
 *
 * <p>Read operations are publicly available, while write operations are protected via Spring Security
 * method security. Only users with the {@code ADMIN} role are allowed to modify global pages.</p>
 *
 * <p>Timestamps are managed in UTC, setting {@code created} and {@code updated} accordingly during
 * insert and update operations.</p>
 */
@Service
public class GlobalPageService {

    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new {@code GlobalPageService} using the provided jOOQ {@link DSLContext}.</p>
     *
     * @param dsl The jOOQ DSL context used to interact with the database; must not be {@code null}.
     */
    public GlobalPageService(final @NotNull DSLContext dsl) {
        super();
        this.dsl = dsl;
    }

    /**
     * <p>Creates or updates (upserts) a global page identified by its {@code slot} and {@code language}.</p>
     *
     * <p>If a record already exists for the given slot and language, it is updated and its
     * {@code updated} timestamp is set to the current UTC time. Otherwise, a new record is inserted
     * with both {@code created} and {@code updated} set to the current UTC time.</p>
     *
     * @param globalPage The global page DTO to persist; must not be {@code null}.
     * @return The persisted global page as a DTO.
     */
    public GlobalPageDto storeGlobalPage(final @NotNull GlobalPageDto globalPage) {
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
     * <p>Retrieves a global page for the given {@code slot} and {@link Locale}.</p>
     *
     * <p>If no page exists for the requested language and the requested language is not English,
     * the method falls back to {@link Locale#ENGLISH}.</p>
     *
     * @param slot The slot identifier of the page; must not be {@code null}.
     * @param locale The desired locale; must not be {@code null}.
     * @return An {@link Optional} containing the page if found, otherwise empty.
     */
    public @NotNull Optional<GlobalPageDto> getGlobalPage(final @NotNull String slot,
                                                          final @NotNull Locale locale) {
        final var languageCode = LocaleUtil.getLanguageCode(locale);
        final var globalPage = dsl.selectFrom(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.SLOT.eq(slot))
                .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode))
                .fetchOptionalInto(GlobalPageDto.class);

        if (globalPage.isEmpty() && !languageCode.equals("en")) {
            return getGlobalPage(slot, Locale.ENGLISH);
        }

        return globalPage;
    }

    /**
     * <p>Returns at most one global page per slot for the given {@link Locale}, preferring the requested
     * language and falling back to English where necessary.</p>
     *
     * @param locale The desired locale; must not be {@code null}.
     * @return A list containing one page per slot in the preferred language or English fallback.
     */
    public @NotNull List<@NotNull GlobalPageDto> getGlobalPages(final @NotNull Locale locale) {
        final var preferredLanguageCode = LocaleUtil.getLanguageCode(locale);
        final var fallbackLanguageCode = "en";

        // Load all pages in the desired language + fallback language
        final var pages = dsl.selectFrom(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.LANGUAGE.in(preferredLanguageCode, fallbackLanguageCode))
                .fetchStreamInto(GlobalPageDto.class);

        // Keep only the page in the desired language per slot, or else fallback
        final var pageMap = pages.collect(Collectors.toMap(
                GlobalPageDto::slot,
                Function.identity(),
                (preferred, fallback) ->
                        preferred.language().equals(locale) ? preferred : fallback
        ));

        return pageMap.values().stream().toList();
    }

    /**
     * <p>Retrieves all global pages regardless of slot or language.</p>
     *
     * @return A list of all global page DTOs.
     */
    public @NotNull List<@NotNull GlobalPageDto> getAllGlobalPages() {
        return dsl.selectFrom(GLOBAL_PAGE)
                .fetchInto(GlobalPageDto.class);
    }

    /**
     * <p>Counts the total number of global pages.</p>
     *
     * @return The total count of global pages; never negative.
     */
    public int getGlobalPageCount() {
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
     * <p>The {@code updated} timestamp is set to the current UTC time. Returns {@code true} if exactly
     * one record was modified; otherwise {@code false}.</p>
     *
     *
     * @param globalPage The page whose slot and language identify the record to update; must not be {@code null}.
     * @param title The new title to set; must not be {@code null}.
     * @param markdown The new markdown content to set; must not be {@code null}.
     * @return {@code true} if exactly one row was updated; otherwise {@code false}.
     */
    public boolean updateGlobalPage(final @NotNull GlobalPageDto globalPage,
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
                .execute() == 1;
    }

    /**
     * <p>Deletes the specified global page identified by its {@code slot} and {@code language}.</p>
     *
     * <p>Returns {@code true} if exactly one record was removed; otherwise {@code false}.</p>
     *
     *
     * @param globalPage The page whose slot and language identify the record(s) to delete; must not be {@code null}.
     * @return {@code true} if exactly one row was deleted; otherwise {@code false}.
     */
    public boolean deleteGlobalPage(final @NotNull GlobalPageDto globalPage) {
        final var slot = globalPage.slot();
        final var languageCode = LocaleUtil.getLanguageCode(globalPage.language());
        return dsl.delete(GLOBAL_PAGE)
                .where(GLOBAL_PAGE.SLOT.eq(slot))
                .and(GLOBAL_PAGE.LANGUAGE.eq(languageCode))
                .execute() == 1;
    }

}
