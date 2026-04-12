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

import app.komunumo.test.KaribuTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.assertj.core.api.Assertions.assertThat;

class EmailDialogKT extends KaribuTest {

    @Test
    void initialState_shouldBeConfigured() {
        final var dialog = new EmailDialog("Message");

        assertThat(dialog.isCloseOnEsc()).isTrue();
        assertThat(dialog.isCloseOnOutsideClick()).isTrue();
        assertThat(dialog.isDraggable()).isFalse();
        assertThat(dialog.isResizable()).isFalse();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Confirm your email address");
        assertThat(dialog.getEmailAddress()).isEmpty();

        final var submitButton = _get(dialog, Button.class, spec -> spec.withClasses("submit-button"));
        assertThat(submitButton.isEnabled()).isFalse();
    }

    @Test
    void submitWithValidEmail_shouldStoreEmailAndCloseDialog() {
        final var dialog = new EmailDialog("Message");
        UI.getCurrent().add(dialog);
        dialog.open();

        final var emailField = _get(dialog, EmailField.class);
        _setValue(emailField, "test@example.com");

        final var submitButton = _get(dialog, Button.class, spec -> spec.withClasses("submit-button"));
        assertThat(submitButton.isEnabled()).isTrue();

        _click(submitButton);

        assertThat(dialog.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(dialog.isOpened()).isFalse();
    }

    @Test
    void invalidEmail_shouldKeepSubmitButtonDisabled() {
        final var dialog = new EmailDialog("Message");

        final var emailField = _get(dialog, EmailField.class);
        _setValue(emailField, "invalid-email");

        final var submitButton = _get(dialog, Button.class, spec -> spec.withClasses("submit-button"));
        assertThat(submitButton.isEnabled()).isFalse();
        assertThat(dialog.getEmailAddress()).isEmpty();
    }

    @Test
    void cancelButton_shouldCloseDialogWithoutSettingEmailAddress() {
        final var dialog = new EmailDialog("Message");
        UI.getCurrent().add(dialog);
        dialog.open();

        final var emailField = _get(dialog, EmailField.class);
        _setValue(emailField, "test@example.com");

        final var cancelButton = _get(dialog, Button.class, spec -> spec.withClasses("cancel-button"));
        _click(cancelButton);

        assertThat(dialog.isOpened()).isFalse();
        assertThat(dialog.getEmailAddress()).isEmpty();
    }

    @Test
    void closeButton_shouldCloseDialog_andOpenCloseBranchesShouldExecute() {
        final var dialog = new EmailDialog("Message");
        UI.getCurrent().add(dialog);

        dialog.open();
        MockVaadin.clientRoundtrip(false);
        assertThat(dialog.isOpened()).isTrue();

        final var closeButton = _get(dialog, Button.class, spec -> spec.withClasses("close-dialog-button"));
        _click(closeButton);

        assertThat(dialog.isOpened()).isFalse();
    }
}

