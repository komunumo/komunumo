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

import app.komunumo.data.db.Tables;
import app.komunumo.data.db.tables.records.ImageRecord;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.infra.persistence.jooq.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.tables.Community.COMMUNITY;
import static app.komunumo.data.db.tables.Event.EVENT;
import static app.komunumo.data.db.tables.Image.IMAGE;
import static app.komunumo.data.db.tables.User.USER;
import static org.jooq.impl.DSL.selectOne;

/**
 * <p>Handles persistence operations for images and related read projections.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating, loading,
 * counting, deleting, and orphan detection of image records.</p>
 */
@Service
final class ImageStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Unique ID generator used when storing new image records.</p>
     */
    private final @NotNull UniqueIdGenerator idGenerator;

    /**
     * <p>Creates a new image store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     * @param idGenerator the generator used to create new primary keys
     */
    ImageStore(final @NotNull DSLContext dsl,
               final @NotNull UniqueIdGenerator idGenerator) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
    }

    /**
     * <p>Stores or updates an image record.</p>
     *
     * @param image the image DTO to persist
     * @return the persisted image DTO
     */
    @NotNull ImageDto storeImage(final @NotNull ImageDto image) {
        final ImageRecord imageRecord = dsl
                .fetchOptional(Tables.IMAGE, Tables.IMAGE.ID.eq(image.id()))
                .orElse(dsl.newRecord(Tables.IMAGE));
        imageRecord.from(image);
        if (imageRecord.getId() == null) { // NOSONAR (false positive: ID may be null for new images)
            imageRecord.setId(idGenerator.getUniqueID(Tables.IMAGE));
        }
        imageRecord.store();
        return imageRecord.into(ImageDto.class);
    }

    /**
     * <p>Loads an image by ID.</p>
     *
     * @param id the image ID; may be {@code null}
     * @return an optional containing the image if found; otherwise empty
     */
    @NotNull Optional<ImageDto> getImage(final @Nullable UUID id) {
        return id == null ? Optional.empty() : dsl
                .selectFrom(IMAGE)
                .where(IMAGE.ID.eq(id))
                .fetchOptionalInto(ImageDto.class);
    }

    /**
     * <p>Loads all images.</p>
     *
     * @return all persisted image DTOs
     */
    @NotNull List<@NotNull ImageDto> getImages() {
        return dsl.selectFrom(IMAGE)
                .fetchInto(ImageDto.class);
    }

    /**
     * <p>Counts all persisted images.</p>
     *
     * @return the total number of images; never negative
     */
    int getImageCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(IMAGE)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Loads all orphaned images.</p>
     *
     * <p>An image is orphaned when it is not referenced by community, event, or user records.</p>
     *
     * @return all orphaned image DTOs
     */
    @NotNull List<@NotNull ImageDto> findOrphanedImages() {
        return dsl.selectFrom(IMAGE)
                .whereNotExists(
                        selectOne()
                                .from(COMMUNITY)
                                .where(COMMUNITY.IMAGE_ID.eq(IMAGE.ID))
                )
                .andNotExists(
                        selectOne()
                                .from(EVENT)
                                .where(EVENT.IMAGE_ID.eq(IMAGE.ID))
                )
                .andNotExists(
                        selectOne()
                                .from(USER)
                                .where(USER.IMAGE_ID.eq(IMAGE.ID))
                )
                .fetchInto(ImageDto.class);
    }

    /**
     * <p>Loads the IDs of all images.</p>
     *
     * @return all persisted image IDs
     */
    @NotNull List<@NotNull UUID> getAllImageIds() {
        return dsl.select(IMAGE.ID)
                .from(IMAGE)
                .stream()
                .map(r -> r.get(IMAGE.ID))
                .toList();
    }

    /**
     * <p>Loads all images.</p>
     *
     * @return all persisted image DTOs
     */
    @NotNull List<@NotNull ImageDto> getAllImages() {
        return dsl.selectFrom(IMAGE)
                .fetchInto(ImageDto.class);
    }

    /**
     * <p>Deletes an image record by its ID.</p>
     *
     * @param image the image whose ID is used for deletion
     * @return the number of deleted rows
     */
    int deleteImage(final @NotNull ImageDto image) {
        return dsl.delete(Tables.IMAGE)
                .where(Tables.IMAGE.ID.eq(image.id()))
                .execute();
    }
}
