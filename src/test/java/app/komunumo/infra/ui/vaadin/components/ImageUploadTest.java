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
package app.komunumo.infra.ui.vaadin.components;

import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImageUploadTest {

    private ImageUpload imageUpload;

    @BeforeEach
    void setUp() {
        final var imageService = mock(ImageService.class);
        imageUpload = new ImageUpload(imageService);
    }

    @Test
    void setLabel() {
        assertThat(imageUpload.getLabel()).isNull();
        final var label = "Test Label";
        imageUpload.setLabel(label);
        assertThat(imageUpload.getLabel()).isEqualTo(label);
    }

    @Test
    void setReadOnly() {
        assertThat(imageUpload.isReadOnly()).isFalse();
        imageUpload.setReadOnly(true);
        assertThat(imageUpload.isReadOnly()).isTrue();
        imageUpload.setReadOnly(false);
        assertThat(imageUpload.isReadOnly()).isFalse();
    }

    @Test
    void setRequired() {
        assertThat(imageUpload.isRequired()).isFalse();
        imageUpload.setRequired(true);
        assertThat(imageUpload.isRequired()).isTrue();
        imageUpload.setRequired(false);
        assertThat(imageUpload.isRequired()).isFalse();
    }

    @Test
    void setRequiredIndicatorVisible() {
        assertThat(imageUpload.isRequiredIndicatorVisible()).isFalse();
        imageUpload.setRequiredIndicatorVisible(true);
        assertThat(imageUpload.isRequiredIndicatorVisible()).isTrue();
        imageUpload.setRequiredIndicatorVisible(false);
        assertThat(imageUpload.isRequiredIndicatorVisible()).isFalse();
    }

    @Test
    void getEmptyValue() {
        assertThat(imageUpload.getEmptyValue()).isNull();
    }

    @Test
    void generateModelValue() {
        final var testImage = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        assertThat(imageUpload.generateModelValue()).isNull();
        imageUpload.setValue(testImage);
        assertThat(imageUpload.generateModelValue()).isEqualTo(testImage);
        assertThat(imageUpload.getValue()).isEqualTo(testImage);
    }

    @Test
    void deleteShouldDoNothingWhenNoImageIsSet() throws Exception {
        // given
        final var imageService = mock(ImageService.class);
        final var imageUpload = new ImageUpload(imageService);

        // when
        final var method = ImageUpload.class.getDeclaredMethod(
                "deleteCurrentImage",
                boolean.class
        );
        method.setAccessible(true);
        method.invoke(imageUpload, false);

        // then
        verify(imageService, never()).deleteImage(any());
    }

    @Test
    void acceptedUploadContentTypesShouldOnlyAllowJpegPngAndWebp() {
        assertThat(ImageUpload.isAcceptedUploadContentType(ContentType.IMAGE_JPEG.getContentType())).isTrue();
        assertThat(ImageUpload.isAcceptedUploadContentType(ContentType.IMAGE_PNG.getContentType())).isTrue();
        assertThat(ImageUpload.isAcceptedUploadContentType(ContentType.IMAGE_WEBP.getContentType())).isTrue();
        assertThat(ImageUpload.isAcceptedUploadContentType(ContentType.IMAGE_GIF.getContentType())).isFalse();
        assertThat(ImageUpload.isAcceptedUploadContentType(ContentType.IMAGE_SVG.getContentType())).isFalse();
        assertThat(ImageUpload.isAcceptedUploadContentType("text/plain")).isFalse();
        assertThat(ImageUpload.isAcceptedUploadContentType(null)).isFalse();
    }
}
