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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class ImageCardKT extends KaribuTest {

    @Test
    @SuppressWarnings("DataFlowIssue")
    void cardShouldRenderImageAndOpenDialogOnClick() {
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        final var imageCard = new ImageCard(image);
        UI.getCurrent().add(imageCard);

        assertThat(imageCard.getClassNames())
                .contains("komunumo-card", "image-card", "clickable");

        final var displayedImage = _get(imageCard, Image.class);
        assertThat(displayedImage).isNotNull();
        assertThat(displayedImage.getSrc()).isNotNull().isNotBlank().contains(image.id().toString());
        assertThat(displayedImage.getAlt().orElseThrow()).isEqualTo(image.name());

        _click(imageCard);
        assertThat(_get(ImageDialog.class).isOpened()).isTrue();
    }
}
