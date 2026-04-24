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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandleOwnerContextTest {

    @Test
    void constructorStoresUserContext() {
        final var userId = UUID.randomUUID();

        final var result = new HandleOwnerContext(userId, null, "saved");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isEqualTo("saved");
    }

    @Test
    void constructorStoresCommunityContext() {
        final var communityId = UUID.randomUUID();

        final var result = new HandleOwnerContext(null, communityId, "saved");

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isEqualTo(communityId);
        assertThat(result.persistedHandle()).isEqualTo("saved");
    }

    @Test
    void constructorAllowsEmptyContext() {
        final var result = new HandleOwnerContext(null, null, null);

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isNull();
    }

    @Test
    void constructorThrowsIfUserIdAndCommunityIdAreSet() {
        assertThatThrownBy(() -> new HandleOwnerContext(UUID.randomUUID(), UUID.randomUUID(), "saved"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only one owner reference may be set.");
    }

    @Test
    void noneReturnsEmptyContext() {
        final var result = HandleOwnerContext.none();

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isNull();
    }

    @Test
    void forUserReturnsUserContext() {
        final var userId = UUID.randomUUID();

        final var result = HandleOwnerContext.forUser(userId, "saved");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isEqualTo("saved");
    }

    @Test
    void forUserAllowsNullUserIdAndHandle() {
        final var result = HandleOwnerContext.forUser(null, null);

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isNull();
    }

    @Test
    void forCommunityReturnsCommunityContext() {
        final var communityId = UUID.randomUUID();

        final var result = HandleOwnerContext.forCommunity(communityId, "saved");

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isEqualTo(communityId);
        assertThat(result.persistedHandle()).isEqualTo("saved");
    }

    @Test
    void forCommunityAllowsNullCommunityIdAndHandle() {
        final var result = HandleOwnerContext.forCommunity(null, null);

        assertThat(result.userId()).isNull();
        assertThat(result.communityId()).isNull();
        assertThat(result.persistedHandle()).isNull();
    }
}
