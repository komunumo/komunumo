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

import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides image-related business operations and delegates persistence to {@link ImageStore}.</p>
 *
 * <p>This service orchestrates image lifecycle use cases such as orphan cleanup and file deletion,
 * while database access is encapsulated in the store implementation.</p>
 */
@Service
public final class ImageService {

    /**
     * <p>Logger used for cleanup progress and file deletion errors.</p>
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    /**
     * <p>Store responsible for image persistence and read operations.</p>
     */
    private final @NotNull ImageStore imageStore;

    /**
     * <p>Creates a new image service.</p>
     *
     * @param imageStore the store used for image persistence access
     */
    ImageService(final @NotNull ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    /**
     * <p>Stores or updates an image record.</p>
     *
     * @param image the image DTO to persist
     * @return the persisted image DTO
     */
    public @NotNull ImageDto storeImage(final @NotNull ImageDto image) {
        return imageStore.storeImage(image);
    }

    /**
     * <p>Loads an image by ID.</p>
     *
     * @param id the image ID; may be {@code null}
     * @return an optional containing the image if found; otherwise empty
     */
    public @NotNull Optional<ImageDto> getImage(final @Nullable UUID id) {
        return imageStore.getImage(id);
    }

    /**
     * <p>Loads all images.</p>
     *
     * @return all persisted images
     */
    public @NotNull List<@NotNull ImageDto> getImages() {
        return imageStore.getImages();
    }

    /**
     * <p>Counts all persisted images.</p>
     *
     * @return the total number of images; never negative
     */
    public int getImageCount() {
        return imageStore.getImageCount();
    }

    /**
     * <p>Loads all orphaned images.</p>
     *
     * <p>An image is orphaned when it is not referenced by community, event, or user records.</p>
     *
     * @return all orphaned image DTOs
     */
    public @NotNull List<@NotNull ImageDto> findOrphanedImages() {
        return imageStore.findOrphanedImages();
    }

    /**
     * <p>Performs periodic cleanup of orphaned images.</p>
     *
     * <p>This cleanup removes orphaned image records and then removes orphaned files from disk.</p>
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOrphanedImages() {
        LOGGER.info("Cleaning up orphaned images...");
        findOrphanedImages().forEach(this::deleteImage);
        ImageUtil.cleanupOrphanedImageFiles(this);
        LOGGER.info("Orphaned images cleaned.");
    }

    /**
     * <p>Loads the IDs of all images.</p>
     *
     * @return all persisted image IDs
     */
    public @NotNull List<@NotNull UUID> getAllImageIds() {
        return imageStore.getAllImageIds();
    }

    /**
     * <p>Loads all images.</p>
     *
     * <p>This method is functionally equivalent to {@link #getImages()} and is used by export workflows.</p>
     *
     * @return all persisted image DTOs
     */
    public @NotNull List<@NotNull ImageDto> getAllImages() {
        return imageStore.getAllImages();
    }

    /**
     * <p>Deletes an image record and its file if present.</p>
     *
     * <p>The file deletion is attempted first. If file deletion fails, the error is logged and
     * database deletion is still attempted.</p>
     *
     * @param image the image to delete
     * @return {@code true} if a database record was deleted; otherwise {@code false}
     */
    public boolean deleteImage(final @NotNull ImageDto image) {
        final var path = ImageUtil.resolveImagePath(image);
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (final IOException e) {
                LOGGER.error("Failed to delete image file: {}", path.toAbsolutePath(), e);
            }
        }

        return imageStore.deleteImage(image) > 0;
    }

}
