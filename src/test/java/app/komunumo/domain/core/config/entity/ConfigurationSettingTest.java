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
package app.komunumo.domain.core.config.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationSettingTest {

    @Test
    void fromString_success() {
        assertThat(ConfigurationSetting.fromString("instance.name"))
                .isEqualTo(ConfigurationSetting.INSTANCE_NAME);
    }

    @Test
    void fromString_invalid() {
        assertThatThrownBy(() -> ConfigurationSetting.fromString("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown setting: invalid");
    }

    @Test
    void fromString_null() {
        assertThatThrownBy(() -> ConfigurationSetting.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown setting: null");
    }

    @Test
    void isLanguageDependent_false() {
        assertThat(ConfigurationSetting.INSTANCE_NAME.isLanguageDependent()).isFalse();
    }

    @Test
    void isLanguageDependent_true() {
        assertThat(ConfigurationSetting.INSTANCE_SLOGAN.isLanguageDependent()).isTrue();
    }

    @Test
    void fromString_privacyUrl() {
        assertThat(ConfigurationSetting.fromString("instance.privacyUrl"))
                .isEqualTo(ConfigurationSetting.INSTANCE_PRIVACY_URL);
    }

    @Test
    void fromString_imprintUrl() {
        assertThat(ConfigurationSetting.fromString("instance.imprintUrl"))
                .isEqualTo(ConfigurationSetting.INSTANCE_IMPRINT_URL);
    }

    @Test
    void privacyUrl_notLanguageDependent() {
        assertThat(ConfigurationSetting.INSTANCE_PRIVACY_URL.isLanguageDependent()).isFalse();
    }

    @Test
    void imprintUrl_notLanguageDependent() {
        assertThat(ConfigurationSetting.INSTANCE_IMPRINT_URL.isLanguageDependent()).isFalse();
    }

}
