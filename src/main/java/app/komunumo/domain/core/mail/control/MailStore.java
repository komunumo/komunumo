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
package app.komunumo.domain.core.mail.control;

import app.komunumo.domain.core.mail.entity.MailTemplate;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.util.LocaleUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static app.komunumo.data.db.tables.MailTemplate.MAIL_TEMPLATE;

/**
 * <p>Handles persistence operations for mail templates.</p>
 *
 * <p>This store encapsulates all jOOQ database access for loading, creating, updating,
 * counting, and listing template variants.</p>
 */
@Service
final class MailStore {

    /**
     * <p>jOOQ DSL context used for all database operations in this store.</p>
     */
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new mail store.</p>
     *
     * @param dsl the jOOQ DSL context used for database access
     */
    MailStore(final @NotNull DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * <p>Loads a mail template by template ID and locale.</p>
     *
     * @param mailTemplateId the template identifier
     * @param locale the locale used to resolve the language variant
     * @return an optional containing the template if found; otherwise empty
     */
    public @NotNull Optional<MailTemplate> getMailTemplate(final @NotNull MailTemplateId mailTemplateId,
                                                            final @NotNull Locale locale) {
        final var languageCode = LocaleUtil.getLanguageCode(locale);
        return dsl.selectFrom(MAIL_TEMPLATE)
                .where(MAIL_TEMPLATE.ID.eq(mailTemplateId.name()))
                .and(MAIL_TEMPLATE.LANGUAGE.eq(languageCode))
                .fetchOptionalInto(MailTemplate.class);
    }

    /**
     * <p>Stores or updates a mail template.</p>
     *
     * @param mailTemplate the template to persist
     * @return the persisted template
     */
    public @NotNull MailTemplate storeMailTemplate(final @NotNull MailTemplate mailTemplate) {
        final var languageCode = LocaleUtil.getLanguageCode(mailTemplate.language());
        final var mailTemplateRecord = dsl.selectFrom(MAIL_TEMPLATE)
                .where(MAIL_TEMPLATE.ID.eq(mailTemplate.id().name()))
                .and(MAIL_TEMPLATE.LANGUAGE.eq(languageCode))
                .fetchOptional()
                .orElse(dsl.newRecord(MAIL_TEMPLATE));
        mailTemplateRecord.setId(mailTemplate.id().name());
        mailTemplateRecord.setLanguage(languageCode);
        mailTemplateRecord.setSubject(mailTemplate.subject());
        mailTemplateRecord.setMarkdown(mailTemplate.markdown());
        mailTemplateRecord.store();
        return mailTemplateRecord.into(MailTemplate.class);
    }

    /**
     * <p>Counts the total number of mail templates.</p>
     *
     * @return the total count of mail templates; never negative
     */
    public int getMailTemplateCount() {
        return Optional.ofNullable(
                dsl.selectCount()
                        .from(MAIL_TEMPLATE)
                        .fetchOne(0, Integer.class)
        ).orElse(0);
    }

    /**
     * <p>Returns all mail templates stored in the database.</p>
     *
     * @return a list of all mail templates including all language variants
     */
    public @NotNull List<@NotNull MailTemplate> getAllMailTemplates() {
        return dsl.selectFrom(MAIL_TEMPLATE)
                .fetch()
                .map(record -> new MailTemplate(
                        MailTemplateId.valueOf(record.get(MAIL_TEMPLATE.ID)),
                        Locale.forLanguageTag(record.get(MAIL_TEMPLATE.LANGUAGE)),
                        record.get(MAIL_TEMPLATE.SUBJECT),
                        record.get(MAIL_TEMPLATE.MARKDOWN)
                ));
    }
}
