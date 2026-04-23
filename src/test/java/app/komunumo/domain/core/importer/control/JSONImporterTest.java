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
package app.komunumo.domain.core.importer.control;

import app.komunumo.KomunumoException;
import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.util.ImageUtil;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JSONImporterTest {

    private static final String IDENTIFIED_COUNTS_MESSAGE = "Identified 7 settings, 4 images, 6 users, 7 communities, 7 events, 25 members, 7 participants, 4 global pages, and 5 mail templates.";
    private static final String TEST_BASE_URL =
            requireNonNull(System.getProperty("komunumo.test.base-url"),
                    "Missing system property: komunumo.test.base-url");
    private static final UUID UUID_ZERO = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Test
    void testImporterWithURLNotFound() {
        final var jsonUrl = importUrl("non-existing.json");
        final var expectedMessage = "Failed to download JSON data from URL: " + jsonUrl;
        assertThatThrownBy(() -> new JSONImporter(new ImporterLog(null), jsonUrl))
                .isInstanceOf(KomunumoException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void testImporterWithFileNotFound() {
        final var jsonFile = Path.of("non-existing.json").toFile();
        final var expectedMessage = "Failed to load JSON data from file: " + jsonFile.getName();
        assertThatThrownBy(() -> new JSONImporter(new ImporterLog(null), jsonFile))
                .isInstanceOf(KomunumoException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void testSettingsNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No settings found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importSettings(mock(ConfigurationService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testImagesNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No images found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importImages(mock(ImageService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testUsersNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No users found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importUsers(mock(UserService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testCommunitiesNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No communities found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importCommunities(mock(CommunityService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testMembersNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No members found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importMembers(mock(MemberService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testEventsNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No events found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importEvents(mock(EventService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testParticipantsNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No participants found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importParticipants(mock(ParticipantService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testGlobalPagesNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No global pages found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importGlobalPages(mock(GlobalPageService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testMailTemplatesNotFound() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "No mail templates found in JSON data.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importMailTemplates(mock(MailService.class));
            assertThat(logCaptor.getWarnLogs()).contains(expectedMessage);
        }
    }

    @Test
    void testImporterWithRealJson() {
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            new JSONImporter(new ImporterLog(null), jsonUrl);
            assertThat(logCaptor.getInfoLogs()).containsExactly(IDENTIFIED_COUNTS_MESSAGE);
        }
    }

    @Test
    void testImportSettings() {
        final var configurationService = mock(ConfigurationService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(configurationService)
                .setConfiguration(argThat(setting -> "simulated.failure".equals(setting.setting())), any());
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importSettings(configurationService);
            verify(configurationService, times(4)).setConfiguration(any(), any(), any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing settings...",
                    "...finished importing 4 settings.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping setting 'instance.slogan' because it is language-dependent but no language was provided.",
                    "Skipping setting 'instance.createCommunityAllowed' because it is not language-dependent but a language was provided.",
                    "Skipping setting '{\"setting\":\"simulated.failure\",\"value\":\"The test will throw an exception if this setting is read.\"}': Unknown setting: simulated.failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportImages() {
        final var imageService = mock(ImageService.class);
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class);
             var mockedImageUtil = mockStatic(ImageUtil.class)) {

            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importImages(imageService);

            mockedImageUtil.verify(() -> ImageUtil.storeImage(any(), any()), times(2));
            verify(imageService, times(2)).storeImage(any());

            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing images...",
                    "...finished importing 2 images.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    ("Skipping image '{\"imageId\":\"d7bd2d09-3310-4e37-ad0a-c7c4c43389ad\",\"contentType\":\"image/svg+xml\",\"url\":\"%s/import/non-existing.svg\"}': " +
                            "Failed to download file from '%s/import/non-existing.svg': HTTP status code 404")
                            .formatted(TEST_BASE_URL, TEST_BASE_URL),
                    "Skipping image '{\"imageId\":\"c81bca0e-6a1f-422c-b03f-aee75bee6779\",\"contentType\":\"image/png\",\"url\":\"data:broken\"}': Invalid data URL: data:broken");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportUsers() {
        final var userService = mock(UserService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(userService)
                .storeUser(argThat(user -> UUID_ZERO.equals(user.id())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importUsers(userService);
            verify(userService, times(6)).storeUser(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing users...",
                    "...finished importing 5 users.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping user '{\"userId\":\"00000000-0000-0000-0000-000000000000\",\"email\":\"anonymous@example.com\",\"name\":\"Doris Duplicate\",\"bio\":\"User with duplicate email for integration tests\",\"imageId\":\"\",\"role\":\"USER\",\"type\":\"ANONYMOUS\"}': Simulated failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportUsersWithNullOrMissingHandle() {
        final var userService = mock(UserService.class);
        final var importedUsers = new ArrayList<UserDto>();
        doAnswer(invocation -> {
            final var user = invocation.getArgument(0, UserDto.class);
            importedUsers.add(user);
            return user;
        }).when(userService).storeUser(any(UserDto.class));

        final var jsonUrl = importUrl("data-users-optional-handle.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importUsers(userService);

            verify(userService, times(3)).storeUser(any(UserDto.class));
            assertThat(importedUsers).hasSize(3);
            assertThat(importedUsers).allSatisfy(user -> assertThat(user.handle()).isNull());
            assertThat(logCaptor.getInfoLogs()).contains("...finished importing 3 users.");
            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportCommunities() {
        final var communityService = mock(CommunityService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(communityService)
                .storeCommunity(argThat(community -> UUID_ZERO.equals(community.id())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importCommunities(communityService);
            verify(communityService, times(7)).storeCommunity(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing communities...",
                    "...finished importing 6 communities.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping community '{\"communityId\":\"00000000-0000-0000-0000-000000000000\",\"handle\":\"demoCommunity1\",\"name\":\"Duplicate Handle Community\",\"description\":\"A community with duplicate handle for integration tests.\",\"imageId\":\"\"}': Simulated failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportEvents() {
        final var eventStore = new ArrayList<EventDto>();
        final var eventService = mock(EventService.class);
        doAnswer(invocation -> {
            final var event = invocation.getArgument(0, EventDto.class);
            eventStore.add(event);
            return event;
        }).when(eventService).storeEvent(any(EventDto.class));
        doThrow(new RuntimeException("Simulated failure"))
                .when(eventService)
                .storeEvent(argThat(event -> UUID_ZERO.equals(event.id())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importEvents(eventService);
            verify(eventService, times(7)).storeEvent(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing events...",
                    "...finished importing 6 events.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping event '{\"eventId\":\"00000000-0000-0000-0000-000000000000\",\"communityId\":\"42524267-be7e-404b-a9bf-c3da72b97717\",\"title\":\"Event with invalid community\",\"description\":\"Event with a community that does not exist for integration tests.\",\"location\":\"Somewhere\",\"begin\":\"2030-01-07T18:00:00+02:00[Europe/Zurich]\",\"end\":\"2030-01-07T20:00:00+02:00[Europe/Zurich]\",\"imageId\":\"\",\"visibility\":\"PRIVATE\",\"status\":\"DRAFT\"}': Simulated failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();

            // test correct handling of 'anonymousParticipationAllowed'
            final var demoEvent1 = eventStore.get(0);
            assertThat(demoEvent1.anonymousParticipationAllowed()).isTrue();
            final var demoEvent2 = eventStore.get(4);
            assertThat(demoEvent2.anonymousParticipationAllowed()).isTrue();
            final var demoEvent3 = eventStore.get(5);
            assertThat(demoEvent3.anonymousParticipationAllowed()).isFalse();
        }
    }

    @Test
    void testImportMembers() {
        final var memberService = mock(MemberService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(memberService)
                .storeMember(argThat(member -> UUID_ZERO.equals(member.userId())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importMembers(memberService);
            verify(memberService, times(25)).storeMember(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing members...",
                    "...finished importing 24 members.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping member '{\"userId\":\"00000000-0000-0000-0000-000000000000\",\"communityId\":\"00000000-0000-0000-0000-000000000000\",\"role\":\"MEMBER\",\"since\":\"2025-01-01T10:00:00+02:00[Europe/Zurich]\"}': Simulated failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportParticipants() {
        final var participantService = mock(ParticipantService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(participantService)
                .storeParticipant(argThat(participant -> UUID_ZERO.equals(participant.eventId())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importParticipants(participantService);
            verify(participantService, times(7)).storeParticipant(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing participants...",
                    "...finished importing 6 participants.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping participant '{\"eventId\":\"00000000-0000-0000-0000-000000000000\",\"userId\":\"c9fc8b0a-6ff7-4c00-a6f2-d85f5829edff\",\"registered\":\"2029-12-07T12:00:00+02:00[Europe/Zurich]\"}': Simulated failure");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportGlobalPages() {
        final var globalPageService = mock(GlobalPageService.class);
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importGlobalPages(globalPageService);
            verify(globalPageService, times(2)).storeGlobalPage(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing global pages...",
                    "...finished importing 2 global pages.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping global page '{\"slot\":\"invalid\",\"language\":null,\"title\":\"Page with invalid language\",\"markdown\":\"This page has an invalid language code for integration tests.\"}': Language must not be null",
                    "Skipping global page '{\"slot\":\"invalid\",\"title\":\"Page with missing language\",\"markdown\":\"This page has a missing language code for integration tests.\"}': Language must be set");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testImportMailTemplates() {
        final var mailService = mock(MailService.class);
        doThrow(new RuntimeException("Simulated failure"))
                .when(mailService)
                .storeMailTemplate(argThat(template -> "TEST".equals(template.id().name())));
        final var jsonUrl = importUrl("data.json");
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            final var importer = new JSONImporter(new ImporterLog(null), jsonUrl);
            importer.importMailTemplates(mailService);
            verify(mailService, times(4)).storeMailTemplate(any());
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    IDENTIFIED_COUNTS_MESSAGE,
                    "Start importing mail templates...",
                    "...finished importing 2 mail templates.");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "Skipping mail template '{\"mailTemplateId\":\"TEST\",\"language\":\"en\",\"subject\":\"Test mail\",\"markdown\":\"Hello,\\n\\nthis is a test mail from ${instanceName}.\"}': Simulated failure", // TEST EN
                    "Skipping mail template '{\"mailTemplateId\":\"TEST\",\"language\":\"de\",\"subject\":\"Testmail\",\"markdown\":\"Hallo,\\n\\ndies ist eine Testmail von ${instanceName}.\"}': Simulated failure", // TEST DE
                    "Skipping mail template '{\"mailTemplateId\":\"INVALID_TEMPLATE\",\"language\":\"en\",\"subject\":\"This should fail\",\"markdown\":\"This template has an invalid ID\"}': No enum constant app.komunumo.domain.core.mail.entity.MailTemplateId.INVALID_TEMPLATE");
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    @Test
    void testNoData() {
        final var jsonUrl = importUrl("no-data.json");
        final var expectedMessage = "Identified 0 settings, 0 images, 0 users, 0 communities, 0 events, 0 members, 0 participants, 0 global pages, and 0 mail templates.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            new JSONImporter(new ImporterLog(null), jsonUrl);
            assertThat(logCaptor.getInfoLogs()).containsExactly(expectedMessage);
        }
    }

    @Test
    void testNullData() {
        final var jsonUrl = importUrl("null-data.json");
        final var expectedMessage = "Identified 0 settings, 0 images, 0 users, 0 communities, 0 events, 0 members, 0 participants, 0 global pages, and 0 mail templates.";
        try (var logCaptor = LogCaptor.forClass(ImporterLog.class)) {
            new JSONImporter(new ImporterLog(null), jsonUrl);
            assertThat(logCaptor.getInfoLogs()).containsExactly(expectedMessage);
        }
    }

    private static String importUrl(final String fileName) {
        return TEST_BASE_URL + "/import/" + fileName;
    }

}
