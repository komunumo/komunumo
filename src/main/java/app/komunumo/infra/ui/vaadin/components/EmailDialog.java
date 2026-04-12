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
package app.komunumo.infra.ui.vaadin.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import org.jetbrains.annotations.NotNull;

import static com.vaadin.flow.component.button.ButtonVariant.PRIMARY;
import static com.vaadin.flow.component.button.ButtonVariant.TERTIARY;
import static com.vaadin.flow.data.value.ValueChangeMode.EAGER;

/**
 * <p>Dialog for collecting and validating an email address before triggering an email-based action.</p>
 *
 * <p>The dialog renders a message, validates the user input, and stores the confirmed address after submit.</p>
 */
public final class EmailDialog extends Dialog {

    /**
     * <p>The email address entered by the user and confirmed via the submit action.</p>
     */
    private @NotNull String emailAddress = "";

    /**
     * <p>Creates a new email dialog instance with the given message content.</p>
     *
     * <p>The message is rendered as {@link Markdown} and the dialog initializes all UI controls and validation.</p>
     *
     * @param message the message shown above the email input field
     */
    public EmailDialog(final @NotNull String message) {
        super();
        addClassName("email-dialog");

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setModality(ModalityMode.STRICT);
        setDraggable(false);
        setResizable(false);

        setHeaderTitle(getTranslation("vaadin.component.EmailDialog.title"));
        final var closeDialogButton = new Button(new Icon("lumo", "cross"),
                _ -> close());
        closeDialogButton.addThemeVariants(ButtonVariant.TERTIARY);
        closeDialogButton.addClassName("close-dialog-button");
        getHeader().add(closeDialogButton);

        final var markdown = new Markdown(message);
        markdown.addClassName("message");
        add(markdown);

        final var emailField = new EmailField();
        emailField.setPlaceholder(getTranslation("vaadin.component.EmailDialog.emailField.placeholder"));
        emailField.setValueChangeMode(EAGER);
        emailField.setWidthFull();
        add(emailField);

        final var submitButton = new Button(getTranslation("vaadin.component.EmailDialog.submitButton.text"),
                _ -> {
                    emailAddress = emailField.getValue();
                    close();
                });
        submitButton.addThemeVariants(PRIMARY);
        submitButton.addClassName("submit-button");
        getFooter().add(submitButton);

        final var cancelButton = new Button(getTranslation("vaadin.component.EmailDialog.cancelButton.text"),
                _ -> close());
        cancelButton.addThemeVariants(TERTIARY);
        cancelButton.addClassName("cancel-button");
        getFooter().add(cancelButton);

        final var binder = new Binder<DummyBean>();
        binder.forField(emailField)
                .asRequired("")
                .withValidator(new EmailValidator(
                        getTranslation("vaadin.component.EmailDialog.emailField.validationError")))
                .bind(_ -> null, (_, _) -> { });
        binder.setBean(new DummyBean());
        binder.addStatusChangeListener(_ -> submitButton.setEnabled(binder.isValid()));
        binder.validate();

        addOpenedChangeListener(evt -> {
            if (evt.isOpened()) {
                emailField.focus();
            }
        });
    }

    /**
     * <p>Returns the email address that was confirmed by submitting the dialog.</p>
     *
     * <p>If the dialog was closed without submit, an empty string is returned.</p>
     *
     * @return the confirmed email address or an empty string
     */
    public @NotNull String getEmailAddress() {
        return emailAddress;
    }

    /**
     * <p>Dummy bean used only to enable {@link Binder} validation for the input field.</p>
     */
    @SuppressWarnings("java:S2094") // DummyBean for Binder (to use validation only)
    private static final class DummyBean { }

}
