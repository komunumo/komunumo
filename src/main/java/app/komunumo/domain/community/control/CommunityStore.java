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
package app.komunumo.domain.community.control;

import app.komunumo.data.db.Tables;
import app.komunumo.data.db.tables.records.CommunityRecord;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.community.entity.CommunityWithImageDto;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.persistence.jooq.AbstractStore;
import app.komunumo.infra.persistence.jooq.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.Tables.MEMBER;
import static app.komunumo.data.db.tables.ActorHandle.ACTOR_HANDLE;
import static app.komunumo.data.db.tables.Community.COMMUNITY;
import static app.komunumo.data.db.tables.Image.IMAGE;
import static app.komunumo.domain.member.entity.MemberRole.ORGANIZER;
import static app.komunumo.domain.member.entity.MemberRole.OWNER;

/**
 * <p>Handles persistence operations for communities and related read projections.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating, loading,
 * counting, and deleting communities.</p>
 */
@Service
final class CommunityStore extends AbstractStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new community store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     * @param idGenerator the unique ID generator used by {@link AbstractStore}
     */
    CommunityStore(final @NotNull DSLContext dsl,
                          final @NotNull UniqueIdGenerator idGenerator) {
        super(idGenerator);
        this.dsl = dsl;
    }

    /**
     * <p>Stores a community in the database.</p>
     *
     * <p>If a community with the same ID already exists, it is updated.
     * Otherwise, a new record is created.</p>
     *
     * @param community a DTO representation of the community information
     * @return the persisted community information in DTO form
     */
    public @NotNull CommunityDto storeCommunity(final @NotNull CommunityDto community) {
        final CommunityRecord communityRecord = dsl.fetchOptional(COMMUNITY, COMMUNITY.ID.eq(community.id()))
                .orElse(dsl.newRecord(COMMUNITY));
        createOrUpdate(COMMUNITY, community, communityRecord);
        return getCommunity(communityRecord.getId()).orElseThrow();
    }

    /**
     * <p>Loads a community by its unique identifier.</p>
     *
     * @param id the community ID
     * @return an optional containing the community if found; otherwise empty
     */
    public @NotNull Optional<CommunityDto> getCommunity(final @NotNull UUID id) {
        return dsl.select(COMMUNITY.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(COMMUNITY)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.COMMUNITY_ID.eq(COMMUNITY.ID))
                .where(COMMUNITY.ID.eq(id))
                .fetchOptional(this::toCommunityDto);
    }

    /**
     * <p>Loads a community by profile and includes optional image data.</p>
     *
     * @param profile the community profile slug/name
     * @return an optional containing the community and image projection if found; otherwise empty
     */
    public @NotNull Optional<CommunityWithImageDto> getCommunityWithImage(final @NotNull String profile) {
        return dsl.select()
                .from(Tables.COMMUNITY)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.COMMUNITY_ID.eq(COMMUNITY.ID))
                .leftJoin(IMAGE).on(Tables.COMMUNITY.IMAGE_ID.eq(IMAGE.ID))
                .where(COMMUNITY.PROFILE.eq(profile))
                .fetchOptional()
                .map(rec -> new CommunityWithImageDto(
                        toCommunityDto(rec),
                        rec.get(IMAGE.ID) != null ? rec.into(IMAGE).into(ImageDto.class) : null
                ));
    }

    /**
     * <p>Loads all communities ordered by name.</p>
     *
     * @return all communities
     */
    public @NotNull List<@NotNull CommunityDto> getCommunities() {
        return dsl.select(COMMUNITY.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(COMMUNITY)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.COMMUNITY_ID.eq(COMMUNITY.ID))
                .orderBy(COMMUNITY.NAME)
                .fetch(this::toCommunityDto);
    }

    /**
     * <p>Loads all communities with optional image data ordered by name.</p>
     *
     * @return all communities including optional image projection
     */
    public @NotNull List<@NotNull CommunityWithImageDto> getCommunitiesWithImage() {
        return dsl.select()
                .from(COMMUNITY)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.COMMUNITY_ID.eq(COMMUNITY.ID))
                .leftJoin(IMAGE).on(COMMUNITY.IMAGE_ID.eq(IMAGE.ID))
                .orderBy(COMMUNITY.NAME.asc())
                .fetch(rec -> new CommunityWithImageDto(
                        toCommunityDto(rec),
                        rec.get(IMAGE.ID) != null ? rec.into(IMAGE).into(ImageDto.class) : null
                ));
    }

    /**
     * <p>Loads communities where the given user has manager permissions.</p>
     *
     * <p>The user is considered manager if the member role is {@code OWNER}
     * or {@code ORGANIZER}.</p>
     *
     * @param user the user whose manageable communities should be loaded
     * @return all communities the user can manage
     */
    public @NotNull List<@NotNull CommunityDto> getCommunitiesForManager(final @NotNull UserDto user) {
        return dsl.select(COMMUNITY.fields())
                .select(ACTOR_HANDLE.HANDLE)
                .from(COMMUNITY)
                .leftJoin(ACTOR_HANDLE).on(ACTOR_HANDLE.COMMUNITY_ID.eq(COMMUNITY.ID))
                .join(MEMBER).on(MEMBER.COMMUNITY_ID.eq(COMMUNITY.ID))
                .where(MEMBER.USER_ID.eq(user.id())
                        .and(MEMBER.ROLE.in(OWNER.name(), ORGANIZER.name())))
                .orderBy(COMMUNITY.NAME)
                .fetch(this::toCommunityDto);
    }

    /**
     * <p>Counts the total number of communities.</p>
     *
     * @return the total count of communities; never negative
     */
    public int getCommunityCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(COMMUNITY)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Counts communities with the given profile name.</p>
     *
     * @param profile the profile name to filter by
     * @return the number of matching communities; never negative
     */
    public int getCommunityCount(final @NotNull String profile) {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(COMMUNITY)
                        .where(COMMUNITY.PROFILE.eq(profile))
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Deletes the given community and its related member records.</p>
     *
     * @param community the community to delete
     * @return the number of deleted community rows (typically {@code 0} or {@code 1})
     */
    public int deleteCommunity(final @NotNull CommunityDto community) {
        dsl.delete(MEMBER)
                .where(MEMBER.COMMUNITY_ID.eq(community.id()))
                .execute();

        return dsl.delete(COMMUNITY)
                .where(COMMUNITY.ID.eq(community.id()))
                .execute();
    }

    private @NotNull CommunityDto toCommunityDto(final @NotNull Record record) {
        final var community = record.into(COMMUNITY).into(CommunityDto.class);
        final var handle = Optional.ofNullable(record.get(ACTOR_HANDLE.HANDLE)).orElse(community.profile());
        return new CommunityDto(
                community.id(),
                community.profile(),
                handle,
                community.created(),
                community.updated(),
                community.name(),
                community.description(),
                community.imageId()
        );
    }
}
