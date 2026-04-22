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
package app.komunumo.domain.user.boundary;

import app.komunumo.SecurityConfig;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.ui.vaadin.components.MarkdownEditor;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.NotificationUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Route(value = "settings/profile", layout = WebsiteLayout.class)
@RolesAllowed("USER_LOCAL")
public final class EditProfileView extends AbstractView implements BeforeLeaveObserver {

    private static final int MIN_NAME_LENGTH = 5;

    private final @NotNull LoginService loginService;
    private final @NotNull UserService userService;

    /**
     * <p>Creates a new view instance with access to the configuration service for
     * retrieving localized configuration values such as the instance name.</p>
     *
     * @param configurationService the configuration service used to resolve the instance name;
     *                             must not be {@code null}
     */
    public EditProfileView(final @NotNull ConfigurationService configurationService,
                           final @NotNull LoginService loginService,
                           final @NotNull UserService userService) {
        super(configurationService);
        this.loginService = loginService;
        this.userService = userService;
        createUserInterface();
        addClassName("edit-profile-view");
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("user.boundary.EditProfileView.title");
    }

    private void createUserInterface() {
        final var user = loginService.getLoggedInUser()
                .orElseThrow(() -> new IllegalStateException("No logged-in user"));

        add(new H2(getViewTitle()));

        final var emailField = new EmailField(getTranslation("user.boundary.EditProfileView.email"));
        emailField.addClassName("email-field");
        emailField.setReadOnly(true);
        emailField.setWidthFull();

        final var nameField = new TextField(getTranslation("user.boundary.EditProfileView.name"));
        nameField.addClassName("name-field");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setValueChangeMode(ValueChangeMode.EAGER);
        nameField.setWidthFull();

        final var bioField = new MarkdownEditor(getLocale());
        bioField.setLabel(getTranslation("user.boundary.EditProfileView.bio"));
        bioField.addClassName("bio-field");
        bioField.setWidthFull();

        final var binder = new Binder<EditProfileFormData>();

        binder.forField(emailField)
                .asRequired(getTranslation("user.boundary.EditProfileView.email.required"))
                .bind(EditProfileFormData::email, EditProfileFormData::setEmail);

        binder.forField(nameField)
                .asRequired(getTranslation("user.boundary.EditProfileView.name.required"))
                .withValidator(name -> name.length() >= MIN_NAME_LENGTH,
                        getTranslation("user.boundary.EditProfileView.name.length", MIN_NAME_LENGTH))
                .bind(EditProfileFormData::name, EditProfileFormData::setName);

        binder.forField(bioField)
                .bind(EditProfileFormData::bio, EditProfileFormData::setBio);

        final var formData = new EditProfileFormData(user);
        binder.setBean(formData);
        binder.validate();

        final var saveButton = new Button(getTranslation("user.boundary.EditProfileView.save"));
        saveButton.addClassName("save-button");
        saveButton.addClickListener(_ -> {
            if (!binder.validate().isOk()) {
                return;
            }

            final var updatedUser = new UserDto(
                    user.id(),
                    user.created(),
                    user.updated(),
                    user.handle(),
                    user.email(),
                    formData.name().trim(),
                    formData.bio().trim(),
                    user.imageId(),
                    user.role(),
                    user.type()
            );

            userService.storeUser(updatedUser);
            NotificationUtil.showNotification(
                    getTranslation("user.boundary.EditProfileView.saveSuccess"),
                    NotificationVariant.SUCCESS);
        });

        add(emailField, nameField, bioField, saveButton);

        if (nameField.getValue().isBlank()) {
            nameField.focus();
        }
    }

    @Override
    public void beforeLeave(final @NotNull BeforeLeaveEvent event) {
        // logout is always allowed
        if (event.getLocation().getPath().equals(SecurityConfig.LOGOUT_URL)) {
            return;
        }

        // stop navigating away if the profile is incomplete
        if (!userService.isProfileComplete(loginService.getLoggedInUser().orElseThrow())) {
            event.postpone().cancel();
            if (event.getTrigger() != NavigationTrigger.PROGRAMMATIC) {
                NotificationUtil.showNotification(
                        getTranslation("user.boundary.EditProfileView.profileIncomplete"),
                        NotificationVariant.ERROR);
            }
        }
    }

    private static final class EditProfileFormData {

        private String email;
        private String name;
        private String bio;

        EditProfileFormData(final @NotNull UserDto user) {
            setEmail(Optional.ofNullable(user.email()).orElse(""));
            setName(user.name());
            setBio(user.bio());
        }

        String email() {
            return email;
        }

        void setEmail(final @NotNull String email) {
            this.email = email;
        }

        String name() {
            return name;
        }

        void setName(final @NotNull String name) {
            this.name = name;
        }

        String bio() {
            return bio;
        }

        public void setBio(final @NotNull String bio) {
            this.bio = bio;
        }
    }
}
