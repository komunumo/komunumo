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

import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.util.ImageUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Dialog for displaying a single image in a constrained viewport-aware size.</p>
 */
public final class ImageDialog extends Dialog {

    public ImageDialog(final @NotNull ImageDto image) {
        super();
        addClassName("image-dialog");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setResizable(false);
        setDraggable(false);

        getHeader().removeAll();
        getFooter().removeAll();
        removeAll();

        setHeaderTitle(image.name());
        final var closeButton = new Button(new Icon("lumo", "cross"), _ -> close());
        closeButton.addThemeVariants(ButtonVariant.TERTIARY);
        getHeader().add(closeButton);

        final var imageUrl = ImageUtil.resolveImageUrl(image);
        final var displayedImage = new Image(imageUrl == null ? "" : imageUrl, image.name());
        displayedImage.addClassName("image-dialog-image");

        add(displayedImage);
    }
}
