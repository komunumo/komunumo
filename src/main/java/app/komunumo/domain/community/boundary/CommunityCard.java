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
package app.komunumo.domain.community.boundary;

import app.komunumo.domain.community.entity.CommunityWithImageDto;
import app.komunumo.infra.ui.vaadin.components.KomunumoCard;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import org.jetbrains.annotations.NotNull;

public class CommunityCard extends KomunumoCard {

    public CommunityCard(final @NotNull CommunityWithImageDto communityWithImage) {
        super(communityWithImage.community().name(), communityWithImage.image());
        addClassName("community-card");

        final var community = communityWithImage.community();
        setSubtitle(new Div("@".concat(community.handle())));
        addClickListener((_ -> UI.getCurrent().navigate(LinkUtil.getLink(community))));
    }

}
