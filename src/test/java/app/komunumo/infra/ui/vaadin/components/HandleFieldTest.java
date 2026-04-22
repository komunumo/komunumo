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
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandleFieldTest {

    private HandleField handleField;

    @BeforeEach
    void setUp() {
        final var configurationService = mock(ConfigurationService.class);
        final var actorHandleService = mock(ActorHandleService.class);
        when(configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN)).thenReturn("example.com");
        handleField = new HandleField(configurationService, actorHandleService);
    }

    @Test
    void setLabel() {
        assertThat(handleField.getLabel()).isNull();
        final var label = "Test Label";
        handleField.setLabel(label);
        assertThat(handleField.getLabel()).isEqualTo(label);
    }

    @Test
    void setReadOnly() {
        assertThat(handleField.isReadOnly()).isFalse();
        handleField.setReadOnly(true);
        assertThat(handleField.isReadOnly()).isTrue();
        handleField.setReadOnly(false);
        assertThat(handleField.isReadOnly()).isFalse();
    }

    @Test
    void setRequired() {
        assertThat(handleField.isRequired()).isFalse();
        handleField.setRequired(true);
        assertThat(handleField.isRequired()).isTrue();
        handleField.setRequired(false);
        assertThat(handleField.isRequired()).isFalse();
    }

    @Test
    void setRequiredIndicatorVisible() {
        assertThat(handleField.isRequiredIndicatorVisible()).isFalse();
        handleField.setRequiredIndicatorVisible(true);
        assertThat(handleField.isRequiredIndicatorVisible()).isTrue();
        handleField.setRequiredIndicatorVisible(false);
        assertThat(handleField.isRequiredIndicatorVisible()).isFalse();
    }

    @Test
    void setValueChangeMode() {
        assertThat(handleField.getValueChangeMode()).isEqualTo(ValueChangeMode.EAGER);
        handleField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        assertThat(handleField.getValueChangeMode()).isEqualTo(ValueChangeMode.ON_CHANGE);
        handleField.setValueChangeMode(ValueChangeMode.ON_BLUR);
        assertThat(handleField.getValueChangeMode()).isEqualTo(ValueChangeMode.ON_BLUR);
    }

    @Test
    void getEmptyValue() {
        assertThat(handleField.getEmptyValue()).isEqualTo("");
    }

    @Test
    void generateModelValue() {
        assertThat(handleField.generateModelValue()).isEqualTo("");
        handleField.setValue("testValue");
        assertThat(handleField.generateModelValue()).isEqualTo("testValue");
        assertThat(handleField.getValue()).isEqualTo("testValue");
    }
}
