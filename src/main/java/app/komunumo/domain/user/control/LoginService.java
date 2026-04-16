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

import app.komunumo.SecurityConfig;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.user.entity.AuthenticationState;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserPrincipal;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.util.SecurityUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Service responsible for passwordless login, logout, and confirmation-link handling.</p>
 *
 * <p>The service authenticates users via email-based confirmation links and synchronizes the
 * authentication state with the current Vaadin/Spring Security session.</p>
 */
@Service
public final class LoginService {

    /**
     * <p>Logger used for operational login and logout diagnostics.</p>
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(LoginService.class);
    /**
     * <p>Validity window for login confirmation links.</p>
     */
    private static final @NotNull Duration CONFIRMATION_TIMEOUT = Duration.ofMinutes(5);
    /**
     * <p>Query parameter name carrying the confirmation identifier.</p>
     */
    public static final @NotNull String CONFIRMATION_PARAMETER = "confirm";

    /**
     * <p>Shared authentication state used by the UI layer.</p>
     */
    private final @NotNull AuthenticationState authenticationState;
    /**
     * <p>Service used to load users during login and confirmation handling.</p>
     */
    private final @NotNull UserService userService;

    /**
     * <p>In-memory cache storing confirmation data for pending login confirmations.</p>
     *
     * <p>Entries expire automatically after a fixed timeout to limit the validity of confirmation
     * links and to protect against abuse.</p>
     */
    private final @NotNull Cache<@NotNull String, @NotNull ConfirmationData> confirmationCache = Caffeine.newBuilder()
            .expireAfterWrite(CONFIRMATION_TIMEOUT)
            .maximumSize(1_000) // prevent memory overflow (DDOS attack)
            .build();
    /**
     * <p>Service used to resolve instance configuration values.</p>
     */
    private final @NotNull ConfigurationService configurationService;
    /**
     * <p>Service used to send login confirmation mails.</p>
     */
    private final @NotNull MailService mailService;

    /**
     * <p>Creates a new login service instance.</p>
     *
     * @param userService service for loading users by email or identifier
     * @param authenticationState shared UI authentication state
     * @param configurationService service for instance configuration values
     * @param mailService service used to send login confirmation mails
     */
    public LoginService(final @NotNull UserService userService,
                        final @NotNull AuthenticationState authenticationState,
                        final @NotNull ConfigurationService configurationService,
                        final @NotNull MailService mailService) {
        super();
        this.userService = userService;
        this.authenticationState = authenticationState;
        this.configurationService = configurationService;
        this.mailService = mailService;
    }

    /**
     * <p>Performs a login attempt for the given email address.</p>
     *
     * @param emailAddress the email address used to authenticate the user
     * @return {@code true} if authentication was successful, otherwise {@code false}
     */
    public boolean login(final @NotNull String emailAddress) {
        return internalLogin(emailAddress);
    }

    /**
     * <p>Executes the internal authentication workflow for the provided email address.</p>
     *
     * <p>The method validates login eligibility, updates the security context, persists it in the
     * HTTP session, and synchronizes the UI authentication state.</p>
     *
     * @param emailAddress the email address used to authenticate the user
     * @return {@code true} if authentication was successful, otherwise {@code false}
     */
    private boolean internalLogin(final @NotNull String emailAddress) {
        final var optUser = userService.getUserByEmail(emailAddress);
        if (optUser.isEmpty()) {
            LOGGER.info("User with email {} not found.", emailAddress);
            authenticationState.setAuthenticated(false);
            return false;
        }

        final var user = optUser.orElseThrow();
        if (!user.type().isLoginAllowed()) {
            LOGGER.info("User with email {} exists but login is not allowed for type {}", emailAddress, user.type());
            authenticationState.setAuthenticated(false);
            return false;
        }

        final var roles = new ArrayList<GrantedAuthority>();
        roles.add(new SimpleGrantedAuthority(UserRole.USER.getRole()));
        if (user.role().equals(UserRole.ADMIN)) {
            roles.add(new SimpleGrantedAuthority(UserRole.ADMIN.getRole()));
        }
        roles.add(new SimpleGrantedAuthority("ROLE_USER_" + user.type().name()));

        final var authorities = Collections.unmodifiableList(roles);
        final var principal = new UserPrincipal(user, authorities);


        // Authentication-Token without password (passwordless)
        final var authentication = new PreAuthenticatedAuthenticationToken(principal, null, authorities);

        // create and set SecurityContext
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // persist in HTTP session
        final var request = VaadinService.getCurrentRequest();
        final var response = VaadinService.getCurrentResponse();
        if (request instanceof VaadinServletRequest vaadinServletRequest
                && response instanceof VaadinServletResponse vaadinServletResponse) {
            final var httpServletRequest = vaadinServletRequest.getHttpServletRequest();
            final var httpServletResponse = vaadinServletResponse.getHttpServletResponse();
            new HttpSessionSecurityContextRepository().saveContext(context, httpServletRequest, httpServletResponse);
        } else {
            // fallback: should never happen in Vaadin UI context
            LOGGER.warn("No Vaadin servlet request/response available; SecurityContext not saved to session.");
        }

        LOGGER.info("User with email {} successfully logged in.", emailAddress);
        authenticationState.setAuthenticated(true, UserRole.ADMIN.equals(user.role()),
                        UserType.LOCAL.equals(user.type()));

        return true;
    }

    /**
     * <p>Returns the currently authenticated user if available.</p>
     *
     * @return the logged-in user wrapped in an {@link Optional}
     */
    public @NotNull Optional<UserDto> getLoggedInUser() {
        return SecurityUtil.getUserPrincipal()
                .flatMap(principal -> userService.getUserById(principal.getUserId()));
    }

    /**
     * <p>Indicates whether a user is currently logged in.</p>
     *
     * @return {@code true} if a user is authenticated, otherwise {@code false}
     */
    public boolean isUserLoggedIn() {
        return getLoggedInUser().isPresent();
    }

    /**
     * <p>Logs out the current user and redirects to the default logout success URL.</p>
     */
    public void logout() {
        logout(SecurityConfig.LOGOUT_SUCCESS_URL);
    }

    /**
     * <p>Logs out the current user and redirects to the given location.</p>
     *
     * @param location the target location used after logout
     */
    public void logout(final @NotNull String location) {
        authenticationState.setAuthenticated(false);

        final var logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);

        final var ui = UI.getCurrent();
        if (ui == null) {
            LOGGER.warn("No UI available during logout; cannot redirect to '{}'.", location);
        } else {
            ui.getPage().setLocation(location);
        }
    }

    /**
     * <p>Starts the email-based login confirmation process for the given user email.</p>
     *
     * <p>If the email belongs to a login-capable user, a confirmation link is generated, cached,
     * and sent via email. Unknown emails are intentionally ignored in the UI and only logged.</p>
     *
     * @param email the email address to start the login process for
     * @param locale the locale used for the outgoing confirmation email
     */
    public void startLoginProcess(final @NotNull String email,
                                  final @NotNull Locale locale) {
        userService.getUserByEmail(email).ifPresentOrElse(
                user -> {
                    if (user.type().isLoginAllowed()) {
                        final var confirmationId = UUID.randomUUID().toString();
                        final var confirmationData = new ConfirmationData(confirmationId, email);
                        confirmationCache.put(confirmationId, confirmationData);
                        final var confirmationLink = generateConfirmationLink(confirmationData);
                        final var variables = Map.of(
                                "userName", user.name(),
                                "confirmationLink", confirmationLink,
                                "timeoutMinutes", Long.toString(CONFIRMATION_TIMEOUT.toMinutes()));
                        mailService.sendMail(MailTemplateId.LOGIN_CONFIRMATION_MAIL, locale, MailFormat.MARKDOWN, variables, email);
                    } else {
                        LOGGER.warn("User with email '{}' exists, but login is not allowed for type '{}'.", email, user.type());
                    }
                },
                () -> LOGGER.warn("User with email '{}' not found. Ignoring login request.", email)
        );
    }

    /**
     * <p>Validates and handles a login confirmation identifier.</p>
     *
     * <p>The identifier is resolved from the in-memory cache. On successful authentication, the
     * cached confirmation data is removed.</p>
     *
     * @param confirmationId the confirmation identifier from the login link
     * @return {@code true} if login was successful, otherwise {@code false}
     */
    public boolean handleLogin(final @NotNull String confirmationId) {
        final var confirmationData = confirmationCache.getIfPresent(confirmationId);
        if (confirmationData != null) {
            final var email = confirmationData.email();
            if (login(email)) {
                confirmationCache.invalidate(confirmationId);
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Generates an absolute confirmation link for the given confirmation data.</p>
     *
     * @param confirmationData the confirmation data containing the identifier
     * @return a URL pointing to the confirmation endpoint
     */
    private @NotNull String generateConfirmationLink(final @NotNull ConfirmationData confirmationData) {
        final var instanceUrl = configurationService.getConfiguration(ConfigurationSetting.INSTANCE_URL);
        return UriComponentsBuilder.fromUriString(instanceUrl)
                .path(SecurityConfig.LOGIN_URL)
                .queryParam(CONFIRMATION_PARAMETER, confirmationData.id())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    /**
     * <p>Internal data structure holding confirmation identifiers and associated email addresses.</p>
     */
    private record ConfirmationData(
            @NotNull String id,
            @NotNull String email) { }
}
