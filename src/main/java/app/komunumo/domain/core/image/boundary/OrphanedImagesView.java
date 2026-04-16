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

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.infra.ui.vaadin.components.KomunumoMessageBox;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Route(value = "admin/images/orphaned", layout = WebsiteLayout.class)
@RolesAllowed("ADMIN")
public final class OrphanedImagesView extends AbstractView {

    private final @NotNull ImageService imageService;
    private final @NotNull Button cleanupButton;

    public OrphanedImagesView(final @NotNull ConfigurationService configurationService,
                              final @NotNull ImageService imageService) {
        super(configurationService);
        this.imageService = imageService;
        setId("orphaned-images-view");
        final var ui = UI.getCurrent();
        cleanupButton = new Button(getTranslation("core.image.boundary.OrphanedImagesView.cleanupButton.title"),
                _ -> Thread.startVirtualThread(() -> runCleanup(ui)));
        cleanupButton.setDisableOnClick(true);
        createUserInterface();
    }

    private void createUserInterface() {
        removeAll();
        add(new H3(getViewTitle()));
        final var orphanedImages = imageService.findOrphanedImages();
        if (orphanedImages.isEmpty()) {
            add(new KomunumoMessageBox(getTranslation("core.image.boundary.OrphanedImagesView.noOrphanedImages"),
                    KomunumoMessageBox.MessageType.SUCCESS));
        } else {
            add(new ImageGrid(orphanedImages));
            add(cleanupButton);
            cleanupButton.setEnabled(true);
        }
    }

    void runCleanup(final @Nullable UI ui) {
        imageService.cleanupOrphanedImages();
        refreshUiIfAttached(ui);
    }

    void refreshUiIfAttached(final @Nullable UI ui) {
        if (ui != null && ui.isAttached()) {
            ui.access(this::createUserInterface);
        }
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("core.image.boundary.OrphanedImagesView.title");
    }

}
