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
package app.komunumo.domain.core.activitypub.entity;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * <p>Describes the optional owner context for a handle input field.</p>
 *
 * <p>The context may reference either a user or a community, together with the
 * currently persisted handle value for that owner. It is used to distinguish
 * unchanged persisted values from new user input and to allow reuse of an
 * existing handle by its current owner.</p>
 *
 * @param userId the user ID owning the handle; may be {@code null}
 * @param communityId the community ID owning the handle; may be {@code null}
 * @param persistedHandle the currently persisted handle of the owner; may be {@code null}
 */
public record HandleOwnerContext(
        @Nullable UUID userId,
        @Nullable UUID communityId,
        @Nullable String persistedHandle
) {

    /**
     * <p>Creates a new owner context.</p>
     *
     * <p>At most one owner reference may be set. The context may also contain
     * neither reference, which represents a new unsaved owner.</p>
     *
     * @throws IllegalArgumentException if both {@code userId} and {@code communityId} are set
     */
    public HandleOwnerContext {
        if (userId != null && communityId != null) {
            throw new IllegalArgumentException("Only one owner reference may be set.");
        }
    }

    /**
     * <p>Creates an empty owner context without persisted owner information.</p>
     *
     * @return a context representing no existing owner
     */
    public static HandleOwnerContext none() {
        return new HandleOwnerContext(null, null, null);
    }

    /**
     * <p>Creates an owner context for a user.</p>
     *
     * @param userId the user ID owning the handle; may be {@code null}
     * @param persistedHandle the currently persisted handle of the user; may be {@code null}
     * @return a context for the given user
     */
    public static HandleOwnerContext forUser(final @Nullable UUID userId,
                                             final @Nullable String persistedHandle) {
        return new HandleOwnerContext(userId, null, persistedHandle);
    }

    /**
     * <p>Creates an owner context for a community.</p>
     *
     * @param communityId the community ID owning the handle; may be {@code null}
     * @param persistedHandle the currently persisted handle of the community; may be {@code null}
     * @return a context for the given community
     */
    public static HandleOwnerContext forCommunity(final @Nullable UUID communityId,
                                                  final @Nullable String persistedHandle) {
        return new HandleOwnerContext(null, communityId, persistedHandle);
    }
}
