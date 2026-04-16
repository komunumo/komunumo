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
import app.komunumo.infra.ui.vaadin.components.KomunumoGrid;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ImageGrid extends KomunumoGrid {

    public ImageGrid(final @NotNull List<ImageDto> images) {
        super(images.stream()
                .map(ImageCard::new)
                .toArray(ImageCard[]::new));
        addClassName("image-grid");
    }

}
