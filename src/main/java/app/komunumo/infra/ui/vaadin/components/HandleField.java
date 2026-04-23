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

import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_ALLOWED_CHARACTERS_PATTERN;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_MAX_LENGTH;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_MIN_LENGTH;
import static app.komunumo.domain.core.activitypub.control.ActorHandleService.HANDLE_PATTERN;

public final class HandleField extends CustomField<String> implements HasValueChangeMode {

    private final TextField textField = new TextField();
    private final Paragraph message = new Paragraph();

    public HandleField(final @NotNull ConfigurationService configurationService,
                       final @NotNull ActorHandleService actorHandleService) {
        super();
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
                showErrorMessage(getTranslation("vaadin.components.HandleField.errorLength",
                        HANDLE_MIN_LENGTH, HANDLE_MAX_LENGTH));
            } else if (actorHandleService.isHandleAvailable(value)) {
                showSuccessMessage(getTranslation("vaadin.components.HandleField.handleAvailable"));
            } else {
                showErrorMessage(getTranslation("vaadin.components.HandleField.handleNotAvailable"));
            }
            setModelValue(value, valueChangeEvent.isFromClient());
        });

        add(textField, message);
    }

    @SuppressWarnings("SameParameterValue")
    private void showSuccessMessage(final @NotNull String successMessage) {
        message.setText(successMessage);
        message.removeClassName("error");
        message.addClassName("success");
    }

    private void showErrorMessage(final @NotNull String errorMessage) {
        message.setText(errorMessage);
        message.removeClassName("success");
        message.addClassName("error");
    }

    @Override
    protected String generateModelValue() {
        return textField.getValue();
    }

    @Override
    protected void setPresentationValue(final String value) {
        textField.setValue(value);
    }

    public void setPlaceholder(@Nullable final String placeholder) {
        textField.setPlaceholder(placeholder);
    }

    public @Nullable String getPlaceholder() {
        return textField.getPlaceholder();
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        textField.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return textField.isReadOnly();
    }

    public void setRequired(final boolean required) {
        setRequiredIndicatorVisible(required);
    }

    public boolean isRequired() {
        return isRequiredIndicatorVisible();
    }

    @Override
    public void setValue(final @NotNull String value) {
        if (!value.isBlank() && !value.matches(HANDLE_ALLOWED_CHARACTERS_PATTERN)) {
            showErrorMessage(getTranslation("vaadin.components.HandleField.syntaxError", value));
            return;
        }

        super.setValue(value);
    }

    @Override
    public @NotNull ValueChangeMode getValueChangeMode() {
        return textField.getValueChangeMode();
    }

    @Override
    public void setValueChangeMode(final @NotNull ValueChangeMode valueChangeMode) {
        textField.setValueChangeMode(valueChangeMode);
    }

    @Override
    public String getEmptyValue() {
        return "";
    }

}
