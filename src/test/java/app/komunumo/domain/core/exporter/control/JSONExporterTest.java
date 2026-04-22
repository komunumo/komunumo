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
package app.komunumo.domain.core.exporter.control;

import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.community.entity.CommunityDto;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.image.entity.ContentType;
import app.komunumo.domain.core.image.entity.ImageDto;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.core.mail.entity.MailTemplate;
import app.komunumo.domain.core.mail.entity.MailTemplateId;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.event.entity.EventDto;
import app.komunumo.domain.event.entity.EventStatus;
import app.komunumo.domain.event.entity.EventVisibility;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.member.entity.MemberDto;
import app.komunumo.domain.member.entity.MemberRole;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.page.entity.GlobalPageDto;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.participant.entity.ParticipantDto;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.domain.user.entity.UserDto;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.domain.user.entity.UserType;
import app.komunumo.infra.config.AppConfig;
import app.komunumo.infra.config.FilesConfig;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.util.ImageUtil;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class JSONExporterTest {

    private static final UUID TEST_UUID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_UUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEST_UUID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @TempDir
    private Path tempDir;

    private ConfigurationService configurationService;
    private ImageService imageService;
    private UserService userService;
    private CommunityService communityService;
    private MemberService memberService;
    private EventService eventService;
    private ParticipantService participantService;
    private GlobalPageService globalPageService;
    private MailService mailService;
    private TranslationProvider translationProvider;
    private JSONExporter exporter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        configurationService = mock(ConfigurationService.class);
        imageService = mock(ImageService.class);
        userService = mock(UserService.class);
        communityService = mock(CommunityService.class);
        memberService = mock(MemberService.class);
        eventService = mock(EventService.class);
        participantService = mock(ParticipantService.class);
        globalPageService = mock(GlobalPageService.class);
        mailService = mock(MailService.class);
        translationProvider = new TranslationProvider();
        exporter = new JSONExporter();
        objectMapper = new ObjectMapper();

        final var appConfig = mock(AppConfig.class);
        when(appConfig.files()).thenReturn(new FilesConfig(tempDir));
        ImageUtil.initialize(appConfig);
    }

    @Test
    void testExportEmptyData() {
        // given
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("settings")).isTrue();
        assertThat(root.has("images")).isTrue();
        assertThat(root.has("users")).isTrue();
        assertThat(root.has("communities")).isTrue();
        assertThat(root.has("events")).isTrue();
        assertThat(root.has("members")).isTrue();
        assertThat(root.has("participants")).isTrue();
        assertThat(root.has("globalPages")).isTrue();
        assertThat(root.has("mailTemplates")).isTrue();

        assertThat(root.get("settings")).isEmpty();
        assertThat(root.get("images")).isEmpty();
        assertThat(root.get("users")).isEmpty();
        assertThat(root.get("communities")).isEmpty();
        assertThat(root.get("events")).isEmpty();
        assertThat(root.get("members")).isEmpty();
        assertThat(root.get("participants")).isEmpty();
        assertThat(root.get("globalPages")).isEmpty();
        assertThat(root.get("mailTemplates")).isEmpty();
    }

    @Test
    void testExportSettings() {
        // given
        mockEmptyServices();
        when(configurationService.getConfigurationWithoutFallback(ConfigurationSetting.INSTANCE_NAME, null))
                .thenReturn("Test Instance");
        when(configurationService.getConfigurationWithoutFallback(ConfigurationSetting.INSTANCE_SLOGAN, Locale.ENGLISH))
                .thenReturn("English Slogan");

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode settings = root.get("settings");
        assertThat(settings).hasSize(2);
        assertThat(settings.get(0).get("setting").asString()).isEqualTo("instance.name");
        assertThat(settings.get(0).has("language")).isFalse();
        assertThat(settings.get(0).get("value").asString()).isEqualTo("Test Instance");
        assertThat(settings.get(1).get("setting").asString()).isEqualTo("instance.slogan");
        assertThat(settings.get(1).get("language").asString()).isEqualTo("en");
        assertThat(settings.get(1).get("value").asString()).isEqualTo("English Slogan");
    }

    @Test
    void testExportUsers() {
        // given
        final var user1 = new UserDto(TEST_UUID_1, null, null, "testuser1", "test1@example.com",
                "Test User 1", "Bio text", TEST_UUID_2, UserRole.USER, UserType.LOCAL);
        final var user2 = new UserDto(TEST_UUID_2, null, null, "testuser2", null,
                "Test User 2", "Bio text", TEST_UUID_1, UserRole.USER, UserType.LOCAL);
        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));
        mockEmptyServicesExceptUsers();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode users = root.get("users");
        assertThat(users).hasSize(2);
        assertThat(users.get(0).get("userId").asString()).isEqualTo(TEST_UUID_1.toString());
        assertThat(users.get(0).get("handle").asString()).isEqualTo("testuser1");
        assertThat(users.get(0).get("email").asString()).isEqualTo("test1@example.com");
        assertThat(users.get(0).get("name").asString()).isEqualTo("Test User 1");
        assertThat(users.get(0).get("role").asString()).isEqualTo("USER");
        assertThat(users.get(0).get("type").asString()).isEqualTo("LOCAL");
        assertThat(users.get(1).get("userId").asString()).isEqualTo(TEST_UUID_2.toString());
        assertThat(users.get(1).get("handle").asString()).isEqualTo("testuser2");
        assertThat(users.get(1).get("email").isNull()).isTrue();
        assertThat(users.get(1).get("name").asString()).isEqualTo("Test User 2");
        assertThat(users.get(1).get("role").asString()).isEqualTo("USER");
        assertThat(users.get(1).get("type").asString()).isEqualTo("LOCAL");
    }

    @Test
    void testExportCommunities() {
        // given
        final var community = new CommunityDto(TEST_UUID_1, "test-community", null, null,
                "Test Community", "A test community description", TEST_UUID_2);
        when(communityService.getCommunities()).thenReturn(List.of(community));
        mockEmptyServicesExceptCommunities();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode communities = root.get("communities");
        assertThat(communities).hasSize(1);
        assertThat(communities.get(0).get("communityId").asString()).isEqualTo(TEST_UUID_1.toString());
        assertThat(communities.get(0).get("handle").asString()).isEqualTo("test-community");
        assertThat(communities.get(0).get("name").asString()).isEqualTo("Test Community");
        assertThat(communities.get(0).get("description").asString()).isEqualTo("A test community description");
    }

    @Test
    void testExportEvents() {
        // given
        final var begin = ZonedDateTime.now();
        final var end = begin.plusHours(2);
        final var event = new EventDto(TEST_UUID_1, TEST_UUID_2, null, null,
                "Test Event", "Event description", "Test Location",
                begin, end, TEST_UUID_3, true, EventVisibility.PUBLIC, EventStatus.PUBLISHED);
        when(eventService.getEvents()).thenReturn(List.of(event));
        mockEmptyServicesExceptEvents();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode events = root.get("events");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("eventId").asString()).isEqualTo(TEST_UUID_1.toString());
        assertThat(events.get(0).get("communityId").asString()).isEqualTo(TEST_UUID_2.toString());
        assertThat(events.get(0).get("title").asString()).isEqualTo("Test Event");
        assertThat(events.get(0).get("visibility").asString()).isEqualTo("PUBLIC");
        assertThat(events.get(0).get("status").asString()).isEqualTo("PUBLISHED");
    }

    @Test
    void testExportMembers() {
        // given
        final var since = ZonedDateTime.now();
        final var member = new MemberDto(TEST_UUID_1, TEST_UUID_2, MemberRole.OWNER, since);
        when(memberService.getMembers()).thenReturn(List.of(member));
        mockEmptyServicesExceptMembers();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode members = root.get("members");
        assertThat(members).hasSize(1);
        assertThat(members.get(0).get("userId").asString()).isEqualTo(TEST_UUID_1.toString());
        assertThat(members.get(0).get("communityId").asString()).isEqualTo(TEST_UUID_2.toString());
        assertThat(members.get(0).get("role").asString()).isEqualTo("OWNER");
    }

    @Test
    void testExportParticipants() {
        // given
        final var registered = ZonedDateTime.now();
        final var participant = new ParticipantDto(TEST_UUID_1, TEST_UUID_2, registered);
        when(participantService.getAllParticipants()).thenReturn(List.of(participant));
        mockEmptyServicesExceptParticipants();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode participants = root.get("participants");
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).get("eventId").asString()).isEqualTo(TEST_UUID_1.toString());
        assertThat(participants.get(0).get("userId").asString()).isEqualTo(TEST_UUID_2.toString());
    }

    @Test
    void testExportGlobalPages() {
        // given
        final var page = new GlobalPageDto("about", Locale.ENGLISH, null, null, "About Us", "# About\n\nThis is about us.");
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of(page));
        mockEmptyServicesExceptGlobalPages();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode globalPages = root.get("globalPages");
        assertThat(globalPages).hasSize(1);
        assertThat(globalPages.get(0).get("slot").asString()).isEqualTo("about");
        assertThat(globalPages.get(0).get("language").asString()).isEqualTo("en");
        assertThat(globalPages.get(0).get("title").asString()).isEqualTo("About Us");
    }

    @Test
    void testExportMailTemplates() {
        // given
        final var template = new MailTemplate(MailTemplateId.CONFIRMATION_PROCESS, Locale.ENGLISH,
                "Confirm your email", "Please confirm your email address.");
        when(mailService.getAllMailTemplates()).thenReturn(List.of(template));
        mockEmptyServicesExceptMailTemplates();

        // when
        final String json = exporter.exportAll(
                configurationService, imageService, userService, communityService,
                memberService, eventService, participantService, globalPageService, mailService, translationProvider
        );

        // then
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode mailTemplates = root.get("mailTemplates");
        assertThat(mailTemplates).hasSize(1);
        assertThat(mailTemplates.get(0).get("mailTemplateId").asString()).isEqualTo("CONFIRMATION_PROCESS");
        assertThat(mailTemplates.get(0).get("language").asString()).isEqualTo("en");
        assertThat(mailTemplates.get(0).get("subject").asString()).isEqualTo("Confirm your email");
    }

    @Test
    void testExportImages() {
        // given
        final var image1 = new ImageDto(TEST_UUID_1, ContentType.IMAGE_JPEG); // ObjectMapper throws exception
        final var image2 = new ImageDto(TEST_UUID_2, ContentType.IMAGE_JPEG); // file does not exist
        final var image3 = new ImageDto(TEST_UUID_3, ContentType.IMAGE_JPEG); // successful export

        List.of(image1, image3).forEach(image -> {
            try {
                final var tmpPath = Files.createTempFile("test-", ".jpg");
                Files.writeString(tmpPath, "test", CREATE);
                ImageUtil.storeImage(image, tmpPath);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        when(imageService.getAllImages()).thenReturn(List.of(image1, image2, image3));
        mockEmptyServicesExceptImages();

        final var objectMapper = spy(new ObjectMapper());
        final var exporter = new JSONExporter(objectMapper);
        final var counter = new AtomicInteger();
        doAnswer(invocation -> {
            if (counter.getAndIncrement() == 1) {
                throw new RuntimeException("this call fails");
            }
            return invocation.callRealMethod();
        }).when(objectMapper).createObjectNode();
        // when
        try (var logCaptor = LogCaptor.forClass(JSONExporter.class)) {
            final String json = exporter.exportAll(
                    configurationService, imageService, userService, communityService,
                    memberService, eventService, participantService, globalPageService, mailService, translationProvider
            );

            // then
            final JsonNode root = objectMapper.readTree(json);
            final JsonNode images = root.get("images");
            assertThat(logCaptor.getWarnLogs()
                    .stream()
                    .filter(l -> l.startsWith("Failed to read image file"))
                    .toList())
                    .hasSize(1);
            assertThat(logCaptor.getWarnLogs()
                    .stream()
                    .filter(l -> l.startsWith("Image not found:"))
                    .toList())
                    .hasSize(1);
            assertThat(images).hasSize(1);
            assertThat(images.get(0).get("imageId").asString()).isEqualTo(TEST_UUID_3.toString());
            assertThat(images.get(0).get("contentType").asString()).isEqualTo("image/jpeg");
        }
    }

    private void mockConfigurationServiceDefaults() {
        when(configurationService.getConfigurationWithoutFallback(
                any(ConfigurationSetting.class),
                nullable(Locale.class)))
                .thenAnswer(invocation -> {
                    final ConfigurationSetting setting = invocation.getArgument(0);
                    return setting.defaultValue();
                });
    }

    private void mockEmptyServices() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptUsers() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptCommunities() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptEvents() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptMembers() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptParticipants() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptGlobalPages() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptMailTemplates() {
        mockConfigurationServiceDefaults();
        when(imageService.getAllImages()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
    }

    private void mockEmptyServicesExceptImages() {
        mockConfigurationServiceDefaults();
        when(userService.getAllUsers()).thenReturn(List.of());
        when(communityService.getCommunities()).thenReturn(List.of());
        when(eventService.getEvents()).thenReturn(List.of());
        when(memberService.getMembers()).thenReturn(List.of());
        when(participantService.getAllParticipants()).thenReturn(List.of());
        when(globalPageService.getAllGlobalPages()).thenReturn(List.of());
        when(mailService.getAllMailTemplates()).thenReturn(List.of());
    }
}
