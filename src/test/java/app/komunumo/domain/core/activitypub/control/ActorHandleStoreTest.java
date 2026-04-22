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
package app.komunumo.domain.core.activitypub.control;

import app.komunumo.data.db.tables.records.ActorHandleRecord;
import app.komunumo.domain.core.activitypub.entity.ActorHandleDto;
import org.junit.jupiter.api.Test;
import org.jooq.DSLContext;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ActorHandleStoreTest {

    @Test
    void fetchByActorReferenceReturnsEmptyWhenAllIDsAreNull() throws Exception {
        final var dsl = mock(DSLContext.class);
        final var actorHandleStore = new ActorHandleStore(dsl);
        final var actorHandle = new ActorHandleDto("testHandle", null, null);

        final Method method = ActorHandleStore.class.getDeclaredMethod(
                "fetchByActorReference", ActorHandleDto.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        final var result = (Optional<ActorHandleRecord>) method.invoke(actorHandleStore, actorHandle);

        assertThat(result).isEmpty();
    }
}

