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
import app.komunumo.infra.config.MailConfig;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    @Test
    void sendMailSendsMarkdownMailWithReplyTo() throws Exception {
        final var appConfig = mock(AppConfig.class);
        final var configurationService = mock(ConfigurationService.class);
        final var mailSender = mock(JavaMailSender.class);
        final var mailStore = mock(MailStore.class);
        final var service = new MailService(appConfig, configurationService, mailSender, mailStore);
        final var template = new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH,
                "Test ${instanceName}",
                "Hello from ${instanceUrl}");
        final var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(appConfig.mail()).thenReturn(new MailConfig("sender@example.org", "reply@example.org"));
        when(configurationService.getConfiguration(INSTANCE_NAME)).thenReturn("Komunumo");
        when(configurationService.getConfiguration(INSTANCE_URL)).thenReturn("https://komunumo.example");
        when(mailStore.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH)).thenReturn(Optional.of(template));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        final var result = service.sendMail(MailTemplateId.TEST, Locale.ENGLISH, MailFormat.MARKDOWN,
                null, "user@example.org");

        assertThat(result).isTrue();
        assertThat(mimeMessage.getSubject()).isEqualTo("[Komunumo] Test Komunumo");
        assertThat(mimeMessage.getContentType()).startsWith("text/plain");
        assertThat(mimeMessage.getContent()).isEqualTo("Hello from https://komunumo.example");
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("sender@example.org");
        assertThat(mimeMessage.getReplyTo()[0].toString()).isEqualTo("reply@example.org");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("user@example.org");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendMailSendsHtmlMailWithoutReplyTo() throws Exception {
        final var appConfig = mock(AppConfig.class);
        final var configurationService = mock(ConfigurationService.class);
        final var mailSender = mock(JavaMailSender.class);
        final var mailStore = mock(MailStore.class);
        final var service = new MailService(appConfig, configurationService, mailSender, mailStore);
        final var template = new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH,
                "Test ${instanceName}",
                "Password: ${password}");
        final var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(appConfig.mail()).thenReturn(new MailConfig("sender@example.org", ""));
        when(configurationService.getConfiguration(INSTANCE_NAME)).thenReturn("Komunumo");
        when(configurationService.getConfiguration(INSTANCE_URL)).thenReturn("https://komunumo.example");
        when(mailStore.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH)).thenReturn(Optional.of(template));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        final var result = service.sendMail(MailTemplateId.TEST, Locale.ENGLISH, MailFormat.HTML,
                Map.of("password", "sEcReT"), "user@example.org");

        assertThat(result).isTrue();
        mimeMessage.saveChanges();
        assertThat(mimeMessage.getSubject()).isEqualTo("[Komunumo] Test Komunumo");
        assertThat(mimeMessage.getContentType()).startsWith("text/html");
        assertThat(mimeMessage.getContent().toString()).contains("<p>Password: sEcReT</p>");
        assertThat(mimeMessage.getReplyTo()).hasSize(1);
        assertThat(mimeMessage.getReplyTo()[0].toString()).isEqualTo("sender@example.org");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendMailReturnsFalseWhenSendingFails() {
        final var appConfig = mock(AppConfig.class);
        final var configurationService = mock(ConfigurationService.class);
        final var mailSender = mock(JavaMailSender.class);
        final var mailStore = mock(MailStore.class);
        final var service = new MailService(appConfig, configurationService, mailSender, mailStore);
        final var template = new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH, "Subject", "Body");

        when(appConfig.mail()).thenReturn(new MailConfig("sender@example.org", "reply@example.org"));
        when(configurationService.getConfiguration(INSTANCE_NAME)).thenReturn("Komunumo");
        when(configurationService.getConfiguration(INSTANCE_URL)).thenReturn("https://komunumo.example");
        when(mailStore.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH)).thenReturn(Optional.of(template));
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        final var result = service.sendMail(MailTemplateId.TEST, Locale.ENGLISH, MailFormat.MARKDOWN,
                null, "user@example.org");

        assertThat(result).isFalse();
    }

    @Test
    void sendMailThrowsWhenTemplateIsMissing() {
        final var mailStore = mock(MailStore.class);
        final var service = createService(mailStore);
        when(mailStore.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMail(MailTemplateId.TEST, Locale.ENGLISH,
                MailFormat.MARKDOWN, null, "user@example.org"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void getMailTemplateDelegatesToStore() {
        final var mailStore = mock(MailStore.class);
        final var service = createService(mailStore);
        final var expected = Optional.of(new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH,
                "Subject", "Body"));
        when(mailStore.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH)).thenReturn(expected);

        final var result = service.getMailTemplate(MailTemplateId.TEST, Locale.ENGLISH);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void storeMailTemplateDelegatesToStore() {
        final var mailStore = mock(MailStore.class);
        final var service = createService(mailStore);
        final var template = new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH, "Subject", "Body");
        when(mailStore.storeMailTemplate(template)).thenReturn(template);

        final var result = service.storeMailTemplate(template);

        assertThat(result).isEqualTo(template);
    }

    @Test
    void getMailTemplateCountDelegatesToStore() {
        final var mailStore = mock(MailStore.class);
        final var service = createService(mailStore);
        when(mailStore.getMailTemplateCount()).thenReturn(3);

        final var result = service.getMailTemplateCount();

        assertThat(result).isEqualTo(3);
    }

    @Test
    void getAllMailTemplatesDelegatesToStore() {
        final var mailStore = mock(MailStore.class);
        final var service = createService(mailStore);
        final var expected = List.of(new MailTemplate(MailTemplateId.TEST, Locale.ENGLISH,
                "Subject", "Body"));
        when(mailStore.getAllMailTemplates()).thenReturn(expected);

        final var result = service.getAllMailTemplates();

        assertThat(result).isEqualTo(expected);
    }

    private static MailService createService(final MailStore mailStore) {
        final var appConfig = mock(AppConfig.class);
        final var configurationService = mock(ConfigurationService.class);
        final var mailSender = mock(JavaMailSender.class);
        return new MailService(appConfig, configurationService, mailSender, mailStore);
    }
}
