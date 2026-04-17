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
package app.komunumo.domain.participant.control;

import app.komunumo.domain.core.confirmation.control.ConfirmationHandler;
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationContext;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.participant.entity.RegisteredParticipantDto;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Provides participant-related business operations and delegates persistence to {@link ParticipantStore}.</p>
 *
 * <p>This service forms the control-layer API for participation use cases and keeps database access
 * concerns encapsulated in the store implementation.</p>
 */
@Service
public final class ParticipantService {

    /**
     * <p>Context key used during confirmation flows to store the target event.</p>
     */
    @VisibleForTesting
    static final @NotNull String CONTEXT_KEY_EVENT = "event";

    /**
     * <p>Logger used for defensive warnings on invalid registration input.</p>
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

    /**
     * <p>Store responsible for participant persistence and read operations.</p>
     */
    private final @NotNull ParticipantStore participantStore;
    /**
     * <p>Service used to send registration and unregistration mails.</p>
     */
    private final @NotNull MailService mailService;
    /**
     * <p>Service used for event-related validation checks.</p>
     */
    private final @NotNull EventService eventService;
    /**
     * <p>Service used to resolve and create users by email.</p>
     */
    private final @NotNull UserService userService;
    /**
     * <p>Service used to access the currently logged-in user.</p>
     */
    private final @NotNull LoginService loginService;
    /**
     * <p>Service used to start and execute confirmation processes.</p>
     */
    private final @NotNull ConfirmationService confirmationService;
    /**
     * <p>Provider used for localized translation texts.</p>
     */
    private final @NotNull TranslationProvider translationProvider;

    /**
     * <p>Creates a new participant service.</p>
     *
     * @param participantStore the store used for participant persistence access
     * @param mailService the mail service for notifications
     * @param eventService the event service for registration eligibility checks
     * @param userService the user service for user retrieval and creation
     * @param loginService the login service for current-user context
     * @param confirmationService the confirmation service for registration flows
     * @param translationProvider the translation provider for localized texts
     */
    ParticipantService(final @NotNull ParticipantStore participantStore,
                       final @NotNull MailService mailService,
                       final @NotNull EventService eventService,
                       final @NotNull UserService userService,
                       final @NotNull LoginService loginService,
                       final @NotNull ConfirmationService confirmationService,
                       final @NotNull TranslationProvider translationProvider) {
        this.participantStore = participantStore;
        this.mailService = mailService;
        this.eventService = eventService;
        this.userService = userService;
        this.loginService = loginService;
        this.confirmationService = confirmationService;
        this.translationProvider = translationProvider;
    }

    /**
     * <p>Starts the confirmation process for an event registration.</p>
     *
     * @param event the event to register for
     * @param locale the locale used for translated confirmation messages
     */
    public void startConfirmationProcess(final @NotNull EventDto event,
                                         final @NotNull Locale locale) {
        final var actionMessage = translationProvider.getTranslation(
                "participant.control.ParticipantService.actionText", locale, event.title());
        final ConfirmationHandler actionHandler = this::handleConfirmationResponse;
        final var actionContext = ConfirmationContext.of(CONTEXT_KEY_EVENT, event);
        final var confirmationRequest = new ConfirmationRequest(
                actionMessage,
                actionHandler,
                actionContext,
                locale
        );
        confirmationService.startConfirmationProcess(confirmationRequest);
    }

    /**
     * <p>Handles the confirmation response for event registration.</p>
     *
     * <p>If no user exists for the given email, an anonymous user is created.</p>
     *
     * @param email the email address entered in the confirmation flow
     * @param context the confirmation context containing the target event
     * @param locale the locale used for translated response messages
     * @return the confirmation response indicating success or warning
     */
    @VisibleForTesting
    @NotNull ConfirmationResponse handleConfirmationResponse(final @NotNull String email,
                                                             final @NotNull ConfirmationContext context,
                                                             final @NotNull Locale locale) {
        final var event = (EventDto) context.get(CONTEXT_KEY_EVENT);
        final var user = userService.getUserByEmail(email)
                .orElseGet(() -> userService.createAnonymousUserWithEmail(email));

        final var eventTitle = event.title();
        final var eventLink = LinkUtil.getLink(event);

        if (registerForEvent(event, user, locale)) {
            final var status = ConfirmationStatus.SUCCESS;
            final var message = translationProvider.getTranslation(
                    "participant.control.ParticipantService.registrationSuccessMessage", locale, eventTitle);
            return new ConfirmationResponse(status, message, eventLink);
        }

        final var status = ConfirmationStatus.WARNING;
        final var message = translationProvider.getTranslation(
                "participant.control.ParticipantService.registrationFailedMessage", locale, eventTitle);
        return new ConfirmationResponse(status, message, eventLink);
    }

    /**
     * <p>Registers a user for an event and sends corresponding notification mails.</p>
     *
     * @param event the event to register for
     * @param user the user to register
     * @param locale the locale used for translated and templated mails
     * @return {@code true} if registration succeeded; otherwise {@code false}
     */
    public boolean registerForEvent(final @NotNull EventDto event,
                                    final @NotNull UserDto user,
                                    final @NotNull Locale locale) {
        if (!eventService.isRegistrationAllowed(event)) {
            LOGGER.warn("Attempted to register for an event where registration is not allowed. Event: {}", event);
            return false;
        }

        if (event.id() == null) {
            LOGGER.warn("Attempted to register for an event where the event ID is NULL. Event: {}", event);
            return false;
        }

        if (user.id() == null) {
            LOGGER.warn("Attempted to register for event where the user ID is NULL. User: {}", user);
            return false;
        }

        final var participant = getParticipant(event, user)
                .orElseGet(() -> new ParticipantDto(event.id(), user.id(), null));
        storeParticipant(participant);

        final var eventTitle = event.title();
        final var eventLink = LinkUtil.getLink(event);

        final var email = user.email();
        if (email != null && !email.isBlank()) {
            final Map<String, String> mailVariables = Map.of("eventTitle", eventTitle, "eventLink", eventLink);
            mailService.sendMail(MailTemplateId.EVENT_REGISTRATION_SUCCESS, locale, MailFormat.MARKDOWN,
                    mailVariables, email);
        }

        notifyEventManagersAboutParticipationChange(event, user, locale,
                MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS);

        return true;
    }

    /**
     * <p>Notifies all event managers about a participant change.</p>
     *
     * @param event the event for which the change happened
     * @param user the user who caused the participation change
     * @param locale the locale used for translated mail content
     * @param mailTemplateId the mail template to use for manager notification
     */
    private void notifyEventManagersAboutParticipationChange(final @NotNull EventDto event,
                                                             final @NotNull UserDto user,
                                                             final @NotNull Locale locale,
                                                             final @NotNull MailTemplateId mailTemplateId) {
        final var recipientEmails = participantStore.getManagerEmailsForEvent(event).toArray(String[]::new);

        final Map<String, String> mailVariables = Map.of(
                "eventTitle", event.title(),
                "participantName", resolveParticipantName(user, locale),
                "participantCount", Integer.toString(getParticipantCount(event))
        );
        mailService.sendMail(mailTemplateId, locale, MailFormat.MARKDOWN, mailVariables, recipientEmails);
    }

    /**
     * <p>Resolves the display name for participant notifications.</p>
     *
     * <p>If the user has no explicit name, a localized anonymous placeholder is used.</p>
     *
     * @param user the participant user
     * @param locale the locale used for translation fallback
     * @return the resolved participant name
     */
    private @NotNull String resolveParticipantName(final @NotNull UserDto user,
                                                   final @NotNull Locale locale) {
        if (!user.name().isBlank()) {
            return user.name();
        }
        return translationProvider.getTranslation("participant.control.ParticipantService.anonymousName", locale);
    }

    /**
     * <p>Stores or updates a participant relation.</p>
     *
     * @param participant the participant relation to persist
     */
    public void storeParticipant(final @NotNull ParticipantDto participant) {
        participantStore.storeParticipant(participant);
    }

    /**
     * <p>Loads all participants.</p>
     *
     * @return all persisted participants
     */
    public @NotNull List<@NotNull ParticipantDto> getAllParticipants() {
        return participantStore.getAllParticipants();
    }

    /**
     * <p>Loads a participant relation by event and user.</p>
     *
     * @param event the event context of the participant
     * @param user the user context of the participant
     * @return an optional containing the participant relation if found; otherwise empty
     */
    public @NotNull Optional<ParticipantDto> getParticipant(final @NotNull EventDto event,
                                                            final @NotNull UserDto user) {
        return participantStore.getParticipant(event, user);
    }

    /**
     * <p>Deletes a participant relation.</p>
     *
     * @param participant the participant relation to delete
     * @return {@code true} if a relation was deleted; otherwise {@code false}
     */
    public boolean deleteParticipant(final @NotNull ParticipantDto participant) {
        return participantStore.deleteParticipant(participant) > 0;
    }

    /**
     * <p>Counts all persisted participants.</p>
     *
     * @return the total number of participants; never negative
     */
    public int getParticipantCount() {
        return participantStore.getParticipantCount();
    }

    /**
     * <p>Counts participants for a specific event.</p>
     *
     * @param event the event whose participants should be counted
     * @return the number of participants of the event; never negative
     */
    public int getParticipantCount(final @NotNull EventDto event) {
        return participantStore.getParticipantCount(event);
    }

    /**
     * <p>Checks whether a user is participant of an event.</p>
     *
     * @param user the user to check
     * @param event the event to check
     * @return {@code true} if the user participates in the event; otherwise {@code false}
     */
    public boolean isParticipant(final @NotNull UserDto user,
                                 final @NotNull EventDto event) {
        return participantStore.isParticipant(user, event);
    }

    /**
     * <p>Checks whether the currently logged-in user participates in the given event.</p>
     *
     * @param event the event to check
     * @return {@code true} if the logged-in user participates; otherwise {@code false}
     */
    public boolean isLoggedInUserParticipantOf(final @NotNull EventDto event) {
        return loginService.getLoggedInUser()
                .map(user -> isParticipant(user, event))
                .orElse(false);
    }

    /**
     * <p>Unregisters a user from an event and sends corresponding notification mails.</p>
     *
     * @param user the user to unregister
     * @param event the event from which to unregister
     * @param locale the locale used for translated and templated mails
     * @return {@code true} if unregistration succeeded; otherwise {@code false}
     */
    public boolean unregisterFromEvent(final @NotNull UserDto user,
                                       final @NotNull EventDto event,
                                       final @NotNull Locale locale) {
        return getParticipant(event, user)
                .map(participant -> {
                    final var success = deleteParticipant(participant);
                    if (success) {
                        final var email = user.email();
                        if (email != null && !email.isBlank()) {
                            final var eventTitle = event.title();
                            final var eventLink = LinkUtil.getLink(event);
                            final Map<String, String> mailVariables = Map.of(
                                    "eventTitle", eventTitle, "eventLink", eventLink);
                            mailService.sendMail(MailTemplateId.EVENT_UNREGISTRATION_SUCCESS, locale,
                                    MailFormat.MARKDOWN, mailVariables, email);
                        }
                        notifyEventManagersAboutParticipationChange(event, user, locale,
                                MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS);
                    }
                    return success;
                })
                .orElse(false);
    }

    /**
     * <p>Loads all participants of an event including user information and registration timestamp.</p>
     *
     * @param event the event whose participants should be loaded
     * @return ordered participants with user projection and registration timestamp
     */
    public @NotNull List<@NotNull RegisteredParticipantDto> getParticipants(final @NotNull EventDto event) {
        return participantStore.getParticipants(event);
    }
}
