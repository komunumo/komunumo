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

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplate;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.infra.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_URL;
import static app.komunumo.domain.core.mail.entity.MailFormat.HTML;
import static app.komunumo.domain.core.mail.entity.MailFormat.MARKDOWN;
import static app.komunumo.util.MarkdownUtil.convertMarkdownToHtml;
import static app.komunumo.util.TemplateUtil.replaceVariables;

/**
 * <p>Provides mail-related use cases and delegates persistence operations to {@link MailStore}.</p>
 *
 * <p>This service encapsulates mail composition and delivery behavior while database access
 * for mail templates remains in the store implementation.</p>
 */
@Service
public final class MailService {

    /**
     * <p>Logger used to record successful and failed mail delivery attempts.</p>
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    /**
     * <p>Application configuration containing sender and reply-to addresses.</p>
     */
    private final @NotNull AppConfig appConfig;
    /**
     * <p>Service used to resolve instance-wide configuration values for templates.</p>
     */
    private final @NotNull ConfigurationService configurationService;
    /**
     * <p>Mail sender used to create and dispatch MIME messages.</p>
     */
    private final @NotNull JavaMailSender mailSender;
    /**
     * <p>Store responsible for loading and persisting mail templates.</p>
     */
    private final @NotNull MailStore mailStore;

    /**
     * <p>Creates a new mail service.</p>
     *
     * @param appConfig the application configuration containing mail sender settings
     * @param configurationService the configuration service used for template variables
     * @param mailSender the mail sender used to dispatch messages
     * @param mailStore the store used to load and persist mail templates
     */
    MailService(final @NotNull AppConfig appConfig,
                final @NotNull ConfigurationService configurationService,
                final @NotNull JavaMailSender mailSender,
                final @NotNull MailStore mailStore) {
        this.appConfig = appConfig;
        this.configurationService = configurationService;
        this.mailSender = mailSender;
        this.mailStore = mailStore;
    }

    /**
     * <p>Sends a mail based on the requested template, locale, and output format.</p>
     *
     * <p>Template variables are enriched with instance-level variables ({@code instanceName},
     * {@code instanceUrl}) before rendering subject and body.</p>
     *
     * @param mailTemplateId the template identifier
     * @param locale the locale used to resolve the template language
     * @param format the desired output format for the mail body
     * @param variables optional template variables provided by the caller
     * @param emailAddresses one or more recipient addresses
     * @return {@code true} if sending succeeded; otherwise {@code false}
     */
    public boolean sendMail(final @NotNull MailTemplateId mailTemplateId,
                            final @NotNull Locale locale,
                            final @NotNull MailFormat format,
                            final @Nullable Map<String, String> variables,
                            final @NotNull String... emailAddresses) {
        final var instanceName = configurationService.getConfiguration(INSTANCE_NAME);
        final var instanceUrl = configurationService.getConfiguration(INSTANCE_URL);
        final HashMap<String, String> allVariables = new HashMap<>();
        if (variables != null) {
            allVariables.putAll(variables);
        }
        allVariables.put("instanceName", instanceName);
        allVariables.put("instanceUrl", instanceUrl);

        final var mailTemplate = getMailTemplate(mailTemplateId, locale).orElseThrow();
        final var subject = "[%s] %s".formatted(instanceName, replaceVariables(mailTemplate.subject(), allVariables));
        final var markdown = replaceVariables(mailTemplate.markdown(), allVariables);

        try {
            final var mimeMessage = mailSender.createMimeMessage();
            final var helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setTo(emailAddresses);
            helper.setFrom(appConfig.mail().from());

            final var replyTo = appConfig.mail().replyTo();
            if (!replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            helper.setSubject(subject);

            final var body = format == MARKDOWN
                    ? markdown
                    : convertMarkdownToHtml(markdown);
            helper.setText(body, format == HTML);

            mailSender.send(mimeMessage);

            LOGGER.info("Mail with subject '{}' successfully sent to {}",
                    subject, emailAddresses);
            return true;
        } catch (final Exception e) {
            LOGGER.error("Unable to send mail with subject '{}' to {}: {}",
                    subject, emailAddresses, e.getMessage());
            return false;
        }
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
        return mailStore.getMailTemplate(mailTemplateId, locale);
    }

    /**
     * <p>Stores or updates a mail template.</p>
     *
     * @param mailTemplate the template to persist
     * @return the persisted template
     */
    public @NotNull MailTemplate storeMailTemplate(final @NotNull MailTemplate mailTemplate) {
        return mailStore.storeMailTemplate(mailTemplate);
    }

    /**
     * <p>Counts the total number of mail templates.</p>
     *
     * @return the total count of mail templates; never negative
     */
    public int getMailTemplateCount() {
        return mailStore.getMailTemplateCount();
    }

    /**
     * <p>Returns all mail templates stored in the database.</p>
     *
     * <p>This method retrieves all mail template entries including all language variants.
     * It is primarily used for exporting the instance mail templates.</p>
     *
     * @return a list of all mail templates
     */
    public @NotNull List<@NotNull MailTemplate> getAllMailTemplates() {
        return mailStore.getAllMailTemplates();
    }
}
