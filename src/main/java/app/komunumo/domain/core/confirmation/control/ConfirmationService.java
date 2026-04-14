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
package app.komunumo.domain.core.confirmation.control;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.confirmation.boundary.ConfirmationDialog;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.infra.i18n.TranslationProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_NAME;
import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_URL;
import static app.komunumo.domain.core.mail.entity.MailFormat.MARKDOWN;
import static app.komunumo.domain.core.mail.entity.MailTemplateId.CONFIRMATION_PROCESS;

@Service
public final class ConfirmationService {

    private static final @NotNull Duration CONFIRMATION_TIMEOUT = Duration.ofMinutes(5);
    private static final @NotNull String CONFIRMATION_PATH = "/confirm";
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ConfirmationService.class);

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull MailService mailService;
    private final @NotNull TranslationProvider translationProvider;

    private final @NotNull Cache<@NotNull String, @NotNull ConfirmationData> confirmationCache = Caffeine.newBuilder()
            .expireAfterWrite(CONFIRMATION_TIMEOUT)
            .maximumSize(1_000) // prevent memory overflow (DDOS attack)
            .build();

    public ConfirmationService(final @NotNull ConfigurationService configurationService,
                               final @NotNull MailService mailService,
                               final @NotNull TranslationProvider translationProvider) {
        super();
        this.configurationService = configurationService;
        this.mailService = mailService;
        this.translationProvider = translationProvider;
    }

    public void startConfirmationProcess(final @NotNull ConfirmationRequest confirmationRequest) {
        new ConfirmationDialog(this, confirmationRequest).open();
    }

    public void sendConfirmationMail(final @NotNull String email,
                                     final @NotNull ConfirmationRequest confirmationRequest) {
        final var confirmationId = UUID.randomUUID().toString();
        final var confirmationData = new ConfirmationData(confirmationId, email, confirmationRequest);
        confirmationCache.put(confirmationId, confirmationData);

        final var locale = confirmationRequest.locale();
        final var instanceName = configurationService.getConfiguration(INSTANCE_NAME);
        final var confirmationLink = generateConfirmationLink(confirmationData);
        final var confirmationTimeout = getConfirmationTimeoutText(locale);
        final var actionMessage = confirmationRequest.actionMessage();

        final Map<String, String> variables = Map.of(
                "instanceName", instanceName,
                "confirmationLink", confirmationLink,
                "confirmationTimeout", confirmationTimeout,
                "actionMessage", actionMessage);
        mailService.sendMail(CONFIRMATION_PROCESS, locale, MARKDOWN, variables, email);
    }

    private String generateConfirmationLink(final @NotNull ConfirmationData confirmationData) {
        final var instanceUrl = configurationService.getConfiguration(INSTANCE_URL);
        return UriComponentsBuilder
                .fromUriString(instanceUrl)
                .path(CONFIRMATION_PATH)
                .queryParam("id", confirmationData.id())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public @NotNull Duration getConfirmationTimeout() {
        return CONFIRMATION_TIMEOUT;
    }

    public @NotNull String getConfirmationTimeoutText(final @Nullable Locale locale) {
        final long minutes = CONFIRMATION_TIMEOUT.toMinutes();
        return translationProvider.getTranslation(
                "core.confirmation.control.ConfirmationService.timeout", locale, minutes);
    }

    public @NotNull ConfirmationResponse confirm(final @NotNull String confirmationId,
                                                 final @NotNull Locale locale) {
        final var confirmationData = confirmationCache.getIfPresent(confirmationId);
        if (confirmationData != null) {
            try {
                final var email = confirmationData.email();
                final var confirmationRequest = confirmationData.confirmationRequest();
                final var actionContext = confirmationRequest.actionContext();
                final var actionHandler = confirmationRequest.actionHandler();
                final var response = actionHandler.handle(email, actionContext, locale);
                if (!response.confirmationStatus().equals(ConfirmationStatus.ERROR)) {
                    confirmationCache.invalidate(confirmationId);
                }
                return response;
            } catch (final Exception exception) {
                LOGGER.error("Error in 'actionHandler' for confirmation ID {}: {}",
                        confirmationId, exception.getMessage(), exception);
                final var message = translationProvider.getTranslation(
                        "core.confirmation.control.ConfirmationService.handlerError", locale);
                return new ConfirmationResponse(ConfirmationStatus.ERROR, message, "");
            }
        }
        LOGGER.warn("Invalid or expired confirmation ID: {}", confirmationId);
        final var confirmationTimeout = getConfirmationTimeoutText(locale);
        final var message = translationProvider.getTranslation(
                "core.confirmation.control.ConfirmationService.error", locale, confirmationTimeout);
        return new ConfirmationResponse(ConfirmationStatus.ERROR, message, "");
    }

    private record ConfirmationData(
            @NotNull String id,
            @NotNull String email,
            @NotNull ConfirmationRequest confirmationRequest) { }

}
