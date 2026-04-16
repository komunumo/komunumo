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
package app.komunumo.domain.core.image.control;

import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.util.ImageUtil;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceTest {

    @Test
    void storeImageDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        when(imageStore.storeImage(image)).thenReturn(image);

        final var result = service.storeImage(image);

        assertThat(result).isEqualTo(image);
        verify(imageStore).storeImage(image);
    }

    @Test
    void getImageDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var id = UUID.randomUUID();
        final var image = new ImageDto(id, ContentType.IMAGE_WEBP);
        when(imageStore.getImage(id)).thenReturn(Optional.of(image));

        final var result = service.getImage(id);

        assertThat(result).contains(image);
        verify(imageStore).getImage(id);
    }

    @Test
    void getImageWithNullIdDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        when(imageStore.getImage(null)).thenReturn(Optional.empty());

        final var result = service.getImage(null);

        assertThat(result).isEmpty();
        verify(imageStore).getImage(null);
    }

    @Test
    void getImagesDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var expected = List.of(new ImageDto(UUID.randomUUID(), ContentType.IMAGE_SVG));
        when(imageStore.getImages()).thenReturn(expected);

        final var result = service.getImages();

        assertThat(result).isEqualTo(expected);
        verify(imageStore).getImages();
    }

    @Test
    void getImageCountDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        when(imageStore.getImageCount()).thenReturn(4);

        final var result = service.getImageCount();

        assertThat(result).isEqualTo(4);
        verify(imageStore).getImageCount();
    }

    @Test
    void findOrphanedImagesDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var expected = List.of(new ImageDto(UUID.randomUUID(), ContentType.IMAGE_GIF));
        when(imageStore.findOrphanedImages()).thenReturn(expected);

        final var result = service.findOrphanedImages();

        assertThat(result).isEqualTo(expected);
        verify(imageStore).findOrphanedImages();
    }

    @Test
    void cleanupOrphanedImagesDeletesOrphansAndCleansFiles() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var orphan1 = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        final var orphan2 = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_WEBP);
        when(imageStore.findOrphanedImages()).thenReturn(List.of(orphan1, orphan2));
        when(imageStore.deleteImage(orphan1)).thenReturn(1);
        when(imageStore.deleteImage(orphan2)).thenReturn(1);

        try (var imageUtilMock = mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(orphan1)).thenReturn(null);
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(orphan2)).thenReturn(null);

            service.cleanupOrphanedImages();

            verify(imageStore).deleteImage(orphan1);
            verify(imageStore).deleteImage(orphan2);
            imageUtilMock.verify(() -> ImageUtil.cleanupOrphanedImageFiles(service));
        }
    }

    @Test
    void getAllImageIdsDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var expected = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(imageStore.getAllImageIds()).thenReturn(expected);

        final var result = service.getAllImageIds();

        assertThat(result).isEqualTo(expected);
        verify(imageStore).getAllImageIds();
    }

    @Test
    void getAllImagesDelegatesToStore() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var expected = List.of(new ImageDto(UUID.randomUUID(), ContentType.IMAGE_SVG));
        when(imageStore.getAllImages()).thenReturn(expected);

        final var result = service.getAllImages();

        assertThat(result).isEqualTo(expected);
        verify(imageStore).getAllImages();
    }

    @Test
    void deleteImageDeletesFileAndReturnsTrueWhenStoreDeletesRow() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        final var path = Path.of("target", "test-image.png");
        when(imageStore.deleteImage(image)).thenReturn(1);

        try (var imageUtilMock = mockStatic(ImageUtil.class);
             var filesMock = mockStatic(Files.class)) {
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(image)).thenReturn(path);
            filesMock.when(() -> Files.deleteIfExists(path)).thenReturn(true);

            final var result = service.deleteImage(image);

            assertThat(result).isTrue();
            filesMock.verify(() -> Files.deleteIfExists(path));
            verify(imageStore).deleteImage(image);
        }
    }

    @Test
    void deleteImageReturnsFalseWhenStoreDeletesNoRow() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        final var path = Path.of("target", "test-image-missing-row.png");
        when(imageStore.deleteImage(image)).thenReturn(0);

        try (var imageUtilMock = mockStatic(ImageUtil.class);
             var filesMock = mockStatic(Files.class)) {
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(image)).thenReturn(path);
            filesMock.when(() -> Files.deleteIfExists(path)).thenReturn(true);

            final var result = service.deleteImage(image);

            assertThat(result).isFalse();
            filesMock.verify(() -> Files.deleteIfExists(path));
            verify(imageStore).deleteImage(image);
        }
    }

    @Test
    void deleteImageSkipsFileDeletionWhenNoPathAndStillDeletesRow() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_SVG);
        when(imageStore.deleteImage(image)).thenReturn(1);

        try (var imageUtilMock = mockStatic(ImageUtil.class);
             var filesMock = mockStatic(Files.class)) {
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(image)).thenReturn(null);

            final var result = service.deleteImage(image);

            assertThat(result).isTrue();
            filesMock.verifyNoInteractions();
            verify(imageStore).deleteImage(image);
        }
    }

    @Test
    void deleteImageLogsErrorWhenFileDeletionFailsAndStillDeletesRow() {
        final var imageStore = mock(ImageStore.class);
        final var service = new ImageService(imageStore);
        final var image = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_WEBP);
        final var path = Path.of("target", "broken-image.webp");
        when(imageStore.deleteImage(image)).thenReturn(1);

        try (var imageUtilMock = mockStatic(ImageUtil.class);
             var filesMock = mockStatic(Files.class);
             var logCaptor = LogCaptor.forClass(ImageService.class)) {
            imageUtilMock.when(() -> ImageUtil.resolveImagePath(image)).thenReturn(path);
            filesMock.when(() -> Files.deleteIfExists(path)).thenThrow(new IOException("I/O failure"));

            final var result = service.deleteImage(image);

            assertThat(result).isTrue();
            assertThat(logCaptor.getErrorLogs()).anySatisfy(log ->
                    assertThat(log).isEqualTo("Failed to delete image file: " + path.toAbsolutePath()));
            verify(imageStore).deleteImage(image);
        }
    }
}
