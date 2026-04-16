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
import app.komunumo.domain.core.confirmation.control.ConfirmationService;
import app.komunumo.domain.core.confirmation.entity.ConfirmationRequest;
import app.komunumo.domain.core.confirmation.entity.ConfirmationStatus;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailFormat;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.user.control.LoginService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.i18n.TranslationProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    @Test
    void isMemberDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        when(memberStore.isMember(user, community)).thenReturn(true);

        final var result = service.isMember(user, community);

        assertThat(result).isTrue();
        verify(memberStore).isMember(user, community);
    }

    @Test
    void isLoggedInUserMemberOfReturnsFalseWhenNoUserIsLoggedIn() {
        final var memberStore = mock(MemberStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(memberStore, loginService);
        final var community = createCommunity();
        when(loginService.getLoggedInUser()).thenReturn(Optional.empty());

        final var result = service.isLoggedInUserMemberOf(community);

        assertThat(result).isFalse();
        verify(loginService).getLoggedInUser();
        verifyNoInteractions(memberStore);
    }

    @Test
    void isLoggedInUserMemberOfReturnsTrueWhenLoggedInUserIsMember() {
        final var memberStore = mock(MemberStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(memberStore, loginService);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        when(loginService.getLoggedInUser()).thenReturn(Optional.of(user));
        when(memberStore.isMember(user, community)).thenReturn(true);

        final var result = service.isLoggedInUserMemberOf(community);

        assertThat(result).isTrue();
        verify(memberStore).isMember(user, community);
    }

    @Test
    void isLoggedInUserMemberOfReturnsFalseWhenLoggedInUserIsNotMember() {
        final var memberStore = mock(MemberStore.class);
        final var loginService = mock(LoginService.class);
        final var service = createService(memberStore, loginService);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        when(loginService.getLoggedInUser()).thenReturn(Optional.of(user));
        when(memberStore.isMember(user, community)).thenReturn(false);

        final var result = service.isLoggedInUserMemberOf(community);

        assertThat(result).isFalse();
        verify(memberStore).isMember(user, community);
    }

    @Test
    void storeMemberDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var member = createMember(MemberRole.MEMBER);
        when(memberStore.storeMember(member)).thenReturn(member);

        final var result = service.storeMember(member);

        assertThat(result).isEqualTo(member);
        verify(memberStore).storeMember(member);
    }

    @Test
    void getMembersDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var expected = List.of(createMember(MemberRole.MEMBER));
        when(memberStore.getMembers()).thenReturn(expected);

        final var result = service.getMembers();

        assertThat(result).isEqualTo(expected);
        verify(memberStore).getMembers();
    }

    @Test
    void getMemberDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        final var expected = Optional.of(createMember(MemberRole.MEMBER));
        when(memberStore.getMember(user, community)).thenReturn(expected);

        final var result = service.getMember(user, community);

        assertThat(result).isEqualTo(expected);
        verify(memberStore).getMember(user, community);
    }

    @Test
    void getMembersByCommunityIdDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var communityId = UUID.randomUUID();
        final var expected = List.of(createMember(MemberRole.MEMBER));
        when(memberStore.getMembersByCommunityId(communityId)).thenReturn(expected);

        final var result = service.getMembersByCommunityId(communityId);

        assertThat(result).isEqualTo(expected);
        verify(memberStore).getMembersByCommunityId(communityId);
    }

    @Test
    void getMembersByCommunityIdAndRoleDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var communityId = UUID.randomUUID();
        final var expected = List.of(createMember(MemberRole.OWNER));
        when(memberStore.getMembersByCommunityId(communityId, MemberRole.OWNER)).thenReturn(expected);

        final var result = service.getMembersByCommunityId(communityId, MemberRole.OWNER);

        assertThat(result).isEqualTo(expected);
        verify(memberStore).getMembersByCommunityId(communityId, MemberRole.OWNER);
    }

    @Test
    void getMemberCountDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        when(memberStore.getMemberCount()).thenReturn(7);

        final var result = service.getMemberCount();

        assertThat(result).isEqualTo(7);
        verify(memberStore).getMemberCount();
    }

    @Test
    void getMemberCountByCommunityDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var communityId = UUID.randomUUID();
        when(memberStore.getMemberCount(communityId)).thenReturn(3);

        final var result = service.getMemberCount(communityId);

        assertThat(result).isEqualTo(3);
        verify(memberStore).getMemberCount(communityId);
    }

    @Test
    void joinCommunityStartConfirmationProcessBuildsRequestAndStartsFlow() {
        final var memberStore = mock(MemberStore.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var service = createService(memberStore, confirmationService, translationProvider);
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        when(translationProvider.getTranslation("member.control.MemberService.join.actionText", locale, community.name()))
                .thenReturn("Join now");

        service.joinCommunityStartConfirmationProcess(community, locale);

        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).startConfirmationProcess(requestCaptor.capture());

        final var request = requestCaptor.getValue();
        assertThat(request.actionMessage()).isEqualTo("Join now");
        assertThat(request.locale()).isEqualTo(locale);
        assertThat(request.actionContext().get(MemberService.CONTEXT_KEY_COMMUNITY)).isEqualTo(community);
    }

    @Test
    void confirmationHandlerUsesExistingUserAndReturnsSuccessResponse() {
        final var memberStore = mock(MemberStore.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var userService = mock(UserService.class);
        final var mailService = mock(MailService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), confirmationService, translationProvider);
        final var community = createCommunity();
        final var locale = Locale.GERMAN;
        final var user = createUser("member@example.org");
        final var member = createMember(MemberRole.MEMBER);

        when(translationProvider.getTranslation("member.control.MemberService.join.actionText", locale, community.name()))
                .thenReturn("Beitreten");
        when(translationProvider.getTranslation("member.control.MemberService.join.successMessage", locale, community.name()))
                .thenReturn("Erfolgreich beigetreten");
        when(userService.getUserByEmail("member@example.org")).thenReturn(Optional.of(user));
        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.getMemberCount(community.id())).thenReturn(5);
        when(memberStore.getMembersByCommunityId(community.id(), MemberRole.OWNER)).thenReturn(List.of());
        when(memberStore.storeMember(member)).thenReturn(member);

        service.joinCommunityStartConfirmationProcess(community, locale);
        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).startConfirmationProcess(requestCaptor.capture());
        final var response = requestCaptor.getValue().actionHandler()
                .handle("member@example.org", requestCaptor.getValue().actionContext(), locale);

        assertThat(response.confirmationStatus()).isEqualTo(ConfirmationStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("Erfolgreich beigetreten");
        assertThat(response.location()).isEqualTo("/communities/" + community.profile());
        verify(userService, never()).createAnonymousUserWithEmail(any());
    }

    @Test
    void confirmationHandlerCreatesAnonymousUserWhenUserDoesNotExist() {
        final var memberStore = mock(MemberStore.class);
        final var confirmationService = mock(ConfirmationService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var userService = mock(UserService.class);
        final var mailService = mock(MailService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), confirmationService, translationProvider);
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        final var user = createUser("new@example.org");
        final var member = createMember(MemberRole.MEMBER);

        when(translationProvider.getTranslation("member.control.MemberService.join.actionText", locale, community.name()))
                .thenReturn("Join");
        when(translationProvider.getTranslation("member.control.MemberService.join.successMessage", locale, community.name()))
                .thenReturn("Joined");
        when(userService.getUserByEmail("new@example.org")).thenReturn(Optional.empty());
        when(userService.createAnonymousUserWithEmail("new@example.org")).thenReturn(user);
        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.getMemberCount(community.id())).thenReturn(5);
        when(memberStore.getMembersByCommunityId(community.id(), MemberRole.OWNER)).thenReturn(List.of());
        when(memberStore.storeMember(member)).thenReturn(member);

        service.joinCommunityStartConfirmationProcess(community, locale);
        final var requestCaptor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        verify(confirmationService).startConfirmationProcess(requestCaptor.capture());
        requestCaptor.getValue().actionHandler()
                .handle("new@example.org", requestCaptor.getValue().actionContext(), locale);

        verify(userService).createAnonymousUserWithEmail("new@example.org");
    }

    @Test
    void joinCommunityWithUserUsesExistingMemberAndSendsMailsToUserAndOwner() {
        final var memberStore = mock(MemberStore.class);
        final var mailService = mock(MailService.class);
        final var userService = mock(UserService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        final var user = createUser("member@example.org");
        final var existingMember = createMember(MemberRole.MEMBER);
        final var ownerMember = new MemberDto(UUID.randomUUID(), community.id(), MemberRole.OWNER, null);
        final var ownerUser = new UserDto(ownerMember.userId(), null, null, "@owner", "owner@example.org",
                "Owner", "", null, UserRole.USER, UserType.LOCAL);

        when(memberStore.getMember(user, community)).thenReturn(Optional.of(existingMember));
        when(memberStore.storeMember(existingMember)).thenReturn(existingMember);
        when(memberStore.getMemberCount(community.id())).thenReturn(4);
        when(memberStore.getMembersByCommunityId(community.id(), MemberRole.OWNER)).thenReturn(List.of(ownerMember));
        when(userService.getUserById(ownerMember.userId())).thenReturn(Optional.of(ownerUser));

        service.joinCommunityWithUser(user, community, locale);

        verify(memberStore).storeMember(existingMember);
        verify(mailService).sendMail(eq(MailTemplateId.COMMUNITY_JOIN_SUCCESS_MEMBER), eq(locale),
                eq(MailFormat.MARKDOWN), any(Map.class), eq("member@example.org"));
        verify(mailService).sendMail(eq(MailTemplateId.COMMUNITY_JOIN_SUCCESS_OWNER), eq(locale),
                eq(MailFormat.MARKDOWN), any(Map.class), eq("owner@example.org"));
    }

    @Test
    void joinCommunityWithUserCreatesNewMemberWhenMissing() {
        final var memberStore = mock(MemberStore.class);
        final var mailService = mock(MailService.class);
        final var userService = mock(UserService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        final var user = createUser("member@example.org");

        when(memberStore.getMember(user, community)).thenReturn(Optional.empty());
        when(memberStore.getMemberCount(community.id())).thenReturn(1);
        when(memberStore.getMembersByCommunityId(community.id(), MemberRole.OWNER)).thenReturn(List.of());
        when(memberStore.storeMember(any(MemberDto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.joinCommunityWithUser(user, community, locale);

        verify(memberStore).storeMember(any(MemberDto.class));
    }

    @Test
    void joinCommunityWithUserThrowsWhenUserHasNoEmailAddress() {
        final var memberStore = mock(MemberStore.class);
        final var mailService = mock(MailService.class);
        final var userService = mock(UserService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        final var user = createUser(null);
        final var member = createMember(MemberRole.MEMBER);

        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.storeMember(member)).thenReturn(member);
        when(memberStore.getMemberCount(community.id())).thenReturn(3);

        assertThatThrownBy(() -> service.joinCommunityWithUser(user, community, locale))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Cannot send community join mail to user without email address.");

        verify(mailService, never()).sendMail(eq(MailTemplateId.COMMUNITY_JOIN_SUCCESS_MEMBER), any(), any(), any(), any());
    }

    @Test
    void joinCommunityWithUserSkipsOwnerMailWhenOwnerUserNotFound() {
        final var memberStore = mock(MemberStore.class);
        final var mailService = mock(MailService.class);
        final var userService = mock(UserService.class);
        final var service = createService(memberStore, mailService, userService,
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
        final var community = createCommunity();
        final var locale = Locale.ENGLISH;
        final var user = createUser("member@example.org");
        final var member = createMember(MemberRole.MEMBER);
        final var ownerMember = new MemberDto(UUID.randomUUID(), community.id(), MemberRole.OWNER, null);

        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.storeMember(member)).thenReturn(member);
        when(memberStore.getMemberCount(community.id())).thenReturn(3);
        when(memberStore.getMembersByCommunityId(community.id(), MemberRole.OWNER)).thenReturn(List.of(ownerMember));
        when(userService.getUserById(ownerMember.userId())).thenReturn(Optional.empty());

        service.joinCommunityWithUser(user, community, locale);

        verify(mailService, times(1)).sendMail(eq(MailTemplateId.COMMUNITY_JOIN_SUCCESS_MEMBER), eq(locale),
                eq(MailFormat.MARKDOWN), any(Map.class), eq("member@example.org"));
        verify(mailService, never()).sendMail(eq(MailTemplateId.COMMUNITY_JOIN_SUCCESS_OWNER), any(), any(), any(), any());
    }

    @Test
    void leaveCommunityReturnsFalseWhenMembershipDoesNotExist() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        when(memberStore.getMember(user, community)).thenReturn(Optional.empty());

        final var result = service.leaveCommunity(user, community);

        assertThat(result).isFalse();
    }

    @Test
    void leaveCommunityReturnsFalseForOwner() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        final var ownerMember = createMember(MemberRole.OWNER);
        when(memberStore.getMember(user, community)).thenReturn(Optional.of(ownerMember));

        final var result = service.leaveCommunity(user, community);

        assertThat(result).isFalse();
        verify(memberStore, never()).deleteMember(any());
    }

    @Test
    void leaveCommunityDeletesNonOwnerAndReturnsTrueOnSuccess() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        final var member = createMember(MemberRole.MEMBER);
        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.deleteMember(member)).thenReturn(1);

        final var result = service.leaveCommunity(user, community);

        assertThat(result).isTrue();
        verify(memberStore).deleteMember(member);
    }

    @Test
    void leaveCommunityDeletesNonOwnerAndReturnsFalseOnFailure() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var user = createUser("member@example.org");
        final var community = createCommunity();
        final var member = createMember(MemberRole.MEMBER);
        when(memberStore.getMember(user, community)).thenReturn(Optional.of(member));
        when(memberStore.deleteMember(member)).thenReturn(0);

        final var result = service.leaveCommunity(user, community);

        assertThat(result).isFalse();
        verify(memberStore).deleteMember(member);
    }

    @Test
    void deleteMemberDelegatesToStore() {
        final var memberStore = mock(MemberStore.class);
        final var service = createService(memberStore);
        final var member = createMember(MemberRole.MEMBER);
        when(memberStore.deleteMember(member)).thenReturn(1);

        final var result = service.deleteMember(member);

        assertThat(result).isTrue();
        verify(memberStore).deleteMember(member);
    }

    private static MemberService createService(final MemberStore memberStore) {
        return createService(memberStore, mock(MailService.class), mock(UserService.class),
                mock(LoginService.class), mock(ConfirmationService.class), mock(TranslationProvider.class));
    }

    private static MemberService createService(final MemberStore memberStore,
                                               final LoginService loginService) {
        return createService(memberStore, mock(MailService.class), mock(UserService.class),
                loginService, mock(ConfirmationService.class), mock(TranslationProvider.class));
    }

    private static MemberService createService(final MemberStore memberStore,
                                               final ConfirmationService confirmationService,
                                               final TranslationProvider translationProvider) {
        return createService(memberStore, mock(MailService.class), mock(UserService.class),
                mock(LoginService.class), confirmationService, translationProvider);
    }

    private static MemberService createService(final MemberStore memberStore,
                                               final MailService mailService,
                                               final UserService userService,
                                               final LoginService loginService,
                                               final ConfirmationService confirmationService,
                                               final TranslationProvider translationProvider) {
        return new MemberService(memberStore, mailService, userService, loginService, confirmationService, translationProvider);
    }

    private static CommunityDto createCommunity() {
        return new CommunityDto(UUID.randomUUID(), "@community", null, null, "Community", "Description", null);
    }

    private static UserDto createUser(final String email) {
        return new UserDto(UUID.randomUUID(), null, null, "@user", email, "User", "Bio", null,
                UserRole.USER, UserType.LOCAL);
    }

    private static MemberDto createMember(final MemberRole role) {
        return new MemberDto(UUID.randomUUID(), UUID.randomUUID(), role, null);
    }
}
