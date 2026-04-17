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
package app.komunumo.infra.ui.vaadin.layout;

import app.komunumo.domain.community.boundary.CommunityGridView;
import app.komunumo.domain.community.boundary.CreateCommunityView;
import app.komunumo.domain.core.config.boundary.ConfigurationEditorView;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.exporter.boundary.ExporterView;
import app.komunumo.domain.core.image.boundary.OrphanedImagesView;
import app.komunumo.domain.core.importer.boundary.ImporterView;
import app.komunumo.domain.event.boundary.CreateEventView;
import app.komunumo.domain.event.boundary.EventsView;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.user.boundary.EditProfileView;
import app.komunumo.domain.user.boundary.LoginView;
import app.komunumo.domain.user.boundary.LogoutView;
import app.komunumo.domain.user.boundary.UserRegistrationView;
import app.komunumo.domain.user.entity.AuthenticationState;
import app.komunumo.infra.ui.vaadin.control.ThemeUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.Signal;
import org.jetbrains.annotations.NotNull;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_CREATE_COMMUNITY_ALLOWED;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_HIDE_COMMUNITIES;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_REGISTRATION_ALLOWED;

public final class NavigationBar extends HorizontalLayout {

    public NavigationBar(final @NotNull ConfigurationService configurationService,
                         final @NotNull GlobalPageService globalPageService,
                         final @NotNull AuthenticationState authenticationState) {
        super();
        final var ui = UI.getCurrent();
        addClassName("navigation-bar");

        final var menuContainer = new Div();
        menuContainer.addClassName("menu-container");
        menuContainer.add(getNavigationBar(ui, configurationService, globalPageService));
        addToStart(menuContainer);

        addToEnd(getAvatar(ui, configurationService, authenticationState));
    }

    private Component getNavigationBar(final @NotNull UI ui,
                                       final @NotNull ConfigurationService configurationService,
                                       final @NotNull GlobalPageService globalPageService) {
        final var menuBar = new Nav();
        menuBar.addClassName("menu-bar");
        menuBar.add(new RouterLink(ui.getTranslation("vaadin.components.NavigationBar.events"), EventsView.class));

        if (!configurationService.getConfiguration(INSTANCE_HIDE_COMMUNITIES, Boolean.class)) {
            menuBar.add(new RouterLink(ui.getTranslation("vaadin.components.NavigationBar.communities"), CommunityGridView.class));
        }
        globalPageService
                .getGlobalPages(ui.getLocale())
                .forEach(page -> menuBar.add(new Anchor("/page/" + page.slot(), page.title())));
        return menuBar;
    }

    private Component getAvatar(final @NotNull UI ui,
                                final @NotNull ConfigurationService configurationService,
                                final @NotNull AuthenticationState authenticationState) {
        final var avatar = new Avatar();
        final var avatarMenu = new ContextMenu(avatar);
        avatarMenu.setOpenOnClick(true);

        // admin menu
        final var adminMenuItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.admin"));
        final var adminMenu = adminMenuItem.getSubMenu();
        adminMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.config"),
                _ -> ui.navigate(ConfigurationEditorView.class)
        );
        adminMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.orphanedImages"),
                _ -> ui.navigate(OrphanedImagesView.class)
        );
        adminMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.import"),
                _ -> ui.navigate(ImporterView.class)
        );
        adminMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.export"), _ ->
                ui.navigate(ExporterView.class)
        );

        // login as first entry in the menu
        final var loginItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.login"),
                _ -> ui.navigate(LoginView.class)
        );

        // register local account
        final var registerItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.register"),
                _ -> ui.navigate(UserRegistrationView.class)
        );

        // settings menu
        final var settingsMenuItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.settings"));
        final var settingsMenu = settingsMenuItem.getSubMenu();

        // edit profile
        settingsMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.editProfile"),
                _ -> ui.navigate(EditProfileView.class)
        );

        // create community
        final var createCommunityItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.createCommunity"),
                _ -> ui.navigate(CreateCommunityView.class));

        // create event
        final var createEventItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.createEvent"),
                _ -> ui.navigate(CreateEventView.class));

        // dark theme toggle
        avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.toggleDarkMode"),
                _ -> ThemeUtil.toggleDarkMode(ui));

        // logout as last entry in the menu
        final var logoutItem = avatarMenu.addItem(ui.getTranslation("vaadin.components.NavigationBar.logout"),
                _ -> ui.navigate(LogoutView.class)
        );

        final var registrationAllowed = configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class);
        final var createCommunityAllowed = configurationService.getConfiguration(INSTANCE_CREATE_COMMUNITY_ALLOWED, Boolean.class);

        loginItem.bindVisible(Signal.not(authenticationState.getAuthenticatedSignal()));
        logoutItem.bindVisible(authenticationState.getAuthenticatedSignal());

        registerItem.bindVisible(Signal.computed(() -> {
            final var authenticationSignal = authenticationState.getAuthenticatedSignal();
            final var isAuthenticated = authenticationSignal.get();
            return registrationAllowed && !isAuthenticated;
        }));

        adminMenuItem.bindVisible(authenticationState.getAdminSignal());
        settingsMenuItem.bindVisible(authenticationState.getLocalUserSignal());

        createCommunityItem.bindVisible(Signal.computed(() -> {
            final var localUserSignal = authenticationState.getLocalUserSignal();
            final var isLocalUser = localUserSignal.get();
            return isLocalUser && createCommunityAllowed;
        }));

        createEventItem.bindVisible(authenticationState.getLocalUserSignal());

        return avatar;
    }
}
