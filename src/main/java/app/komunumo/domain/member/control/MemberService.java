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
package app.komunumo.domain.member.control;

import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.confirmation.control.ConfirmationHandler;
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationContext;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationResponse;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.infra.ui.vaadin.control.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Provides membership-related business operations and delegates persistence to {@link MemberStore}.</p>
 *
 * <p>This service forms the control-layer API for member use cases and keeps database access
 * concerns encapsulated in the store implementation.</p>
 */
@Service
public final class MemberService {

    /**
     * <p>Context key used during confirmation flows to store the target community.</p>
     */
    @VisibleForTesting
    static final @NotNull String CONTEXT_KEY_COMMUNITY = "community";

    /**
     * <p>Store responsible for all member persistence operations.</p>
     */
    private final @NotNull MemberStore memberStore;
    /**
     * <p>Service used for sending confirmation and notification mails.</p>
     */
    private final @NotNull MailService mailService;
    /**
     * <p>Service used for loading and creating users.</p>
     */
    private final @NotNull UserService userService;
    /**
     * <p>Service used for accessing the currently logged-in user.</p>
     */
    private final @NotNull LoginService loginService;
    /**
     * <p>Service used for starting and executing confirmation processes.</p>
     */
    private final @NotNull ConfirmationService confirmationService;
    /**
     * <p>Provider used for localized translation texts.</p>
     */
    private final @NotNull TranslationProvider translationProvider;

    /**
     * <p>Creates a new member service.</p>
     *
     * @param memberStore the store used for member persistence access
     * @param mailService the mail service for notifications
     * @param userService the user service for user retrieval and creation
     * @param loginService the login service for current-user context
     * @param confirmationService the confirmation service for join flows
     * @param translationProvider the translation provider for localized texts
     */
    MemberService(final @NotNull MemberStore memberStore,
                         final @NotNull MailService mailService,
                         final @NotNull UserService userService,
                         final @NotNull LoginService loginService,
                         final @NotNull ConfirmationService confirmationService,
                         final @NotNull TranslationProvider translationProvider) {
        this.memberStore = memberStore;
        this.mailService = mailService;
        this.userService = userService;
        this.loginService = loginService;
        this.confirmationService = confirmationService;
        this.translationProvider = translationProvider;
    }

    /**
     * <p>Checks whether the given user is a member of the specified community.</p>
     *
     * <p>Returns {@code true} if the user belongs to the community, otherwise {@code false}.</p>
     *
     * @param user the user to check for membership
     * @param community the community in which membership is verified
     * @return {@code true} if the user is a member of the community, otherwise {@code false}
     */
    public boolean isMember(final @NotNull UserDto user, final @NotNull CommunityDto community) {
        return memberStore.isMember(user, community);
    }

    /**
     * <p>Checks whether the currently logged-in user is a member of the specified community.</p>
     *
     * <p>Returns {@code false} if no user is logged in.</p>
     *
     * @param community the community in which membership is verified
     * @return {@code true} if the logged-in user is a member of the community, otherwise {@code false}
     */
    public boolean isLoggedInUserMemberOf(final @NotNull CommunityDto community) {
        return loginService.getLoggedInUser()
                .map(user -> isMember(user, community))
                .orElse(false);
    }

    /**
     * <p>Stores/Updates the Member record to the database.</p>
     *
     * @param memberDto a DTO representation of the Member information
     * @return the persisted Member information in DTO form
     */
    public @NotNull MemberDto storeMember(final @NotNull MemberDto memberDto) {
        return memberStore.storeMember(memberDto);
    }

    /**
     * <p>Loads all members.</p>
     *
     * @return all members
     */
    public @NotNull List<@NotNull MemberDto> getMembers() {
        return memberStore.getMembers();
    }

    /**
     * <p>Loads one member by user and community.</p>
     *
     * @param user the user to lookup
     * @param community the community to lookup
     * @return an optional containing the member if found; otherwise empty
     */
    public Optional<MemberDto> getMember(final @NotNull UserDto user,
                                         final @NotNull CommunityDto community) {
        return memberStore.getMember(user, community);
    }

    /**
     * <p>Loads all members of a community.</p>
     *
     * @param communityId the community ID
     * @return all members of the community
     */
    public @NotNull List<@NotNull MemberDto> getMembersByCommunityId(final @NotNull UUID communityId) {
        return memberStore.getMembersByCommunityId(communityId);
    }

    /**
     * <p>Loads all members of a community with the given role.</p>
     *
     * @param communityId the community ID
     * @param role the member role to filter by
     * @return all matching members ordered by membership date descending
     */
    public @NotNull List<@NotNull MemberDto> getMembersByCommunityId(final @NotNull UUID communityId,
                                                                     final @NotNull MemberRole role) {
        return memberStore.getMembersByCommunityId(communityId, role);
    }

    /**
     * <p>Counts the total number of members.</p>
     *
     * @return The total count of members; never negative.
     */
    public int getMemberCount() {
        return memberStore.getMemberCount();
    }

    /**
     * <p>Counts members in a specific community.</p>
     *
     * @param communityId the community ID to filter by
     * @return the number of members in the given community; never negative
     */
    public int getMemberCount(final @Nullable UUID communityId) {
        return memberStore.getMemberCount(communityId);
    }

    /**
     * <p>Starts the confirmation flow for joining a community.</p>
     *
     * @param community the community the user wants to join
     * @param locale the locale used for translated messages
     */
    public void joinCommunityStartConfirmationProcess(final @NotNull CommunityDto community,
                                                      final @NotNull Locale locale) {
        final var actionMessage = translationProvider.getTranslation(
                "member.control.MemberService.join.actionText", locale, community.name());
        final ConfirmationHandler actionHandler = this::joinCommunityWithEmail;
        final var actionContext = ConfirmationContext.of(CONTEXT_KEY_COMMUNITY, community);
        final var confirmationRequest = new ConfirmationRequest(
                actionMessage,
                actionHandler,
                actionContext,
                locale
        );
        confirmationService.startConfirmationProcess(confirmationRequest);
    }

    /**
     * <p>Handles a confirmed community join action by email address.</p>
     *
     * <p>If the user does not exist yet, an anonymous user account is created first.</p>
     *
     * @param email the email address from the confirmation flow
     * @param context the confirmation context holding the target community
     * @param locale the locale used for translated messages
     * @return the confirmation response containing status, message, and redirect location
     */
    private @NotNull ConfirmationResponse joinCommunityWithEmail(final @NotNull String email,
                                                                 final @NotNull ConfirmationContext context,
                                                                 final @NotNull Locale locale) {
        final var community = (CommunityDto) context.get(CONTEXT_KEY_COMMUNITY);
        final var user = userService.getUserByEmail(email)
                .orElseGet(() -> userService.createAnonymousUserWithEmail(email));

        joinCommunityWithUser(user, community, locale);

        final var communityName = community.name();
        final var communityLink = LinkUtil.getLink(community);
        final var status = ConfirmationStatus.SUCCESS;
        final var message = translationProvider.getTranslation("member.control.MemberService.join.successMessage",
                locale, communityName);
        return new ConfirmationResponse(status, message, communityLink);
    }

    /**
     * <p>Joins a user to a community and sends the corresponding notification mails.</p>
     *
     * @param user the user that joins the community
     * @param community the community to join
     * @param locale the locale used for translated and templated mails
     */
    public void joinCommunityWithUser(final @NotNull UserDto user,
                                      final @NotNull CommunityDto community,
                                      final @NotNull Locale locale) {
        //noinspection DataFlowIssue // community and user objects are from the DB and are guaranteed to have an ID
        final var member = getMember(user, community) // try to get existing member
                .orElseGet(() -> new MemberDto(user.id(), community.id(), MemberRole.MEMBER, null));
        storeMember(member);

        final var communityName = community.name();
        final var communityLink = LinkUtil.getLink(community);

        final Map<String, String> mailVariables = Map.of(
                "communityName", communityName,
                "communityLink", communityLink,
                "memberCount", Integer.toString(getMemberCount(community.id())));
        if (user.email() != null) {
            mailService.sendMail(MailTemplateId.COMMUNITY_JOIN_SUCCESS_MEMBER, locale, MailFormat.MARKDOWN,
                    mailVariables, user.email());
        } else {
            // User has no email address; cannot send mail
            // Support for remote users without email address could be added here in the future
            throw new UnsupportedOperationException("Cannot send community join mail to user without email address.");
        }

        //noinspection DataFlowIssue // community object is from the DB and guaranteed to have an ID
        getMembersByCommunityId(community.id(), MemberRole.OWNER)
                .forEach(communityOwner -> {
                    //noinspection DataFlowIssue // community owners are always local users with email address set
                    userService.getUserById(communityOwner.userId()).ifPresent(owner ->
                            mailService.sendMail(MailTemplateId.COMMUNITY_JOIN_SUCCESS_OWNER, locale, MailFormat.MARKDOWN,
                                    mailVariables, owner.email()));
                });
    }

    /**
     * <p>Removes a user from a community.</p>
     *
     * <p>Owners cannot leave via this operation.</p>
     *
     * @param userDto the user that wants to leave
     * @param community the community to leave
     * @return {@code true} if the member was removed; otherwise {@code false}
     */
    public boolean leaveCommunity(final @NotNull UserDto userDto, final @NotNull CommunityDto community) {
        return getMember(userDto, community)
                .map(member -> {
                    if (member.role() != MemberRole.OWNER) {
                        return deleteMember(member);
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * <p>Deletes a member relation.</p>
     *
     * @param member the member relation to delete
     * @return {@code true} if a relation was deleted; otherwise {@code false}
     */
    public boolean deleteMember(final @NotNull MemberDto member) {
        return memberStore.deleteMember(member) > 0;
    }

}
