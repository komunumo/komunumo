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
import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.HandleOwnerContext;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.ui.vaadin.components.HandleField;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Route(value = "settings/profile", layout = WebsiteLayout.class)
@RolesAllowed("USER_LOCAL")
public final class EditProfileView extends AbstractView implements BeforeLeaveObserver {

    private static final int MIN_NAME_LENGTH = 5;

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull LoginService loginService;
    private final @NotNull UserService userService;
    private final @NotNull ActorHandleService actorHandleService;

    /**
     * <p>Creates a new view instance with access to the configuration service for
     * retrieving localized configuration values such as the instance name.</p>
     *
     * @param configurationService the configuration service used to resolve the instance name;
     *                             must not be {@code null}
     * @param loginService the service used to access the currently logged-in user;
     *                     must not be {@code null}
     * @param userService the service used to load and persist user data;
     *                    must not be {@code null}
     * @param actorHandleService the service used to validate and resolve actor handles;
     *                           must not be {@code null}
     */
    public EditProfileView(final @NotNull ConfigurationService configurationService,
                           final @NotNull LoginService loginService,
                           final @NotNull UserService userService,
                           final @NotNull ActorHandleService actorHandleService) {
        super(configurationService);
        this.configurationService = configurationService;
        this.loginService = loginService;
        this.userService = userService;
        this.actorHandleService = actorHandleService;
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

        final var handleField = new HandleField(
                configurationService,
                actorHandleService,
                HandleOwnerContext.forUser(user.id(), user.handle())
        );
        handleField.setLabel(getTranslation("user.boundary.EditProfileView.handle"));
        handleField.addClassName("handle-field");
        handleField.setRequired(true);
        handleField.setWidthFull();

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

        binder.forField(handleField)
                .asRequired(getTranslation("user.boundary.EditProfileView.handle.required"))
                .bind(EditProfileFormData::handle, EditProfileFormData::setHandle);

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
                    formData.handle(),
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

        add(emailField, handleField, nameField, bioField, saveButton);
        focusFirstEmptyField(List.of(
                new FocusableField(handleField::getValue, handleField::focus),
                new FocusableField(nameField::getValue, nameField::focus)
        ));
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

    private void focusFirstEmptyField(final @NotNull List<@NotNull FocusableField> fields) {
        fields.stream()
                .filter(FocusableField::isBlank)
                .findFirst()
                .ifPresent(FocusableField::focus);
    }

    private static final class EditProfileFormData {

        private String email;
        private String handle;
        private String name;
        private String bio;

        EditProfileFormData(final @NotNull UserDto user) {
            setEmail(Optional.ofNullable(user.email()).orElse(""));
            setHandle(user.handle());
            setName(user.name());
            setBio(user.bio());
        }

        String email() {
            return email;
        }

        void setEmail(final @NotNull String email) {
            this.email = email;
        }

        String handle() {
            return handle;
        }

        void setHandle(final @Nullable String handle) {
            this.handle = handle;
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

    private record FocusableField(@NotNull Supplier<@NotNull String> valueSupplier,
                                  @NotNull Runnable focusAction) {

        private boolean isBlank() {
            return valueSupplier.get().isBlank();
        }

        private void focus() {
            focusAction.run();
        }
    }
}
