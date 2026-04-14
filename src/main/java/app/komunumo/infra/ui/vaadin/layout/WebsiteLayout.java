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

import app.komunumo.SecurityConfig;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.user.boundary.EditProfileView;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.AuthenticationState;
import app.komunumo.infra.config.AppConfig;
import app.komunumo.infra.ui.vaadin.components.InfoBanner;
import app.komunumo.infra.ui.vaadin.control.ThemeUtil;
import app.komunumo.util.TimeZoneUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_SLOGAN;

@AnonymousAllowed
public final class WebsiteLayout extends Div implements RouterLayout, BeforeEnterObserver {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(WebsiteLayout.class);

    private final @NotNull Main main;

    private final @NotNull UserService userService;
    private final @NotNull LoginService loginService;

    public WebsiteLayout(final @NotNull AppConfig appConfig,
                         final @NotNull ConfigurationService configurationService,
                         final @NotNull GlobalPageService globalPageService,
                         final @NotNull UserService userService,
                         final @NotNull LoginService loginService,
                         final @NotNull AuthenticationState authenticationState) {
        super();
        this.userService = userService;
        this.loginService = loginService;
        authenticationState.refreshFromSecurityContext();
        final var ui = UI.getCurrent();

        if (appConfig.demo().enabled()) {
            add(new InfoBanner(ui.getTranslation("core.layout.boundary.WebsiteLayout.demoMode")));
        }

        addPageHeader(configurationService);
        add(new NavigationBar(configurationService, globalPageService, authenticationState));

        main = new Main();
        add(main);

        final var komunumoVersion = appConfig.version();
        add(new PageFooter(ui, komunumoVersion));
    }

    private void addPageHeader(final @NotNull ConfigurationService configurationService) {
        final var locale = UI.getCurrent().getLocale();
        final var instanceTitle = configurationService.getConfiguration(INSTANCE_NAME);
        final var instanceSlogan = configurationService.getConfiguration(INSTANCE_SLOGAN, locale);
        add(new PageHeader(instanceTitle, instanceSlogan));
    }

    @Override
    public void showRouterLayoutContent(final @NotNull HasElement content) {
        main.removeAll();
        main.add(content.getElement().getComponent()
                .orElseThrow(() -> new IllegalArgumentException(
                        "WebsiteLayout content must be a Component")));
    }

    @Override
    public void removeRouterLayoutContent(final @Nullable HasElement oldContent) {
        main.removeAll();
    }

    @Override
    public void beforeEnter(final @NotNull BeforeEnterEvent event) {
        // configure utility classes based on browser settings
        final var ui = event.getUI();
        TimeZoneUtil.detectClientTimeZone(ui);
        ThemeUtil.initializeDarkMode(ui);

        // redirect users with incomplete profiles (logout is always allowed)
        loginService.getLoggedInUser().ifPresent(user -> {
            final var path = event.getLocation().getPath();
            if (path.equals("settings/profile") || path.equals(SecurityConfig.LOGOUT_URL)) {
                return;
            }
            if (!userService.isProfileComplete(user)) {
                LOGGER.info("Redirecting user '{}' with incomplete profile to edit profile view.", user.id());
                event.forwardTo(EditProfileView.class);
            }
        });
    }

}
