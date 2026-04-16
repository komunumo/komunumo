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
package app.komunumo.domain.member.control;

import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.user.entity.UserDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.komunumo.data.db.Tables.MEMBER;

/**
 * <p>Handles persistence operations for community memberships.</p>
 *
 * <p>This store encapsulates all jOOQ database access for creating, updating,
 * loading, counting, and deleting member relations.</p>
 */
@Service
final class MemberStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new member store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     */
    MemberStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Checks whether the given user is a member of the specified community.</p>
     *
     * @param user the user to check for membership
     * @param community the community in which membership is verified
     * @return {@code true} if the user is a member; otherwise {@code false}
     */
    public boolean isMember(final @NotNull UserDto user, final @NotNull CommunityDto community) {
        return dsl.fetchExists(dsl.selectFrom(MEMBER)
                .where(MEMBER.USER_ID.eq(user.id())
                        .and(MEMBER.COMMUNITY_ID.eq(community.id()))));
    }

    /**
     * <p>Stores or updates a member relation.</p>
     *
     * @param memberDto a DTO representation of the member relation
     * @return the persisted member relation
     */
    public @NotNull MemberDto storeMember(final @NotNull MemberDto memberDto) {
        final var memberRecord = dsl.fetchOptional(MEMBER,
                        MEMBER.USER_ID.eq(memberDto.userId())
                                .and(MEMBER.COMMUNITY_ID.eq(memberDto.communityId())))
                .orElse(dsl.newRecord(MEMBER));

        memberRecord.setUserId(memberDto.userId());
        memberRecord.setCommunityId(memberDto.communityId());
        memberRecord.setRole(memberDto.role().name());

        if (memberRecord.getSince() == null && memberDto.since() != null) {
            memberRecord.setSince(memberDto.since());
        } else if (memberRecord.getSince() == null) {
            memberRecord.setSince(ZonedDateTime.now(ZoneOffset.UTC));
        }

        memberRecord.store();

        return memberRecord.into(MemberDto.class);
    }

    /**
     * <p>Loads all members.</p>
     *
     * @return all members
     */
    public @NotNull List<@NotNull MemberDto> getMembers() {
        return dsl.selectFrom(MEMBER)
                .fetchInto(MemberDto.class);
    }

    /**
     * <p>Loads one member relation by user and community.</p>
     *
     * @param user the user to lookup
     * @param community the community to lookup
     * @return an optional containing the relation if found; otherwise empty
     */
    public @NotNull Optional<MemberDto> getMember(final @NotNull UserDto user,
                                                  final @NotNull CommunityDto community) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.USER_ID.eq(user.id())
                        .and(MEMBER.COMMUNITY_ID.eq(community.id())))
                .fetchOptionalInto(MemberDto.class);
    }

    /**
     * <p>Loads all members of a community.</p>
     *
     * @param communityId the community ID
     * @return all members of the given community
     */
    public @NotNull List<@NotNull MemberDto> getMembersByCommunityId(final @NotNull UUID communityId) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.COMMUNITY_ID.eq(communityId))
                .fetchInto(MemberDto.class);
    }

    /**
     * <p>Loads all members of a community with a specific role.</p>
     *
     * @param communityId the community ID
     * @param role the role to filter by
     * @return all matching members ordered by membership date descending
     */
    public @NotNull List<@NotNull MemberDto> getMembersByCommunityId(final @NotNull UUID communityId,
                                                                     final @NotNull MemberRole role) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.COMMUNITY_ID.eq(communityId)
                        .and(MEMBER.ROLE.eq(role.name())))
                .orderBy(MEMBER.SINCE.desc())
                .fetchInto(MemberDto.class);
    }

    /**
     * <p>Counts all members across all communities.</p>
     *
     * @return the total count of members; never negative
     */
    public int getMemberCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(MEMBER)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Counts members in a specific community.</p>
     *
     * @param communityId the community ID to filter by
     * @return the number of members in the given community; never negative
     */
    public int getMemberCount(final @Nullable UUID communityId) {
        return dsl.fetchCount(MEMBER, MEMBER.COMMUNITY_ID.eq(communityId));
    }

    /**
     * <p>Deletes a member relation identified by user ID and community ID.</p>
     *
     * <p>The operation targets exactly the relation represented by the given member DTO.
     * The returned value is the number of deleted database rows.</p>
     *
     * @param member the member relation to delete
     * @return the number of deleted rows; usually {@code 0} or {@code 1}
     */
    public int deleteMember(final @NotNull MemberDto member) {
        return dsl.delete(MEMBER)
                .where(MEMBER.USER_ID.eq(member.userId())
                        .and(MEMBER.COMMUNITY_ID.eq(member.communityId())))
                .execute();
    }
}
