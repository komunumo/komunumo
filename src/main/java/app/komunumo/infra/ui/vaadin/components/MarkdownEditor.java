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

import app.komunumo.util.LocaleUtil;
import app.komunumo.util.ResourceUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class MarkdownEditor extends CustomField<String> implements HasValueChangeMode {

    private static final @NotNull String HELP = "/META-INF/resources/files/editor-help-%s.md";
    private static final @NotNull String HELP_EN = HELP.formatted("en");
    private static final @NotNull Cache<@NotNull String, @NotNull String> CACHE =
            Caffeine.newBuilder()
                    .maximumSize(100)
                    .build();

    private final @NotNull TextArea editor;
    private final @NotNull Markdown preview;

    public MarkdownEditor(final @NotNull Locale locale) {
        super();
        addClassName("markdown-editor");

        // create editor
        editor = new TextArea();
        editor.setSizeFull();

        // create preview
        preview = new Markdown();
        preview.setSizeFull();

        // create help
        final Markdown help = new Markdown();
        help.setSizeFull();

        // get translated tab labels
        final var editLabelText = getTranslation("vaadin.component.MarkdownEditor.edit");
        final var previewLabelText = getTranslation("vaadin.component.MarkdownEditor.preview");
        final var helpLabelText = getTranslation("vaadin.component.MarkdownEditor.help");

        // tab sheet to switch between edit and preview
        final var tabSheet = new TabSheet();
        tabSheet.add(editLabelText, editor);
        tabSheet.add(previewLabelText, preview);
        tabSheet.add(helpLabelText, help);
        tabSheet.setSizeFull();
        add(tabSheet);

        tabSheet.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().getLabel().equals(previewLabelText)) {
                // update preview when text area changes
                preview.setContent(editor.getValue());
            } else if (event.getSelectedTab().getLabel().equals(helpLabelText)) {
                // load help content if not already loaded
                if (help.getContent() == null) {
                    help.setContent(loadHelpContent(locale));
                }
            }
        });

        // propagate value changes from editor to the CustomField
        editor.addValueChangeListener(e ->
                setModelValue(e.getValue(), e.isFromClient()));
    }

    private @NotNull String loadHelpContent(final @NotNull Locale locale) {
        final var language = LocaleUtil.getLanguageCode(locale).toLowerCase(locale);
        return CACHE.get(language, _ ->
                ResourceUtil.getResourceAsString(HELP.formatted(language),
                        ResourceUtil.getResourceAsString(HELP_EN,
                                getTranslation("vaadin.component.MarkdownEditor.help.error")))
        );
    }

    @Override
    protected String generateModelValue() {
        return editor.getValue();
    }

    @Override
    protected void setPresentationValue(final String value) {
        editor.setValue(value);
    }

    public void setPlaceholder(@Nullable final String placeholder) {
        editor.setPlaceholder(placeholder);
    }

    public @Nullable String getPlaceholder() {
        return editor.getPlaceholder();
    }

    public void setRequired(final boolean required) {
        setRequiredIndicatorVisible(required);
    }

    public boolean isRequired() {
        return isRequiredIndicatorVisible();
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        editor.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return editor.isReadOnly();
    }

    @Override
    public @NotNull ValueChangeMode getValueChangeMode() {
        return editor.getValueChangeMode();
    }

    @Override
    public void setValueChangeMode(final @NotNull ValueChangeMode valueChangeMode) {
        editor.setValueChangeMode(valueChangeMode);
    }

    @Override
    public String getEmptyValue() {
        return "";
    }
}
