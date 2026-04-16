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

import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.test.KaribuTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImageGridKT extends KaribuTest {

    @Test
    void gridShouldRenderCardsForAllImages() {
        final var imageOne = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_SVG);
        final var imageTwo = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);

        final var imageGrid = new ImageGrid(List.of(imageOne, imageTwo));

        assertThat(imageGrid.getClassNames()).contains("komunumo-grid", "image-grid");
        final var children = imageGrid.getChildren().toList();
        assertThat(children).hasSize(2).allMatch(ImageCard.class::isInstance);
    }

    @Test
    void gridShouldSupportEmptyImageList() {
        final var imageGrid = new ImageGrid(List.of());
        assertThat(imageGrid.getChildren().toList()).isEmpty();
    }
}
