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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.dom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Year;

public final class PageFooter extends Footer {

    public PageFooter(final @NotNull UI ui,
                      final @NotNull String version,
                      final @Nullable String privacyUrl,
                      final @Nullable String imprintUrl) {
        super();
        addClassName("page-footer");

        final var komunumoFooter = ui.getTranslation("vaadin.components.PageFooter.komunumo",
                version, String.valueOf(Year.now().getValue()));
        add(new Anchor("https://komunumo.app/", komunumoFooter, AnchorTarget.BLANK));

        if (privacyUrl != null && !privacyUrl.isEmpty()) {
            getElement().appendChild(new Element("br"));
            final var privacyText = ui.getTranslation("vaadin.components.PageFooter.privacy");
            add(new Anchor(privacyUrl, privacyText, AnchorTarget.BLANK));
        }

        if (imprintUrl != null && !imprintUrl.isEmpty()) {
            if (privacyUrl != null && !privacyUrl.isEmpty()) {
                add(new Text(" | "));
            } else {
                getElement().appendChild(new Element("br"));
            }
            final var imprintText = ui.getTranslation("vaadin.components.PageFooter.imprint");
            add(new Anchor(imprintUrl, imprintText, AnchorTarget.BLANK));
        }
    }

}
