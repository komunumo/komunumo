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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GlobalPageServiceTest {

    @Test
    void storeGlobalPageDelegatesToStore() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var input = globalPage("imprint", Locale.GERMAN);
        final var expected = globalPage("imprint", Locale.GERMAN);
        when(store.storeGlobalPage(input)).thenReturn(expected);

        final var result = service.storeGlobalPage(input);

        assertThat(result).isEqualTo(expected);
        verify(store).storeGlobalPage(input);
    }

    @Test
    void getGlobalPageReturnsRequestedLanguageWhenFound() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var expected = globalPage("imprint", Locale.GERMAN);
        when(store.getGlobalPage("imprint", "de")).thenReturn(Optional.of(expected));

        final var result = service.getGlobalPage("imprint", Locale.GERMAN);

        assertThat(result).contains(expected);
        verify(store).getGlobalPage("imprint", "de");
    }

    @Test
    void getGlobalPageFallsBackToEnglishWhenRequestedLanguageMissing() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var expected = globalPage("imprint", Locale.ENGLISH);
        when(store.getGlobalPage("imprint", "de")).thenReturn(Optional.empty());
        when(store.getGlobalPage("imprint", "en")).thenReturn(Optional.of(expected));

        final var result = service.getGlobalPage("imprint", Locale.GERMAN);

        assertThat(result).contains(expected);
        verify(store).getGlobalPage("imprint", "de");
        verify(store).getGlobalPage("imprint", "en");
    }

    @Test
    void getGlobalPageForEnglishDoesNotFallbackRecursively() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        when(store.getGlobalPage("imprint", "en")).thenReturn(Optional.empty());

        final var result = service.getGlobalPage("imprint", Locale.ENGLISH);

        assertThat(result).isEmpty();
        verify(store).getGlobalPage("imprint", "en");
    }

    @Test
    void getGlobalPageReturnsEmptyWhenRequestedAndFallbackAreMissing() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        when(store.getGlobalPage("imprint", "de")).thenReturn(Optional.empty());
        when(store.getGlobalPage("imprint", "en")).thenReturn(Optional.empty());

        final var result = service.getGlobalPage("imprint", Locale.GERMAN);

        assertThat(result).isEmpty();
        verify(store).getGlobalPage("imprint", "de");
        verify(store).getGlobalPage("imprint", "en");
    }

    @Test
    void getGlobalPagesKeepsPreferredLanguageWhenPreferredComesFirst() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var preferred = globalPage("imprint", Locale.GERMAN);
        final var fallback = globalPage("imprint", Locale.ENGLISH);
        when(store.getGlobalPages("de", "en")).thenReturn(List.of(preferred, fallback));

        final var result = service.getGlobalPages(Locale.GERMAN);

        assertThat(result).containsExactly(preferred);
        verify(store).getGlobalPages("de", "en");
    }

    @Test
    void getGlobalPagesReplacesFallbackWithPreferredWhenPreferredComesSecond() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var fallback = globalPage("imprint", Locale.ENGLISH);
        final var preferred = globalPage("imprint", Locale.GERMAN);
        when(store.getGlobalPages("de", "en")).thenReturn(List.of(fallback, preferred));

        final var result = service.getGlobalPages(Locale.GERMAN);

        assertThat(result).containsExactly(preferred);
        verify(store).getGlobalPages("de", "en");
    }

    @Test
    void getGlobalPagesReturnsMultipleSlots() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var imprint = globalPage("imprint", Locale.GERMAN);
        final var contact = globalPage("contact", Locale.ENGLISH);
        when(store.getGlobalPages("de", "en")).thenReturn(List.of(imprint, contact));

        final var result = service.getGlobalPages(Locale.GERMAN);

        assertThat(result).containsExactlyInAnyOrder(imprint, contact);
        verify(store).getGlobalPages("de", "en");
    }

    @Test
    void getAllGlobalPagesDelegatesToStore() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var expected = List.of(globalPage("imprint", Locale.ENGLISH));
        when(store.getAllGlobalPages()).thenReturn(expected);

        final var result = service.getAllGlobalPages();

        assertThat(result).isEqualTo(expected);
        verify(store).getAllGlobalPages();
    }

    @Test
    void getGlobalPageCountDelegatesToStore() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        when(store.getGlobalPageCount()).thenReturn(7);

        final var result = service.getGlobalPageCount();

        assertThat(result).isEqualTo(7);
        verify(store).getGlobalPageCount();
    }

    @Test
    void updateGlobalPageReturnsTrueWhenExactlyOneRowUpdated() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var page = globalPage("imprint", Locale.ENGLISH);
        when(store.updateGlobalPage(page, "New Title", "New Markdown")).thenReturn(1);

        final var result = service.updateGlobalPage(page, "New Title", "New Markdown");

        assertThat(result).isTrue();
        verify(store).updateGlobalPage(page, "New Title", "New Markdown");
    }

    @Test
    void updateGlobalPageReturnsFalseWhenNoRowsUpdated() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var page = globalPage("imprint", Locale.ENGLISH);
        when(store.updateGlobalPage(page, "New Title", "New Markdown")).thenReturn(0);

        final var result = service.updateGlobalPage(page, "New Title", "New Markdown");

        assertThat(result).isFalse();
        verify(store).updateGlobalPage(page, "New Title", "New Markdown");
    }

    @Test
    void deleteGlobalPageReturnsTrueWhenRowsDeleted() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var page = globalPage("imprint", Locale.ENGLISH);
        when(store.deleteGlobalPage(page)).thenReturn(1);

        final var result = service.deleteGlobalPage(page);

        assertThat(result).isTrue();
        verify(store).deleteGlobalPage(page);
    }

    @Test
    void deleteGlobalPageReturnsFalseWhenNoRowsDeleted() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var page = globalPage("imprint", Locale.ENGLISH);
        when(store.deleteGlobalPage(page)).thenReturn(0);

        final var result = service.deleteGlobalPage(page);

        assertThat(result).isFalse();
        verify(store).deleteGlobalPage(page);
    }

    @Test
    void serviceOnlyInteractsWithStore() {
        final var store = mock(GlobalPageStore.class);
        final var service = new GlobalPageService(store);
        final var page = globalPage("imprint", Locale.ENGLISH);
        when(store.storeGlobalPage(page)).thenReturn(page);

        service.storeGlobalPage(page);

        verify(store).storeGlobalPage(page);
        verifyNoMoreInteractions(store);
    }

    private static GlobalPageDto globalPage(final String slot, final Locale locale) {
        return new GlobalPageDto(slot, locale, null, null, "Title " + slot, "## " + slot);
    }
}
