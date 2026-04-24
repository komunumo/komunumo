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

import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.HandleOwnerContext;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.infra.ui.vaadin.components.HandleField;
import app.komunumo.infra.ui.vaadin.components.ImageUpload;
import app.komunumo.infra.ui.vaadin.components.MarkdownEditor;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.SecurityUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Objects;

import static app.komunumo.util.NotificationUtil.showNotification;

@RolesAllowed("USER_LOCAL")
@Route(value = "/communities/new", layout = WebsiteLayout.class)
public class CreateCommunityView extends AbstractView {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull CommunityService communityService;
    private final @NotNull MemberService memberService;
    private final @NotNull ActorHandleService actorHandleService;
    private final @NotNull ImageService imageService;

    private final @NotNull Binder<CommunityDto> binder = new Binder<>(CommunityDto.class);

    public CreateCommunityView(final @NotNull ConfigurationService configurationService,
                               final @NotNull CommunityService communityService,
                               final @NotNull MemberService memberService,
                               final @NotNull ActorHandleService actorHandleService,
                               final @NotNull ImageService imageService) {
        super(configurationService);
        this.configurationService = configurationService;
        this.communityService = communityService;
        this.memberService = memberService;
        this.actorHandleService = actorHandleService;
        this.imageService = imageService;

        addClassName("create-community-view");
        add(new H2(getTranslation("community.boundary.CreateCommunityView.title")));
        add(createForm());
    }

    /**
     * <p>The fragment of the page title that will appear before the instance name, separated by a hyphen.</p>
     *
     * @return the translated page title value, based on locale
     */
    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("community.boundary.CreateCommunityView.title");
    }

    private @NotNull Component createForm() {
        final var handleField = new HandleField(configurationService, actorHandleService, HandleOwnerContext.none());
        handleField.addClassName("handle-field");
        handleField.setLabel(getTranslation("community.boundary.CreateCommunityView.label.profile"));
        handleField.setRequired(true);
        handleField.setWidthFull();

        final var nameField = new TextField();
        nameField.addClassName("name-field");
        nameField.setValueChangeMode(ValueChangeMode.EAGER);
        nameField.setLabel(getTranslation("community.boundary.CreateCommunityView.label.name"));
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setWidthFull();

        final var descriptionField = new MarkdownEditor(getLocale());
        descriptionField.addClassName("description-field");
        descriptionField.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionField.setLabel(getTranslation("community.boundary.CreateCommunityView.label.description"));
        descriptionField.setWidthFull();

        final var imageField = new ImageUpload(imageService);
        imageField.addClassName("image-field");
        imageField.setLabel(getTranslation("community.boundary.CreateCommunityView.label.image"));
        imageField.setWidthFull();

        binder.forField(handleField)
                .asRequired(getTranslation("community.boundary.CreateCommunityView.validation.handle.required"))
                .bind(CommunityDto::handle, null);

        binder.forField(nameField)
                .asRequired(getTranslation("community.boundary.CreateCommunityView.validation.name.required"))
                .bind(CommunityDto::name, null);

        final var createButton = new Button(getTranslation("community.boundary.CreateCommunityView.createButton"), _ -> {
            var userPrincipalOptional = SecurityUtil.getUserPrincipal();

            if (userPrincipalOptional.isEmpty()) {
                showNotification(getTranslation("community.boundary.CreateCommunityView.permissionError"),
                        NotificationVariant.ERROR);
            } else if (binder.validate().isOk()) {
                final var image = imageField.getValue();
                final var imageId = image != null ? image.id() : null;
                final var newCommunity = new CommunityDto(null, handleField.getValue(), null, null,
                        nameField.getValue(), descriptionField.getValue(), imageId);
                final var community = communityService.storeCommunity(newCommunity);

                var userPrincipal = userPrincipalOptional.orElseThrow(NotFoundException::new);
                var memberDto = new MemberDto(userPrincipal.getUserId(),
                        Objects.requireNonNull(community.id()), MemberRole.OWNER, null);
                memberService.storeMember(memberDto);

                showNotification(getTranslation("community.boundary.CreateCommunityView.notification.success"),
                        NotificationVariant.SUCCESS);
                UI.getCurrent().navigate("communities/@%s".formatted(community.handle()));

            } else {
                showNotification(getTranslation("community.boundary.CreateCommunityView.notification.error"),
                        NotificationVariant.ERROR);
            }
        });

        createButton.addClassName("create-button");

        return new VerticalLayout(handleField, nameField, descriptionField, imageField, createButton);
    }

    /**
     * <p>Getter method to retrieve the binder object for this component.</p>
     *
     * @return the binder object
     */
    @VisibleForTesting
    @NotNull Binder<CommunityDto> getBinder() {
        return binder;
    }

}
