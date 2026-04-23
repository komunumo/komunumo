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
package app.komunumo.domain.core.image.boundary;

import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.infra.ui.vaadin.components.KomunumoMessageBox;
import app.komunumo.test.KaribuTest;
import com.github.mvysny.kaributesting.v10.MockAccessDeniedException;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrphanedImagesViewKT extends KaribuTest {

    @MockitoBean
    private ImageService imageService;

    @Test
    void userRoleShouldNotAccessOrphanedImagesView() {
        login(getTestUser(UserRole.USER));

        assertThatThrownBy(() -> UI.getCurrent().navigate("admin/images/orphaned"))
                .isInstanceOf(MockAccessDeniedException.class);
    }

    @Test
    void adminShouldSeeMessageWhenNoOrphanedImagesExist() {
        when(imageService.findOrphanedImages()).thenReturn(List.of());
        login(getTestUser(UserRole.ADMIN));

        UI.getCurrent().navigate(OrphanedImagesView.class);

        final var view = _get(OrphanedImagesView.class);
        assertThat(view.getId().orElseThrow()).isEqualTo("orphaned-images-view");
        assertThat(_get(view, KomunumoMessageBox.class).getText())
                .isEqualTo("No orphaned images found.");
        assertThat(_find(view, Button.class)).isEmpty();
    }

    @Test
    void adminShouldSeeImageGridAndCleanupButtonWhenOrphanedImagesExist() {
        final var orphanedImage = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        when(imageService.findOrphanedImages()).thenReturn(List.of(orphanedImage));
        login(getTestUser(UserRole.ADMIN));

        UI.getCurrent().navigate(OrphanedImagesView.class);

        final var view = _get(OrphanedImagesView.class);
        assertThat(_get(view, ImageGrid.class)).isNotNull();
        assertThat(_find(view, ImageCard.class)).hasSize(1);
        assertThat(_get(view, Button.class, spec -> spec.withText("Delete Orphaned Images")).isEnabled()).isTrue();
    }

    @Test
    void cleanupShouldInvokeServiceAndRefreshView() {
        final var orphanedImage = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        when(imageService.findOrphanedImages()).thenReturn(List.of(orphanedImage), List.of());
        login(getTestUser(UserRole.ADMIN));

        UI.getCurrent().navigate(OrphanedImagesView.class);

        final var view = _get(OrphanedImagesView.class);
        final var cleanupButton = _get(view, Button.class, spec -> spec.withText("Delete Orphaned Images"));
        _click(cleanupButton);
        assertThat(cleanupButton.isEnabled()).isFalse();

        await().atMost(2, SECONDS).untilAsserted(() -> verify(imageService).cleanupOrphanedImages());
        MockVaadin.clientRoundtrip(false);
        await().atMost(2, SECONDS).untilAsserted(() ->
                assertThat(_get(view, KomunumoMessageBox.class).getText()).isEqualTo("No orphaned images found."));
        verify(imageService, times(2)).findOrphanedImages();
    }

    @Test
    void refreshUiIfAttachedShouldIgnoreNullUi() {
        when(imageService.findOrphanedImages()).thenReturn(List.of());
        login(getTestUser(UserRole.ADMIN));
        UI.getCurrent().navigate(OrphanedImagesView.class);
        final var view = _get(OrphanedImagesView.class);

        assertThatCode(() -> view.refreshUiIfAttached(null)).doesNotThrowAnyException();
    }

    @Test
    void refreshUiIfAttachedShouldIgnoreDetachedUi() {
        when(imageService.findOrphanedImages()).thenReturn(List.of());
        login(getTestUser(UserRole.ADMIN));
        UI.getCurrent().navigate(OrphanedImagesView.class);
        final var view = _get(OrphanedImagesView.class);

        final var ui = mock(UI.class);
        when(ui.isAttached()).thenReturn(false);

        view.refreshUiIfAttached(ui);

        verify(ui).isAttached();
        verify(ui, never()).access(any());
    }
}
