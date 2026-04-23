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
import app.komunumo.util.ImageUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static app.komunumo.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.notification.NotificationVariant.ERROR;

public final class ImageUpload extends CustomField<ImageDto> {

    private static final int MAX_FILE_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10MB
    private static final @NotNull String[] ACCEPTED_MIME_TYPES = {
            ContentType.IMAGE_JPEG.getContentType(),
            ContentType.IMAGE_PNG.getContentType(),
            ContentType.IMAGE_WEBP.getContentType()
    };
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ImageUpload.class);

    private final @NotNull ImageService imageService;

    private final @NotNull Upload upload;
    private final @NotNull Image preview;
    private final @NotNull Button deleteButton;

    private @Nullable ImageDto currentImage;
    private boolean imageFromUpload;

    public ImageUpload(final @NotNull ImageService imageService) {
        this.imageService = imageService;

        final var layout = new VerticalLayout();
        layout.addClassName("image-upload");

        final var uploadHandler = UploadHandler
                .toTempFile(this::processUploadSuccess);

        final var uploadI18N = new UploadI18N();
        uploadI18N.setAddFiles(new UploadI18N.AddFiles().setOne(
                getTranslation("vaadin.components.ImageUpload.uploadButton")));
        uploadI18N.setDropFiles(new UploadI18N.DropFiles().setOne(
                getTranslation("vaadin.components.ImageUpload.uploadDrop")));
        uploadI18N.setError(new UploadI18N.Error().setIncorrectFileType(
                getTranslation("vaadin.components.ImageUpload.uploadIncorrectFileType")));

        upload = new Upload(uploadHandler);
        upload.setSizeFull();
        upload.setMaxFileSize(MAX_FILE_SIZE_IN_BYTES);
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);
        upload.setI18n(uploadI18N);
        upload.addFileRemovedListener(_ -> deleteCurrentImage(false));

        preview = new Image();
        preview.setVisible(false);
        preview.addClassName("image-upload-preview");

        deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addClassName("image-upload-delete");
        deleteButton.setText(getTranslation("vaadin.components.ImageUpload.deleteButton"));
        deleteButton.setVisible(false);
        deleteButton.addClickListener(_ -> deleteCurrentImage(true));

        layout.add(upload, preview, deleteButton);
        add(layout);
    }

    private void processUploadSuccess(final @NotNull UploadMetadata metadata, final @NotNull File file) {
        file.deleteOnExit();

        if (!isAcceptedUploadContentType(metadata.contentType())) {
            showNotification(getTranslation("vaadin.components.ImageUpload.uploadIncorrectFileType"), ERROR);
            return;
        }

        final var contentType = ContentType.fromContentType(metadata.contentType());
        final var newImage = imageService.storeImage(new ImageDto(null, contentType));

        try {
            ImageUtil.storeImage(newImage, file.toPath());

            currentImage = newImage;
            imageFromUpload = true;

            updatePreview();
            setModelValue(currentImage, true);
        } catch (final IOException e) {
            LOGGER.error("Failed to store uploaded image file: {}", e.getMessage(), e);
            showNotification(getTranslation("vaadin.components.ImageUpload.storeError"),
                    NotificationVariant.ERROR);
        }
    }

    @VisibleForTesting
    static boolean isAcceptedUploadContentType(final @Nullable String contentType) {
        if (contentType == null) {
            return false;
        }
        for (final var acceptedMimeType : ACCEPTED_MIME_TYPES) {
            if (acceptedMimeType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    private void deleteCurrentImage(final boolean clearUploadFileList) {
        if (currentImage == null) {
            return;
        }

        if (imageFromUpload) {
            imageService.deleteImage(currentImage);
        }

        currentImage = null;
        imageFromUpload = false;

        if (clearUploadFileList) {
            upload.clearFileList();
        }

        updatePreview();
        setModelValue(null, true);
    }


    private void updatePreview() {
        if (currentImage == null) {
            preview.setSrc("");
            preview.setVisible(false);
            deleteButton.setVisible(false);
            return;
        }

        preview.setSrc(ImageUtil.resolveImageUrl(currentImage));
        preview.setVisible(true);
        deleteButton.setVisible(true);
    }

    @Override
    protected ImageDto generateModelValue() {
        return currentImage;
    }

    @Override
    protected void setPresentationValue(final @Nullable ImageDto newImage) {
        currentImage = newImage;
        imageFromUpload = false;
        updatePreview();
    }

    public void setRequired(final boolean required) {
        setRequiredIndicatorVisible(required);
    }

    public boolean isRequired() {
        return isRequiredIndicatorVisible();
    }

}
