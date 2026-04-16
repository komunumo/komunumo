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
package app.komunumo.infra.persistence.jooq;

import org.jetbrains.annotations.NotNull;
import org.jooq.impl.TableImpl;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * <p>Provides generic persistence functionality for database records that
 * include timestamp fields such as {@code created} and {@code updated}.</p>
 *
 * <p>The {@link AbstractStore} handles both insert and update operations:
 * it determines whether a record is new or existing, assigns a unique ID
 * if necessary, and updates the relevant timestamps accordingly before
 * storing the record through jOOQ.</p>
 *
 * <p>This service ensures a consistent handling of record metadata across
 * all entities managed by Komunumo.</p>
 */
public abstract class AbstractStore {

    /**
     * <p>The generator used to create unique identifiers for new records.</p>
     */
    private final @NotNull UniqueIdGenerator idGenerator;

    /**
     * <p>Creates a new {@link AbstractStore} instance.</p>
     *
     * @param idGenerator the unique ID generator used for assigning identifiers
     *                    to newly created records
     */
    protected AbstractStore(final @NotNull UniqueIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * <p>Inserts or updates a database record depending on whether it already
     * exists.</p>
     *
     * <p>The method copies all field values from the given DTO into the corresponding
     * database record, assigns a unique ID if the record is new, and updates
     * the {@code created} and {@code updated} timestamps in UTC. Finally, the record
     * is stored via jOOQ’s {@link org.jooq.UpdatableRecord#store()} method. </p>
     *
     * @param table  the jOOQ table definition associated with the record
     * @param dto    the data transfer object containing updated field values
     * @param record the jOOQ record representing the database row to be created or updated
     */
    protected void createOrUpdate(final @NotNull TableImpl<? extends RecordWithTimestamps> table,
                                  final @NotNull Record dto,
                                  final @NotNull RecordWithTimestamps record) {
        record.from(dto);

        if (record.getId() == null) { // NOSONAR (false positive: ID may be null for new records)
            record.setId(idGenerator.getUniqueID(table));
        }

        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        if (record.getCreated() == null) { // NOSONAR (false positive: date may be null for new records)
            record.setCreated(now);
            record.setUpdated(now);
        } else {
            record.setUpdated(now);
        }
        record.store();
    }

}
