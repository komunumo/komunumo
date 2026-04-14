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

import app.komunumo.KomunumoException;
import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.util.Base64;

import static app.komunumo.util.LocaleUtil.getLanguageCode;

/**
 * <p>Exports all Komunumo instance data as a JSON file.</p>
 *
 * <p>This exporter creates a JSON structure compatible with the {@code JSONImporter},
 * allowing backup and transfer of instance configurations and content.</p>
 */
public final class JSONExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONExporter.class);

    private final @NotNull ObjectMapper objectMapper;

    public JSONExporter() {
        this(new ObjectMapper());
    }

    public JSONExporter(final @NotNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * <p>Exports all instance data as a JSON string.</p>
     *
     * @param configurationService service for configuration data
     * @param imageService service for image data
     * @param userService service for user data
     * @param communityService service for community data
     * @param memberService service for member data
     * @param eventService service for event data
     * @param participantService service for participant data
     * @param globalPageService service for global page data
     * @param mailService service for mail template data
     * @return a pretty-printed JSON string containing all exported data
     */
    @SuppressWarnings({"java:S107", "checkstyle:ParameterNumber"}) // Number of parameters is justified for complete export
    public String exportAll(
            final @NotNull ConfigurationService configurationService,
            final @NotNull ImageService imageService,
            final @NotNull UserService userService,
            final @NotNull CommunityService communityService,
            final @NotNull MemberService memberService,
            final @NotNull EventService eventService,
            final @NotNull ParticipantService participantService,
            final @NotNull GlobalPageService globalPageService,
            final @NotNull MailService mailService,
            final @NotNull TranslationProvider translationProvider
            ) {
        try {
            final var root = objectMapper.createObjectNode();

            exportSettings(root, configurationService, translationProvider);
            exportImages(root, imageService);
            exportUsers(root, userService);
            exportCommunities(root, communityService);
            exportEvents(root, eventService);
            exportMembers(root, memberService);
            exportParticipants(root, participantService);
            exportGlobalPages(root, globalPageService);
            exportMailTemplates(root, mailService);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (final Exception e) {
            throw new KomunumoException(e.getMessage(), e);
        }
    }

    private void exportSettings(final @NotNull ObjectNode root,
                                final @NotNull ConfigurationService configurationService,
                                final @NotNull TranslationProvider translationProvider) {
        final var settingsArray = objectMapper.createArrayNode();

        for (final var configurationSetting : ConfigurationSetting.values()) {
            if (configurationSetting.isLanguageDependent()) {
                for (final var locale : translationProvider.getProvidedLocales()) {
                    final var defaultValue = configurationSetting.defaultValue();
                    final var actualValue = configurationService.getConfigurationWithoutFallback(configurationSetting, locale);
                    final var isDefaultValue = defaultValue.equals(actualValue);

                    if (isDefaultValue) {
                        continue; // skip default values to reduce export size
                    }

                    final var node = objectMapper.createObjectNode();
                    node.put("setting", configurationSetting.setting());
                    node.put("language", getLanguageCode(locale));
                    node.put("value", actualValue);
                    settingsArray.add(node);
                }
            } else {
                final var defaultValue = configurationSetting.defaultValue();
                final var actualValue = configurationService.getConfigurationWithoutFallback(configurationSetting, null);
                final var isDefaultValue = defaultValue.equals(actualValue);

                if (isDefaultValue) {
                    continue; // skip default values to reduce export size
                }

                final var node = objectMapper.createObjectNode();
                node.put("setting", configurationSetting.setting());
                node.put("value", actualValue);
                settingsArray.add(node);
            }
        }

        root.set("settings", settingsArray);
    }

    private void exportImages(final @NotNull ObjectNode root,
                              final @NotNull ImageService imageService) {
        final var imagesArray = objectMapper.createArrayNode();
        imageService.getAllImages().forEach(image -> {
            final var imagePath = ImageUtil.resolveImagePath(image);
            //noinspection DataFlowIssue // imagePath is never null because image is never null because it comes from db
            if (!Files.exists(imagePath)) {
                LOGGER.warn("Image not found: {}", imagePath);
            } else {
                try {
                    final var imageData = Files.readAllBytes(imagePath);
                    final var node = objectMapper.createObjectNode();
                    //noinspection DataFlowIssue // image ID is never null because it comes from db
                    node.put("imageId", image.id().toString());
                    node.put("contentType", image.contentType().getContentType());
                    node.put("data", Base64.getEncoder().encodeToString(imageData));
                    imagesArray.add(node);
                } catch (final Exception exception) {
                    LOGGER.warn("Failed to read image file '{}': {}",
                            imagePath.toAbsolutePath(), exception.getMessage(), exception);
                }
            }
        });
        root.set("images", imagesArray);
    }

    private void exportUsers(final @NotNull ObjectNode root,
                             final @NotNull UserService userService) {
        final var usersArray = objectMapper.createArrayNode();
        userService.getAllUsers().forEach(user -> {
            final var node = objectMapper.createObjectNode();
            //noinspection DataFlowIssue // user ID is never null because it comes from db
            node.put("userId", user.id().toString());
            node.put("profile", user.profile());
            node.put("email", user.email());
            node.put("name", user.name());
            node.put("bio", user.bio());
            node.put("imageId", user.imageId() != null ? user.imageId().toString() : null);
            node.put("role", user.role().name());
            node.put("type", user.type().name());
            usersArray.add(node);
        });
        root.set("users", usersArray);
    }

    private void exportCommunities(final @NotNull ObjectNode root,
                                   final @NotNull CommunityService communityService) {
        final var communitiesArray = objectMapper.createArrayNode();
        communityService.getCommunities().forEach(community -> {
            final var node = objectMapper.createObjectNode();
            //noinspection DataFlowIssue // community ID is never null because it comes from db
            node.put("communityId", community.id().toString());
            node.put("profile", community.profile());
            node.put("name", community.name());
            node.put("description", community.description());
            node.put("imageId", community.imageId() != null ? community.imageId().toString() : "");
            communitiesArray.add(node);
        });
        root.set("communities", communitiesArray);
    }

    private void exportEvents(final @NotNull ObjectNode root,
                              final @NotNull EventService eventService) {
        final var eventsArray = objectMapper.createArrayNode();
        eventService.getEvents().forEach(event -> {
            final var node = objectMapper.createObjectNode();
            //noinspection DataFlowIssue // event ID is never null because it comes from db
            node.put("eventId", event.id().toString());
            //noinspection DataFlowIssue // community ID is never null because it comes from db
            node.put("communityId", event.communityId().toString());
            node.put("title", event.title());
            node.put("description", event.description());
            node.put("location", event.location());
            node.put("begin", event.begin() != null ? event.begin().toString() : "");
            node.put("end", event.end() != null ? event.end().toString() : "");
            node.put("imageId", event.imageId() != null ? event.imageId().toString() : "");
            node.put("anonymousParticipationAllowed", event.anonymousParticipationAllowed());
            node.put("visibility", event.visibility().name());
            node.put("status", event.status().name());
            eventsArray.add(node);
        });
        root.set("events", eventsArray);
    }

    private void exportMembers(final @NotNull ObjectNode root,
                               final @NotNull MemberService memberService) {
        final var membersArray = objectMapper.createArrayNode();
        memberService.getMembers().forEach(member -> {
            final var node = objectMapper.createObjectNode();
            node.put("userId", member.userId().toString());
            node.put("communityId", member.communityId().toString());
            node.put("role", member.role().name());
            //noinspection DataFlowIssue // since date is never null because it comes from db
            node.put("since", member.since().toString());
            membersArray.add(node);
        });
        root.set("members", membersArray);
    }

    private void exportParticipants(final @NotNull ObjectNode root,
                                    final @NotNull ParticipantService participantService) {
        final var participantsArray = objectMapper.createArrayNode();
        participantService.getAllParticipants().forEach(participant -> {
            final var node = objectMapper.createObjectNode();
            node.put("eventId", participant.eventId().toString());
            node.put("userId", participant.userId().toString());
            //noinspection DataFlowIssue // registered date is never null because it comes from db
            node.put("registered", participant.registered().toString());
            participantsArray.add(node);
        });
        root.set("participants", participantsArray);
    }

    private void exportGlobalPages(final @NotNull ObjectNode root,
                                   final @NotNull GlobalPageService globalPageService) {
        final var globalPagesArray = objectMapper.createArrayNode();
        globalPageService.getAllGlobalPages().forEach(page -> {
            final var node = objectMapper.createObjectNode();
            node.put("slot", page.slot());
            node.put("language", getLanguageCode(page.language()));
            node.put("title", page.title());
            node.put("markdown", page.markdown());
            globalPagesArray.add(node);
        });
        root.set("globalPages", globalPagesArray);
    }

    private void exportMailTemplates(final @NotNull ObjectNode root,
                                     final @NotNull MailService mailService) {
        final var mailTemplatesArray = objectMapper.createArrayNode();
        mailService.getAllMailTemplates().forEach(template -> {
            final var node = objectMapper.createObjectNode();
            node.put("mailTemplateId", template.id().name());
            node.put("language", template.language().toLanguageTag());
            node.put("subject", template.subject());
            node.put("markdown", template.markdown());
            mailTemplatesArray.add(node);
        });
        root.set("mailTemplates", mailTemplatesArray);
    }
}
