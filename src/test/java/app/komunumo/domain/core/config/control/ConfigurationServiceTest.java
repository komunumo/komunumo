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
package app.komunumo.domain.core.config.control;

import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_HIDE_COMMUNITIES;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_SLOGAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationServiceTest {

    @Test
    void getConfigurationCountDelegatesToStore() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationCount()).thenReturn(7);

        final var result = service.getConfigurationCount();

        assertThat(result).isEqualTo(7);
        verify(store).getConfigurationCount();
    }

    @Test
    void getConfigurationWithoutLocaleReturnsDefaultWhenNoValueExists() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_NAME, "")).thenReturn(Optional.empty());

        final var result = service.getConfiguration(INSTANCE_NAME);

        assertThat(result).isEqualTo("Your Instance Name");
    }

    @Test
    void getConfigurationWithLocaleFallsBackToNeutralValue() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "fr")).thenReturn(Optional.empty());
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "en")).thenReturn(Optional.empty());
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "")).thenReturn(Optional.of("Neutral Slogan"));

        final var result = service.getConfiguration(INSTANCE_SLOGAN, Locale.FRENCH);

        assertThat(result).isEqualTo("Neutral Slogan");
    }

    @Test
    void getConfigurationWithLocaleReturnsDefaultWhenAllFallbacksMissing() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "fr")).thenReturn(Optional.empty());
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "en")).thenReturn(Optional.empty());
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "")).thenReturn(Optional.empty());

        final var result = service.getConfiguration(INSTANCE_SLOGAN, Locale.FRENCH);

        assertThat(result).isEqualTo("Your Instance Slogan");
    }

    @Test
    void getConfigurationWithoutFallbackReadsOnlyRequestedLocale() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_SLOGAN, "fr")).thenReturn(Optional.empty());

        final var result = service.getConfigurationWithoutFallback(INSTANCE_SLOGAN, Locale.FRENCH);

        assertThat(result).isEqualTo("Your Instance Slogan");
        verify(store).getConfigurationValue(INSTANCE_SLOGAN, "fr");
        verify(store, never()).getConfigurationValue(INSTANCE_SLOGAN, "en");
    }

    @Test
    void getConfigurationWithBooleanTypeReturnsParsedBoolean() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_HIDE_COMMUNITIES, "")).thenReturn(Optional.of("true"));

        final var result = service.getConfiguration(INSTANCE_HIDE_COMMUNITIES, Boolean.class);

        assertThat(result).isTrue();
    }

    @Test
    void getConfigurationWithUnsupportedTypeThrowsException() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);

        assertThatThrownBy(() -> service.getConfiguration(INSTANCE_NAME, List.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported type: interface java.util.List");
    }

    @Test
    void setConfigurationShouldInvalidateCacheForSetting() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_NAME, ""))
                .thenReturn(Optional.of("Old"))
                .thenReturn(Optional.of("New"));

        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Old");

        service.setConfiguration(INSTANCE_NAME, "updated");

        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("New");
        verify(store).upsertConfigurationValue(INSTANCE_NAME, "", "updated");
    }

    @Test
    void clearCacheShouldForceReloadFromStore() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_NAME, ""))
                .thenReturn(Optional.of("Cached"))
                .thenReturn(Optional.of("Reloaded"));

        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Cached");
        service.clearCache();
        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Reloaded");
    }

    @Test
    void deleteConfigurationShouldInvalidateCacheForSetting() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_NAME, ""))
                .thenReturn(Optional.of("Configured"))
                .thenReturn(Optional.empty());
        when(store.deleteConfigurationValue(INSTANCE_NAME, "")).thenReturn(1);

        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Configured");

        final var deleted = service.deleteConfiguration(INSTANCE_NAME);

        assertThat(deleted).isTrue();
        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Your Instance Name");
        verify(store).deleteConfigurationValue(INSTANCE_NAME, "");
    }

    @Test
    void deleteConfigurationShouldReturnFalseWhenNoRowDeleted() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.deleteConfigurationValue(INSTANCE_NAME, "")).thenReturn(0);

        final var deleted = service.deleteConfiguration(INSTANCE_NAME);

        assertThat(deleted).isFalse();
    }

    @Test
    void deleteAllConfigurationsShouldClearCacheAndDelegateToStore() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.getConfigurationValue(INSTANCE_NAME, ""))
                .thenReturn(Optional.of("Configured"))
                .thenReturn(Optional.empty());
        when(store.deleteAllConfigurations()).thenReturn(1);

        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Configured");

        final var deleted = service.deleteAllConfigurations();

        assertThat(deleted).isTrue();
        assertThat(service.getConfiguration(INSTANCE_NAME)).isEqualTo("Your Instance Name");
        verify(store).deleteAllConfigurations();
    }

    @Test
    void deleteAllConfigurationsShouldReturnFalseWhenNoRowDeleted() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);
        when(store.deleteAllConfigurations()).thenReturn(0);

        final var deleted = service.deleteAllConfigurations();

        assertThat(deleted).isFalse();
    }

    @Test
    void getConfigurationShouldRejectMissingLocaleForLanguageDependentSetting() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);

        assertThatThrownBy(() -> service.getConfiguration(INSTANCE_SLOGAN, null, String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Setting 'instance.slogan' is language-dependent; you need to specify a locale!");
    }

    @Test
    void getConfigurationShouldRejectLocaleForLanguageIndependentSetting() {
        final var store = mock(ConfigurationStore.class);
        final var service = new ConfigurationService(store);

        assertThatThrownBy(() -> service.getConfiguration(ConfigurationSetting.INSTANCE_NAME, Locale.ENGLISH, String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Setting 'instance.name' is not language-dependent; do not specify a locale!");
    }

}
