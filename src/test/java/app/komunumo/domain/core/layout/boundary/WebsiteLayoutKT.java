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
package app.komunumo.domain.core.layout.boundary;

import app.komunumo.domain.event.boundary.EventGridView;
import app.komunumo.domain.home.boundary.HomeView;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.ui.vaadin.components.InfoBanner;
import app.komunumo.infra.ui.vaadin.layout.NavigationBar;
import app.komunumo.infra.ui.vaadin.layout.PageFooter;
import app.komunumo.infra.ui.vaadin.layout.PageHeader;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.test.KaribuTest;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLink;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static app.komunumo.test.TestUtil.assertContainsExactlyOneInstanceOf;
import static app.komunumo.test.TestUtil.assertContainsExactlyOneRouterLinkOf;
import static app.komunumo.test.TestUtil.findComponent;
import static app.komunumo.test.TestUtil.findComponents;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebsiteLayoutKT extends KaribuTest {

    @Autowired
    private @NotNull UserService userService;

    private @NotNull WebsiteLayout websiteLayout;

    @BeforeEach
    void setUp() {
        UI.getCurrent().navigate(HomeView.class);
        final var uiParent = UI.getCurrent()
                .getCurrentView()
                .getParent().orElseThrow()
                .getParent().orElseThrow();
        websiteLayout = (WebsiteLayout) uiParent;
    }

    @Test
    void testLayoutContent() {
        final var components = websiteLayout.getChildren().toList();
        assertContainsExactlyOneInstanceOf(components,
                InfoBanner.class, PageHeader.class, NavigationBar.class, Main.class, PageFooter.class);
    }

    @Test
    void testBanner()  {
        final var infoBanner = findComponent(websiteLayout, InfoBanner.class);
        assertThat(infoBanner).isNotNull();

        final var markdown = findComponent(infoBanner, Markdown.class);
        assertThat(markdown).isNotNull();
        assertThat(markdown.getContent())
                .isEqualTo("**Demo Mode:** All data will be reset at the top of every hour!");
    }

    @Test
    void testPageHeader() {
        final var header = findComponent(websiteLayout, Header.class);
        assertThat(header).isNotNull();

        final var h1 = findComponent(header, H1.class);
        assertThat(h1).isNotNull();
        assertThat(h1.getText()).isEqualTo("Komunumo Test");

        final var h2 = findComponent(header, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("This is a local test instance of Komunumo.");
    }

    @Test
    void testPageFooter() {
        final var footer = findComponent(websiteLayout, Footer.class);
        assertThat(footer).isNotNull();

        final var anchor = findComponent(footer, Anchor.class);
        assertThat(anchor).isNotNull();
        assertThat(anchor.getText()).contains("Komunumo · Version");
    }

    @Test
    void testNavigationBar() {
        final var navigationBar = findComponent(websiteLayout, NavigationBar.class);
        assertThat(navigationBar).isNotNull();

        final var routerLinks = findComponents(navigationBar, RouterLink.class);
        assertContainsExactlyOneRouterLinkOf(routerLinks,
                new Anchor("events", "Events"),
                new Anchor("communities", "Communities"));
    }

    @Test
    void testRouterLayoutContent() {
        final var main = findComponent(websiteLayout, Main.class);
        assertThat(main).isNotNull();
        assertThat(main.getElement().getChildCount()).isEqualTo(1);

        websiteLayout.showRouterLayoutContent(new Paragraph("foo"));
        assertThat(main.getElement().getChildCount()).isEqualTo(1);
        assertThat(main.getElement().getChild(0).getText()).isEqualTo("foo");

        websiteLayout.showRouterLayoutContent(new Paragraph("bar"));
        assertThat(main.getElement().getChildCount()).isEqualTo(1);
        assertThat(main.getElement().getChild(0).getText()).isEqualTo("bar");

        websiteLayout.removeRouterLayoutContent(null);
        assertThat(main.getElement().getChildCount()).isZero();
    }

    @Test
    void testRouterLayoutContentException() {
        final var content = mock(HasElement.class);
        final var element = mock(Element.class);
        when(content.getElement()).thenReturn(element);
        when(element.getComponent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> websiteLayout.showRouterLayoutContent(content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebsiteLayout content must be a Component");
    }

    @Test
    void testNavigationWithCompleteProfile_shouldSucceed() {
        final var user = userService.storeUser(new UserDto(null, null, null,
                "demoUserComplete", "demo-user-complete@example.com", "Demo User Complete", "", null,
                UserRole.USER, UserType.LOCAL));
        login(user);

        UI.getCurrentOrThrow().navigate(EventGridView.class);

        final var eventGrids = _find(EventGridView.class);
        assertThat(eventGrids).hasSize(1);
    }

    @Test
    void testNavigationWithIncompleteProfile_shouldRedirect() {
        final var user = userService.storeUser(new UserDto(null, null, null,
                "demoUserIncomplete", "demo-user-incomplete@example.com", "", "", null,
                UserRole.USER, UserType.LOCAL));
        login(user);

        UI.getCurrentOrThrow().navigate(EventGridView.class);

        final var eventGrids = _find(EventGridView.class);
        assertThat(eventGrids).isEmpty();

        final var titles = _find(H2.class, spec -> spec.withText("My Profile"));
        assertThat(titles).hasSize(1);
    }
}
