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

import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.HandleOwnerContext;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.test.KaribuTest;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandleFieldKT extends KaribuTest {

    private HandleField handleField;

    @BeforeEach
    void setUp() {
        final var configurationService = mock(ConfigurationService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        final var actorHandleService = mock(ActorHandleService.class);
        when(actorHandleService.isHandleAvailable(anyString(), eq(HandleOwnerContext.none())))
                .thenAnswer(invocation -> {
                    final String handle = invocation.getArgument(0);
                    return !handle.contains("taken");
                });
        handleField = new HandleField(configurationService, actorHandleService, HandleOwnerContext.none());
    }

    @Test
    void handleIsAvailable() {
        handleField.setValue("available");
        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("This handle is available.");
    }

    @Test
    void handleIsNotAvailable() {
        handleField.setValue("taken");
        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("This handle is not available!");
    }

    @Test
    void ownHandleIsAvailable() {
        final var userId = UUID.randomUUID();
        final var configurationService = mock(ConfigurationService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        final var actorHandleService = mock(ActorHandleService.class);
        final var ownerContext = HandleOwnerContext.forUser(userId, "other");
        when(actorHandleService.isHandleAvailable(eq("taken"), eq(ownerContext))).thenReturn(true);
        handleField = new HandleField(configurationService, actorHandleService, ownerContext);

        handleField.setValue("taken");

        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("This handle is available.");
    }

    @Test
    void persistedHandleDoesNotShowAvailabilityMessage() {
        final var userId = UUID.randomUUID();
        final var configurationService = mock(ConfigurationService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        final var actorHandleService = mock(ActorHandleService.class);
        handleField = new HandleField(configurationService,
                actorHandleService,
                HandleOwnerContext.forUser(userId, "saved"));

        handleField.setValue("saved");

        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEmpty();
    }

    @Test
    void changedHandleIsNotTreatedAsPersistedValue() {
        final var userId = UUID.randomUUID();
        final var configurationService = mock(ConfigurationService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        final var actorHandleService = mock(ActorHandleService.class);
        final var ownerContext = HandleOwnerContext.forUser(userId, "saved");
        when(actorHandleService.isHandleAvailable(eq("changed"), eq(ownerContext))).thenReturn(true);
        handleField = new HandleField(configurationService, actorHandleService, ownerContext);

        handleField.setValue("changed");

        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("This handle is available.");
    }

    @Test
    void blankHandleIsNotTreatedAsPersistedValue() {
        final var userId = UUID.randomUUID();
        final var configurationService = mock(ConfigurationService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        final var actorHandleService = mock(ActorHandleService.class);
        final var ownerContext = HandleOwnerContext.forUser(userId, "saved");
        when(actorHandleService.isHandleAvailable(eq("   "), eq(ownerContext))).thenReturn(false);
        handleField = new HandleField(configurationService, actorHandleService, ownerContext);

        handleField.setValue("   ");

        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("This handle is not available!");
    }

    @Test
    void handleIsTooShort() {
        handleField.setValue("ab");
        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("The handle must be between 3 and 30 characters long!");
    }

    @Test
    void handleIsTooLong() {
        handleField.setValue("abcdefghijklmnopqrstuvwxyz1234567890");
        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("The handle must be between 3 and 30 characters long!");
    }

    @Test
    void handleHasInvalidSyntax() {
        handleField.setValue("@test");
        final var message = _get(handleField, Paragraph.class);
        assertThat(message.getText()).isEqualTo("The syntax of the handle is invalid: @test");
    }

    @Test
    void placeholder() {
        assertThat(handleField.getPlaceholder()).isNull();
        handleField.setPlaceholder("test");
        assertThat(handleField.getPlaceholder()).isEqualTo("test");
        handleField.setPlaceholder(null);
        assertThat(handleField.getPlaceholder()).isEmpty();
    }

}
