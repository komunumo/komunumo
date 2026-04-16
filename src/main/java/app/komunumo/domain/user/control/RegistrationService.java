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
package app.komunumo.domain.user.control;

import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.confirmation.control.ConfirmationHandler;
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationContext;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.i18n.TranslationProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

import static app.komunumo.domain.core.config.entity.ConfigurationSetting.INSTANCE_REGISTRATION_ALLOWED;

/**
 * <p>Provides registration use cases including confirmation flow, account upsert, and post-registration login.</p>
 */
@Service
public final class RegistrationService {

    /**
     * <p>Logger used for operational registration diagnostics.</p>
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);
    /**
     * <p>Confirmation-context key holding the entered display name.</p>
     */
    private static final @NotNull String CONTEXT_REGISTRATION_NAME = "name";
    /**
     * <p>Target location used after successful registration.</p>
     */
    private static final @NotNull String REGISTRATION_SUCCESS_REDIRECT = "/settings/profile";

    /**
     * <p>Service used to read registration-related configuration values.</p>
     */
    private final @NotNull ConfigurationService configurationService;
    /**
     * <p>Service used to load and persist user data.</p>
     */
    private final @NotNull UserService userService;
    /**
     * <p>Service used to create an authenticated session after registration.</p>
     */
    private final @NotNull LoginService loginService;
    /**
     * <p>Service used to send registration-related mails.</p>
     */
    private final @NotNull MailService mailService;
    /**
     * <p>Service used to drive the email confirmation flow.</p>
     */
    private final @NotNull ConfirmationService confirmationService;
    /**
     * <p>Provider used for localized registration texts.</p>
     */
    private final @NotNull TranslationProvider translationProvider;

    /**
     * <p>Creates a new registration service.</p>
     *
     * @param configurationService service for registration feature flags
     * @param userService service for user lookup and persistence
     * @param loginService service for post-registration login
     * @param mailService service for registration success mails
     * @param confirmationService service for confirmation-process handling
     * @param translationProvider provider for localized confirmation texts
     */
    public RegistrationService(final @NotNull ConfigurationService configurationService,
                               final @NotNull UserService userService,
                               final @NotNull LoginService loginService,
                               final @NotNull MailService mailService,
                               final @NotNull ConfirmationService confirmationService,
                               final @NotNull TranslationProvider translationProvider) {
        super();
        this.configurationService = configurationService;
        this.userService = userService;
        this.loginService = loginService;
        this.mailService = mailService;
        this.confirmationService = confirmationService;
        this.translationProvider = translationProvider;
    }

    /**
     * <p>Starts the registration confirmation process.</p>
     *
     * <p>If registration is disabled, the request is ignored and logged.</p>
     *
     * @param name the display name entered during registration
     * @param email the email address to register
     * @param locale the locale used for translated confirmation texts
     */
    public void startRegistrationProcess(final @NotNull String name,
                                         final @NotNull String email,
                                         final @NotNull Locale locale) {
        if (!configurationService.getConfiguration(INSTANCE_REGISTRATION_ALLOWED, Boolean.class)) {
            LOGGER.warn("Registration attempt while registration is disabled.");
            return;
        }

        final var actionMessage = translationProvider.getTranslation("user.control.AccountService.registrationText", locale);
        final ConfirmationHandler actionHandler = this::passwordlessRegistrationHandler;
        final var actionContext = ConfirmationContext.of(CONTEXT_REGISTRATION_NAME, name);
        final var confirmationRequest = new ConfirmationRequest(
                actionMessage,
                actionHandler,
                actionContext,
                locale
        );
        confirmationService.sendConfirmationMail(email, confirmationRequest);
    }

    /**
     * <p>Handles the confirmed passwordless registration action.</p>
     *
     * <p>Creates a new local user when missing or upgrades an existing non-local user to local.
     * Afterwards it sends a registration-success mail and logs the user in.</p>
     *
     * @param email the confirmed email address
     * @param context the confirmation context containing optional registration data
     * @param locale the locale used for translated messages and templates
     * @return a success response including redirect target
     */
    private @NotNull ConfirmationResponse passwordlessRegistrationHandler(final @NotNull String email,
                                                                          final @NotNull ConfirmationContext context,
                                                                          final @NotNull Locale locale) {
        final var name = (String) context.getOrDefault(CONTEXT_REGISTRATION_NAME, "");
        var localUser = userService.getUserByEmail(email).orElseGet(() -> createNewLocalUser(name, email));

        if (localUser.type() != UserType.LOCAL) {
            localUser = userService.changeUserType(localUser, UserType.LOCAL);
            userService.storeUser(localUser);
        }

        final var variables = Map.of("name", (String) context.getOrDefault(CONTEXT_REGISTRATION_NAME, ""));
        mailService.sendMail(MailTemplateId.ACCOUNT_REGISTRATION_SUCCESS, locale, MailFormat.MARKDOWN, variables, email);
        loginService.login(email);

        final var status = ConfirmationStatus.SUCCESS;
        final var message = translationProvider.getTranslation("user.control.AccountService.successMessage", locale);
        return new ConfirmationResponse(status, message, REGISTRATION_SUCCESS_REDIRECT);
    }

    /**
     * <p>Creates and stores a new local user for registration.</p>
     *
     * @param name the display name to assign
     * @param email the email address to assign
     * @return the persisted local user
     */
    private @NotNull UserDto createNewLocalUser(final @NotNull String name, final @NotNull String email) {
        return userService.storeUser(new UserDto(null, null, null, null, email, name, "", null,
                UserRole.USER, UserType.LOCAL));
    }

}
