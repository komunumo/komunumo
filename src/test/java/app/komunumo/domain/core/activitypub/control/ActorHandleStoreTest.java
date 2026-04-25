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
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.DeleteUsingStep;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static app.komunumo.data.db.tables.ActorHandle.ACTOR_HANDLE;

class ActorHandleStoreTest {

    @Test
    void storeActorHandleReturnsExistingRecordWhenHandleIsUnchanged() {
        final var dsl = mock(DSLContext.class);
        final var actorHandleStore = new ActorHandleStore(dsl);
        final var userId = UUID.randomUUID();
        final var actorHandle = new ActorHandleDto("testHandle", userId, null);
        final var existingRecord = mock(ActorHandleRecord.class);

        when(dsl.fetchOptional(ACTOR_HANDLE, ACTOR_HANDLE.USER_ID.eq(userId))).thenReturn(Optional.of(existingRecord));
        when(existingRecord.getHandle()).thenReturn("testHandle");
        when(existingRecord.into(ActorHandleDto.class)).thenReturn(actorHandle);

        final var result = actorHandleStore.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(dsl, never()).newRecord(ACTOR_HANDLE);
        verify(existingRecord, never()).store();
    }

    @Test
    void storeActorHandleReplacesExistingRecordWhenHandleHasChanged() {
        final var dsl = mock(DSLContext.class);
        final var actorHandleStore = new ActorHandleStore(dsl);
        final var userId = UUID.randomUUID();
        final var actorHandle = new ActorHandleDto("newHandle", userId, null);
        final var existingRecord = mock(ActorHandleRecord.class);
        final var newRecord = mock(ActorHandleRecord.class);
        @SuppressWarnings("unchecked")
        final var deleteUsingStep = (DeleteUsingStep<ActorHandleRecord>) mock(DeleteUsingStep.class);
        @SuppressWarnings("unchecked")
        final var deleteStep = (DeleteConditionStep<ActorHandleRecord>) mock(DeleteConditionStep.class);

        when(dsl.fetchOptional(ACTOR_HANDLE, ACTOR_HANDLE.USER_ID.eq(userId))).thenReturn(Optional.of(existingRecord));
        when(existingRecord.getHandle()).thenReturn("oldHandle");
        when(dsl.delete(ACTOR_HANDLE)).thenReturn(deleteUsingStep);
        when(deleteUsingStep.where(ACTOR_HANDLE.USER_ID.eq(userId))).thenReturn(deleteStep);
        when(deleteStep.execute()).thenReturn(1);
        when(dsl.newRecord(ACTOR_HANDLE)).thenReturn(newRecord);
        when(newRecord.into(ActorHandleDto.class)).thenReturn(actorHandle);

        final var result = actorHandleStore.storeActorHandle(actorHandle);

        assertThat(result).isEqualTo(actorHandle);
        verify(deleteStep).execute();
        verify(dsl).newRecord(ACTOR_HANDLE);
        verify(newRecord).from(actorHandle);
        verify(newRecord).store();
    }

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

    @Test
    void storeActorHandleThrowsWhenNoReferenceIsSet() {
        final var dsl = mock(DSLContext.class);
        final var actorHandleStore = new ActorHandleStore(dsl);
        final var actorHandle = new ActorHandleDto("testHandle", null, null);

        assertThatThrownBy(() -> actorHandleStore.storeActorHandle(actorHandle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("storeActorHandle requires a user or community reference.");
    }
}
