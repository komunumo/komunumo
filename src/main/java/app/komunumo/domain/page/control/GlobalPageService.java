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

import app.komunumo.domain.page.entity.GlobalPageDto;
import app.komunumo.util.LocaleUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * <p>Provides global page use cases and delegates persistence operations to {@link GlobalPageStore}.</p>
 *
 * <p>This service contains orchestration logic such as language fallback handling, while database
 * access is encapsulated in the store implementation.</p>
 */
@Service
public class GlobalPageService {

    private final @NotNull GlobalPageStore globalPageStore;

    /**
     * <p>Creates a new {@code GlobalPageService} using the provided {@link GlobalPageStore}.</p>
     *
     * @param globalPageStore the store used to interact with the database; must not be {@code null}
     */
    GlobalPageService(final @NotNull GlobalPageStore globalPageStore) {
        super();
        this.globalPageStore = globalPageStore;
    }

    /**
     * <p>Creates or updates a global page.</p>
     *
     * @param globalPage the global page DTO to persist; must not be {@code null}
     * @return the persisted global page as a DTO
     */
    public GlobalPageDto storeGlobalPage(final @NotNull GlobalPageDto globalPage) {
        return globalPageStore.storeGlobalPage(globalPage);
    }

    /**
     * <p>Retrieves a global page for the given {@code slot} and {@link Locale}.</p>
     *
     * <p>If no page exists for the requested language and the requested language is not English,
     * the method falls back to {@link Locale#ENGLISH}.</p>
     *
     * @param slot the slot identifier of the page; must not be {@code null}
     * @param locale the desired locale; must not be {@code null}
     * @return an {@link Optional} containing the page if found, otherwise empty
     */
    public @NotNull Optional<GlobalPageDto> getGlobalPage(final @NotNull String slot,
                                                          final @NotNull Locale locale) {
        final var languageCode = LocaleUtil.getLanguageCode(locale);
        final var globalPage = globalPageStore.getGlobalPage(slot, languageCode);

        if (globalPage.isEmpty() && !languageCode.equals("en")) {
            return getGlobalPage(slot, Locale.ENGLISH);
        }

        return globalPage;
    }

    /**
     * <p>Returns at most one global page per slot for the given {@link Locale}, preferring the requested
     * language and falling back to English where necessary.</p>
     *
     * @param locale the desired locale; must not be {@code null}
     * @return a list containing one page per slot in the preferred language or English fallback
     */
    public @NotNull List<@NotNull GlobalPageDto> getGlobalPages(final @NotNull Locale locale) {
        final var preferredLanguageCode = LocaleUtil.getLanguageCode(locale);
        final var fallbackLanguageCode = "en";

        // Load all pages in the desired language + fallback language
        final var pages = globalPageStore.getGlobalPages(preferredLanguageCode, fallbackLanguageCode);

        // Keep only the page in the desired language per slot, or else fallback
        final var pageMap = pages.stream().collect(toMap(
                GlobalPageDto::slot, Function.identity(),
                (preferred, fallback) -> preferred.language().equals(locale) ? preferred : fallback
        ));

        return pageMap.values().stream().toList();
    }

    /**
     * <p>Retrieves all global pages regardless of slot or language.</p>
     *
     * @return a list of all global page DTOs
     */
    public @NotNull List<@NotNull GlobalPageDto> getAllGlobalPages() {
        return globalPageStore.getAllGlobalPages();
    }

    /**
     * <p>Counts the total number of global pages.</p>
     *
     * @return the total count of global pages; never negative
     */
    public int getGlobalPageCount() {
        return globalPageStore.getGlobalPageCount();
    }

    /**
     * <p>Updates the title and markdown of an existing global page identified by its {@code slot}
     * and {@code language}.</p>
     *
     * <p>Returns {@code true} if exactly one record was modified; otherwise {@code false}.</p>
     *
     * @param globalPage the page whose slot and language identify the record to update; must not be {@code null}
     * @param title the new title to set; must not be {@code null}
     * @param markdown the new markdown content to set; must not be {@code null}
     * @return {@code true} if exactly one row was updated; otherwise {@code false}
     */
    public boolean updateGlobalPage(final @NotNull GlobalPageDto globalPage,
                                    final @NotNull String title,
                                    final @NotNull String markdown) {
        return globalPageStore.updateGlobalPage(globalPage, title, markdown) == 1;
    }

    /**
     * <p>Deletes the specified global page identified by its {@code slot} and {@code language}.</p>
     *
     * <p>Returns {@code true} if exactly one record was removed; otherwise {@code false}.</p>
     * @param globalPage the page whose slot and language identify the record(s) to delete; must not be {@code null}
     * @return {@code true} if the global page was deleted; otherwise {@code false}
     */
    public boolean deleteGlobalPage(final @NotNull GlobalPageDto globalPage) {
        return globalPageStore.deleteGlobalPage(globalPage) > 0;
    }

}
