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

import app.komunumo.domain.core.activitypub.control.ActorHandleService;
import app.komunumo.domain.core.activitypub.entity.HandleOwnerContext;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_ALLOWED_CHARACTERS_PATTERN;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_MAX_LENGTH;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_MIN_LENGTH;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_PATTERN;

/**
 * <p>Custom Vaadin field for entering and validating local actor handles.</p>
 *
 * <p>The field renders the local part of a federated handle together with the
 * configured instance domain, validates syntax and length locally, and checks
 * availability through {@link ActorHandleService}. When the current value matches
 * the persisted handle of the owner, no availability message is shown.</p>
 */
public final class HandleField extends CustomField<String> implements HasValueChangeMode {

    private final @NotNull TextField textField = new TextField();
    private final @NotNull Paragraph message = new Paragraph();
    private final @NotNull HandleOwnerContext ownerContext;

    private @NotNull HandleFieldI18N i18n;

    /**
     * <p>Creates a new handle field.</p>
     *
     * @param configurationService the configuration service used to resolve the instance domain
     * @param actorHandleService the service used to check handle availability
     * @param ownerContext the owner context for persisted handle comparisons and ownership checks
     */
    public HandleField(final @NotNull ConfigurationService configurationService,
                       final @NotNull ActorHandleService actorHandleService,
                       final @NotNull HandleOwnerContext ownerContext) {
        super();
        this.ownerContext = ownerContext;
        i18n = createDefaultI18n();
        addClassName("handle-field");
        setWidthFull();

        message.setClassName("handle-message");
        final var domainName = configurationService.getConfiguration(ConfigurationSetting.INSTANCE_DOMAIN);

        textField.setMinLength(HANDLE_MIN_LENGTH);
        textField.setMaxLength(HANDLE_MAX_LENGTH);
        textField.setAllowedCharPattern(HANDLE_PATTERN);

        textField.setPrefixComponent(new Span("@"));
        textField.setSuffixComponent(new Span("@" + domainName));

        textField.setWidthFull();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addValueChangeListener(valueChangeEvent -> {
            final var value = valueChangeEvent.getValue();
            if (value.length() < HANDLE_MIN_LENGTH
                    || value.length() > HANDLE_MAX_LENGTH) {
                showErrorMessage(MessageFormat.format(i18n.getErrorLength(), HANDLE_MIN_LENGTH, HANDLE_MAX_LENGTH));
            } else if (isPersistedValue(value)) {
                clearMessage();
            } else if (actorHandleService.isHandleAvailable(value, ownerContext)) {
                showSuccessMessage(i18n.getHandleAvailable());
            } else {
                showErrorMessage(i18n.getHandleNotAvailable());
            }
            setModelValue(value, valueChangeEvent.isFromClient());
        });

        add(textField, message);
    }

    /**
     * <p>Creates the default localized messages for the handle field from the translation provider.</p>
     *
     * @return the default I18N object
     */
    private @NotNull HandleFieldI18N createDefaultI18n() {
        return new HandleFieldI18N()
                .setHandleAvailable(getTranslation("vaadin.components.HandleField.handleAvailable"))
                .setHandleNotAvailable(getTranslation("vaadin.components.HandleField.handleNotAvailable"))
                .setErrorLength(getTranslation("vaadin.components.HandleField.errorLength"))
                .setSyntaxError(getTranslation("vaadin.components.HandleField.syntaxError"));
    }

    /**
     * <p>Shows a success message below the input field.</p>
     *
     * @param successMessage the localized success message to display
     */
    private void showSuccessMessage(final @NotNull String successMessage) {
        message.setText(successMessage);
        message.removeClassName("error");
        message.addClassName("success");
    }

    /**
     * <p>Shows an error message below the input field.</p>
     *
     * @param errorMessage the localized error message to display
     */
    private void showErrorMessage(final @NotNull String errorMessage) {
        message.setText(errorMessage);
        message.removeClassName("success");
        message.addClassName("error");
    }

    /**
     * <p>Clears any success or error message shown below the input field.</p>
     */
    private void clearMessage() {
        message.setText("");
        message.removeClassName("success");
        message.removeClassName("error");
    }

    /**
     * <p>Checks whether the current field value matches the persisted handle of the owner.</p>
     *
     * @param value the current field value
     * @return {@code true} if the value is non-blank and equals the persisted handle; otherwise {@code false}
     */
    private boolean isPersistedValue(final @NotNull String value) {
        return !value.isBlank() && value.equals(ownerContext.persistedHandle());
    }

    /**
     * <p>Returns the current model value of the field.</p>
     *
     * @return the current handle value
     */
    @Override
    protected String generateModelValue() {
        return textField.getValue();
    }

    /**
     * <p>Updates the visible field value without changing the external API contract.</p>
     *
     * @param value the value to present in the inner text field
     */
    @Override
    protected void setPresentationValue(final String value) {
        textField.setValue(value);
    }

    /**
     * <p>Sets the placeholder text shown by the inner text field.</p>
     *
     * @param placeholder the placeholder text to show; may be {@code null}
     */
    public void setPlaceholder(@Nullable final String placeholder) {
        textField.setPlaceholder(placeholder);
    }

    /**
     * <p>Returns the placeholder text shown by the inner text field.</p>
     *
     * @return the placeholder text; may be {@code null}
     */
    public @Nullable String getPlaceholder() {
        return textField.getPlaceholder();
    }

    /**
     * <p>Sets the localized messages used by the handle field.</p>
     *
     * @param i18n the localized messages to use
     */
    public void setI18n(final @NotNull HandleFieldI18N i18n) {
        this.i18n = i18n;
    }

    /**
     * <p>Returns the localized messages used by the handle field.</p>
     *
     * @return the localized messages
     */
    public @NotNull HandleFieldI18N getI18n() {
        return i18n;
    }

    /**
     * <p>Sets whether the field is read-only.</p>
     *
     * @param readOnly {@code true} to make the field read-only; otherwise {@code false}
     */
    @Override
    public void setReadOnly(final boolean readOnly) {
        textField.setReadOnly(readOnly);
    }

    /**
     * <p>Returns whether the field is currently read-only.</p>
     *
     * @return {@code true} if the field is read-only; otherwise {@code false}
     */
    @Override
    public boolean isReadOnly() {
        return textField.isReadOnly();
    }

    /**
     * <p>Sets whether the field is required.</p>
     *
     * @param required {@code true} if the field should be marked as required; otherwise {@code false}
     */
    public void setRequired(final boolean required) {
        setRequiredIndicatorVisible(required);
    }

    /**
     * <p>Returns whether the field is marked as required.</p>
     *
     * @return {@code true} if the field is marked as required; otherwise {@code false}
     */
    public boolean isRequired() {
        return isRequiredIndicatorVisible();
    }

    /**
     * <p>Sets the current handle value after validating its basic syntax.</p>
     *
     * @param value the handle value to set
     */
    @Override
    public void setValue(final @NotNull String value) {
        if (!value.isBlank() && !value.matches(HANDLE_ALLOWED_CHARACTERS_PATTERN)) {
            showErrorMessage(MessageFormat.format(i18n.getSyntaxError(), value));
            return;
        }

        super.setValue(value);
    }

    /**
     * <p>Returns the value change mode used by the inner text field.</p>
     *
     * @return the current value change mode
     */
    @Override
    public @NotNull ValueChangeMode getValueChangeMode() {
        return textField.getValueChangeMode();
    }

    /**
     * <p>Sets the value change mode used by the inner text field.</p>
     *
     * @param valueChangeMode the value change mode to apply
     */
    @Override
    public void setValueChangeMode(final @NotNull ValueChangeMode valueChangeMode) {
        textField.setValueChangeMode(valueChangeMode);
    }

    /**
     * <p>Returns the empty value representation of the field.</p>
     *
     * @return the empty value, which is always an empty string
     */
    @Override
    public String getEmptyValue() {
        return "";
    }

    /**
     * <p>Container for localized handle field messages, following Vaadin's I18N object pattern.</p>
     */
    public static final class HandleFieldI18N {

        private @NotNull String handleAvailable;
        private @NotNull String handleNotAvailable;
        private @NotNull String errorLength;
        private @NotNull String syntaxError;

        /**
         * <p>Creates a new localized message container for the handle field.</p>
         */
        public HandleFieldI18N() {
            handleAvailable = "";
            handleNotAvailable = "";
            errorLength = "";
            syntaxError = "";
        }

        /**
         * <p>Returns the localized message shown when the handle is available.</p>
         *
         * @return the localized availability message
         */
        public @NotNull String getHandleAvailable() {
            return handleAvailable;
        }

        /**
         * <p>Sets the localized message shown when the handle is available.</p>
         *
         * @param handleAvailableMessage the localized availability message
         * @return this I18N object
         */
        public @NotNull HandleFieldI18N setHandleAvailable(final @NotNull String handleAvailableMessage) {
            this.handleAvailable = handleAvailableMessage;
            return this;
        }

        /**
         * <p>Returns the localized message shown when the handle is not available.</p>
         *
         * @return the localized unavailability message
         */
        public @NotNull String getHandleNotAvailable() {
            return handleNotAvailable;
        }

        /**
         * <p>Sets the localized message shown when the handle is not available.</p>
         *
         * @param handleNotAvailableMessage the localized unavailability message
         * @return this I18N object
         */
        public @NotNull HandleFieldI18N setHandleNotAvailable(final @NotNull String handleNotAvailableMessage) {
            this.handleNotAvailable = handleNotAvailableMessage;
            return this;
        }

        /**
         * <p>Returns the localized error message for invalid handle length.</p>
         *
         * @return the localized error message
         */
        public @NotNull String getErrorLength() {
            return errorLength;
        }

        /**
         * <p>Sets the localized error message for invalid handle length.</p>
         *
         * @param errorLengthMessage the localized error message
         * @return this I18N object
         */
        public @NotNull HandleFieldI18N setErrorLength(final @NotNull String errorLengthMessage) {
            this.errorLength = errorLengthMessage;
            return this;
        }

        /**
         * <p>Returns the localized syntax error message.</p>
         *
         * @return the localized syntax error message
         */
        public @NotNull String getSyntaxError() {
            return syntaxError;
        }

        /**
         * <p>Sets the localized syntax error message.</p>
         *
         * @param syntaxErrorMessage the localized syntax error message
         * @return this I18N object
         */
        public @NotNull HandleFieldI18N setSyntaxError(final @NotNull String syntaxErrorMessage) {
            this.syntaxError = syntaxErrorMessage;
            return this;
        }
    }
}
