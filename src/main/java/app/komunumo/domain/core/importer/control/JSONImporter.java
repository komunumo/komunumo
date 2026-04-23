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
import app.komunumo.util.DownloadUtil;
import app.komunumo.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("java:S1192") // Suppressing "String literals should not be duplicated" because of different contexts
public final class JSONImporter {

    private final @NotNull ImporterLog importerLog;
    private final @NotNull JsonNode root;
    private final @NotNull ObjectMapper objectMapper;

    public JSONImporter(final @NotNull ImporterLog importerLog,
                        final @NotNull String jsonDataUrl) {
        this.importerLog = importerLog;
        this.objectMapper = new ObjectMapper();
        try {
            final var json = DownloadUtil.getString(jsonDataUrl);
            this.root = objectMapper.readTree(json);
            logJSONInfo();
        } catch (IOException | URISyntaxException | KomunumoException e) {
            importerLog.error("Failed to download JSON data from URL: %s".formatted(jsonDataUrl));
            throw new KomunumoException("Failed to download JSON data from URL: %s".formatted(jsonDataUrl), e);
        }
    }

    public JSONImporter(final @NotNull ImporterLog importerLog,
                        final @NotNull File jsonDataFile) {
        this.importerLog = importerLog;
        this.objectMapper = new ObjectMapper();
        try {
            final var json = Files.readString(jsonDataFile.toPath());
            this.root = objectMapper.readTree(json);
            logJSONInfo();
        } catch (IOException e) {
            importerLog.error("Failed to download JSON data from URL: %s".formatted(jsonDataFile.getName()));
            throw new KomunumoException("Failed to load JSON data from file: %s".formatted(jsonDataFile.getName()), e);
        }
    }

    private void logJSONInfo() {
        importerLog.info("""
                Identified %d settings, %d images, %d users, %d communities, %d events, %d members, \
                %d participants, %d global pages, and %d mail templates."""
                .formatted(
                        countArrayItems("settings"),
                        countArrayItems("images"),
                        countArrayItems("users"),
                        countArrayItems("communities"),
                        countArrayItems("events"),
                        countArrayItems("members"),
                        countArrayItems("participants"),
                        countArrayItems("globalPages"),
                        countArrayItems("mailTemplates")));
    }

    private int countArrayItems(final @NotNull String arrayName) {
        final var node = root.get(arrayName);
        return node != null && node.isArray() ? node.size() : 0;
    }

    public void importSettings(final @NotNull ConfigurationService configurationService) {
        if (root.has("settings")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing settings...");
            root.get("settings").forEach(node -> {
                try {
                    final var setting = ConfigurationSetting.fromString(node.path("setting").asString());
                    final var language = node.path("language").asString(null);

                    if (setting.isLanguageDependent() && language == null) {
                        importerLog.warn("Skipping setting '%s' because it is language-dependent but no language was provided."
                                .formatted(setting.setting()));
                        return;
                    } else if (!setting.isLanguageDependent() && language != null) {
                        importerLog.warn("Skipping setting '%s' because it is not language-dependent but a language was provided."
                                .formatted(setting.setting()));
                        return;
                    }

                    final var locale = language == null ? null : Locale.forLanguageTag(language);
                    final var value = node.path("value").asString();
                    configurationService.setConfiguration(setting, locale, value);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping setting '%s': %s".formatted(node, e.getMessage()));
                }
            });
            configurationService.clearCache();
            importerLog.info("...finished importing %d settings.".formatted(counter.get()));
        } else {
            importerLog.warn("No settings found in JSON data.");
        }
    }

    public void importUsers(final @NotNull UserService userService) {
        if (root.has("users")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing users...");
            root.get("users").forEach(node -> {
                try {
                    final var userId = UUID.fromString(node.path("userId").asString());
                    final var handle = getHandle(node);
                    final var email = node.path("email").asString().trim();
                    final var name = node.path("name").asString().trim();
                    final var bio = node.path("bio").asString().trim();
                    final var imageId = parseUUID(node.path("imageId").asString());
                    final var role = UserRole.valueOf(node.path("role").asString().trim());
                    final var type = UserType.valueOf(node.path("type").asString().trim());

                    final var user = new UserDto(userId, null, null, handle, email, name, bio, imageId,
                            role, type);
                    userService.storeUser(user);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping user '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d users.".formatted(counter.get()));
        } else {
            importerLog.warn("No users found in JSON data.");
        }
    }

    public void importImages(final @NotNull ImageService imageService) {
        if (root.has("images")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing images...");
            root.get("images").forEach(node -> {
                try {
                    final var imageId = UUID.fromString(node.path("imageId").asString());
                    final var contentType = ContentType.fromContentType(node.path("contentType").asString());

                    final var url = node.path("url").asString();
                    final var path = DownloadUtil.downloadFile(url);

                    final var image = new ImageDto(imageId, contentType);
                    ImageUtil.storeImage(image, path);
                    imageService.storeImage(image);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping image '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d images.".formatted(counter.get()));
        } else {
            importerLog.warn("No images found in JSON data.");
        }
    }

    public void importCommunities(final @NotNull CommunityService communityService) {
        if (root.has("communities")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing communities...");
            root.get("communities").forEach(node -> {
                try {
                    final var communityId = UUID.fromString(node.path("communityId").asString());
                    final var handle = getHandle(node);
                    final var name = node.path("name").asString().trim();
                    final var description = node.path("description").asString().trim();
                    final var imageId = parseUUID(node.path("imageId").asString());

                    final var community = new CommunityDto(communityId, handle, null, null,
                            name, description, imageId);
                    communityService.storeCommunity(community);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping community '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d communities.".formatted(counter.get()));
        } else {
            importerLog.warn("No communities found in JSON data.");
        }
    }

    private @Nullable String getHandle(final @NotNull JsonNode node) {
        if (node.hasNonNull("handle")) {
            final var handle = node.path("handle").asString().trim();
            return handle.isEmpty() ? null : handle;
        }
        return null;
    }

    public void importEvents(final @NotNull EventService eventService) {
        if (root.has("events")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing events...");
            root.get("events").forEach(node -> {
                try {
                    final var eventId = UUID.fromString(node.path("eventId").asString());
                    final var communityId = UUID.fromString(node.path("communityId").asString());
                    final var title = node.path("title").asString().trim();
                    final var description = node.path("description").asString().trim();
                    final var location = node.path("location").asString().trim();
                    final var begin = parseDateTime(node.path("begin").asString());
                    final var end = parseDateTime(node.path("end").asString());
                    final var imageId = parseUUID(node.path("imageId").asString());
                    final var anonymousParticipationAllowed = parseBoolean(node, "anonymousParticipationAllowed", true);
                    final var visibility = EventVisibility.valueOf(node.path("visibility").asString());
                    final var status = EventStatus.valueOf(node.path("status").asString());

                    final var event = new EventDto(eventId, communityId, null, null, title,
                            description, location, begin, end, imageId, anonymousParticipationAllowed, visibility, status);
                    eventService.storeEvent(event);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping event '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d events.".formatted(counter.get()));
        } else {
            importerLog.warn("No events found in JSON data.");
        }
    }

    private boolean parseBoolean(final JsonNode node, final @NotNull String propertyName, final boolean defaultValue) {
        if (node.has(propertyName)) {
            return node.path(propertyName).asBoolean();
        }
        return defaultValue;
    }

    public void importParticipants(final @NotNull ParticipantService participantService) {
        if (root.has("participants")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing participants...");
            root.get("participants").forEach(node -> {
                try {
                    final var eventId = UUID.fromString(node.path("eventId").asString());
                    final var userId = UUID.fromString(node.path("userId").asString());
                    final var registeredDate = parseDateTime(node.path("registered").asString());
                    final var participant =  new ParticipantDto(eventId, userId, registeredDate);
                    participantService.storeParticipant(participant);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping participant '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d participants.".formatted(counter.get()));
        } else {
            importerLog.warn("No participants found in JSON data.");
        }
    }

    public void importMembers(final @NotNull MemberService memberService) {
        if (root.has("members")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing members...");
            root.get("members").forEach(node -> {
                try {
                    final var userId = UUID.fromString(node.path("userId").asString());
                    final var communityId = UUID.fromString(node.path("communityId").asString());
                    final var role = MemberRole.valueOf(node.path("role").asString());
                    final var since = parseDateTime(node.path("since").asString());

                    final var member = new MemberDto(userId, communityId, role, since);
                    memberService.storeMember(member);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping member '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d members.".formatted(counter.get()));
        } else {
            importerLog.warn("No members found in JSON data.");
        }
    }

    public void importGlobalPages(final @NotNull GlobalPageService globalPageService) {
        if (root.has("globalPages")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing global pages...");
            root.get("globalPages").forEach(node -> {
                try {
                    final var slot = node.path("slot").asString().trim();
                    final var languageNode = node.path("language");
                    if (languageNode.isMissingNode()) {
                        throw new NullPointerException("Language must be set");
                    }
                    if (languageNode.isNull()) {
                        throw new NullPointerException("Language must not be null");
                    }
                    final var locale = Locale.forLanguageTag(languageNode.asString());
                    final var title = node.path("title").asString().trim();
                    final var markdown = node.path("markdown").asString().trim();

                    final var globalPage = new GlobalPageDto(slot, locale, null, null, title, markdown);
                    globalPageService.storeGlobalPage(globalPage);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping global page '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d global pages.".formatted(counter.get()));
        } else {
            importerLog.warn("No global pages found in JSON data.");
        }
    }

    public void importMailTemplates(final @NotNull MailService mailService) {
        if (root.has("mailTemplates")) {
            final var counter = new AtomicInteger(0);
            importerLog.info("Start importing mail templates...");
            root.get("mailTemplates").forEach(node -> {
                try {
                    final var mailTemplateId = MailTemplateId.valueOf(node.path("mailTemplateId").asString());
                    final var language = Locale.forLanguageTag(node.path("language").asString());
                    final var subject = node.path("subject").asString().trim();
                    final var markdown = node.path("markdown").asString().trim();

                    final var mailTemplate = new MailTemplate(mailTemplateId, language, subject, markdown);

                    mailService.storeMailTemplate(mailTemplate);
                    counter.incrementAndGet();
                } catch (final Exception e) {
                    importerLog.warn("Skipping mail template '%s': %s".formatted(node, e.getMessage()));
                }
            });
            importerLog.info("...finished importing %d mail templates.".formatted(counter.get()));
        } else {
            importerLog.warn("No mail templates found in JSON data.");
        }
    }

    private static @Nullable UUID parseUUID(final @NotNull String uuidString) {
        return uuidString.isBlank() ? null : UUID.fromString(uuidString);
    }

    private static @Nullable ZonedDateTime parseDateTime(final @NotNull String dateTime) {
        return dateTime.isBlank() ? null : ZonedDateTime.parse(dateTime);
    }
}
