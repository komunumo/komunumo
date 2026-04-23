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
import app.komunumo.test.KaribuTest;
import app.komunumo.util.ImageUtil;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.streams.UploadMetadata;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ImageUploadKT extends KaribuTest {

    @Test
    void exceptionHandlingWhenStoringImageOnServer() throws Exception {
        // given
        final var imageService = mock(ImageService.class);
        final var imageDto = new ImageDto(UUID.randomUUID(), ContentType.IMAGE_PNG);
        when(imageService.storeImage(any(ImageDto.class))).thenReturn(imageDto);

        final var imageUpload = new ImageUpload(imageService);
        final var metadata = new UploadMetadata(
                "test.png",
                ContentType.IMAGE_PNG.getContentType(),
                1_234L
        );
        final var tempFile = Files.createTempFile("image-upload-test-", ".png").toFile();

        try (var mocked = mockStatic(ImageUtil.class);
             var logCaptor = LogCaptor.forClass(ImageUpload.class)) {
            mocked.when(() -> ImageUtil.storeImage(any(), any()))
                    .thenThrow(new IOException("simulated I/O failure"));

            // when
            final var method = ImageUpload.class.getDeclaredMethod(
                    "processUploadSuccess",
                    UploadMetadata.class,
                    File.class
            );
            method.setAccessible(true);
            method.invoke(imageUpload, metadata, tempFile);

            // then
            assertThat(logCaptor.getErrorLogs()).containsExactly(
                    "Failed to store uploaded image file: simulated I/O failure");
        }
    }

    @Test
    void unsupportedImageTypeShouldBeRejectedWithGenericIncorrectFileTypeMessage() throws Exception {
        // given
        final var imageService = mock(ImageService.class);
        final var imageUpload = new ImageUpload(imageService);
        final var metadata = new UploadMetadata(
                "test.gif",
                ContentType.IMAGE_GIF.getContentType(),
                1_234L
        );
        final var tempFile = Files.createTempFile("image-upload-test-", ".gif").toFile();
        UI.getCurrent().add(imageUpload);

        // when
        final var method = ImageUpload.class.getDeclaredMethod(
                "processUploadSuccess",
                UploadMetadata.class,
                File.class
        );
        method.setAccessible(true);
        method.invoke(imageUpload, metadata, tempFile);

        // then
        verify(imageService, never()).storeImage(any(ImageDto.class));
        assertThat(imageUpload.getValue()).isNull();

        final var notification = LocatorJ._get(Notification.class);
        assertThat(notification.isOpened()).isTrue();
        final var div = LocatorJ._get(notification, Div.class);
        assertThat(div.getText()).isEqualTo("The selected file is not an image. Please select a valid image to upload.");
    }
}
