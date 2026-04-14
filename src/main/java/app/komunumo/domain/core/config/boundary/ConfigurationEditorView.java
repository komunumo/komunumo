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
package app.komunumo.domain.core.config.boundary;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;

@RolesAllowed("ADMIN")
@Route(value = "admin/config", layout = WebsiteLayout.class)
public final class ConfigurationEditorView extends AbstractView implements AfterNavigationObserver {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull TranslationProvider translationProvider;

    public ConfigurationEditorView(final @NotNull ConfigurationService configurationService,
                                   final @NotNull TranslationProvider translationProvider) {
        super(configurationService);
        this.configurationService = configurationService;
        this.translationProvider = translationProvider;
        addClassName("configuration-editor-view");
        add(new H2(getTranslation("core.config.boundary.ConfigurationEditorView.title")));
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("core.config.boundary.ConfigurationEditorView.title");
    }

    @Override
    public void afterNavigation(final @NotNull AfterNavigationEvent afterNavigationEvent) {
        for (final var setting : ConfigurationSetting.values()) {
            add(new ConfigurationEditorComponent(configurationService, setting, translationProvider));
        }
    }

}
