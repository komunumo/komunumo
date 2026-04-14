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
package app.komunumo.domain.core.exporter.boundary;

import app.komunumo.KomunumoException;
import app.komunumo.domain.community.control.CommunityService;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.exporter.control.JSONExporter;
import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.core.mail.control.MailService;
import app.komunumo.domain.event.control.EventService;
import app.komunumo.domain.member.control.MemberService;
import app.komunumo.domain.page.control.GlobalPageService;
import app.komunumo.domain.participant.control.ParticipantService;
import app.komunumo.domain.user.control.UserService;
import app.komunumo.infra.i18n.TranslationProvider;
import app.komunumo.infra.ui.vaadin.layout.AbstractView;
import app.komunumo.infra.ui.vaadin.layout.WebsiteLayout;
import app.komunumo.util.NotificationUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RolesAllowed("ADMIN")
@Route(value = "admin/export", layout = WebsiteLayout.class)
public final class ExporterView extends AbstractView {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssXXX");

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ImageService imageService;
    private final @NotNull UserService userService;
    private final @NotNull CommunityService communityService;
    private final @NotNull MemberService memberService;
    private final @NotNull EventService eventService;
    private final @NotNull ParticipantService participantService;
    private final @NotNull GlobalPageService globalPageService;
    private final @NotNull MailService mailService;
    private final @NotNull TranslationProvider translationProvider;

    private final @NotNull UI ui;
    private final @NotNull UnorderedList exportLog;
    private final @NotNull VerticalLayout exportFieldsContainer;
    private final @NotNull VerticalLayout exportLogContainer;

    @SuppressWarnings("checkstyle:ParameterNumber") // constructor injection
    public ExporterView(final @NotNull ConfigurationService configurationService,
                        final @NotNull ImageService imageService,
                        final @NotNull UserService userService,
                        final @NotNull CommunityService communityService,
                        final @NotNull MemberService memberService,
                        final @NotNull EventService eventService,
                        final @NotNull ParticipantService participantService,
                        final @NotNull GlobalPageService globalPageService,
                        final @NotNull MailService mailService,
                        final @NotNull TranslationProvider translationProvider) {
        super(configurationService);

        this.configurationService = configurationService;
        this.imageService = imageService;
        this.userService = userService;
        this.communityService = communityService;
        this.memberService = memberService;
        this.eventService = eventService;
        this.participantService = participantService;
        this.globalPageService = globalPageService;
        this.mailService = mailService;
        this.translationProvider = translationProvider;
        this.ui = UI.getCurrent();

        final var exportButton = new Button(getTranslation("core.exporter.boundary.ExporterView.startExportButton"));
        exportButton.setEnabled(true);
        exportButton.addClassName("start-export-button");
        exportButton.addClickListener(_ -> processExport());

        final var exportLogTitle = new H3(getTranslation("core.exporter.boundary.ExporterView.exportLogTitle"));
        exportLog = new UnorderedList();
        exportLog.addClassName("export-log");

        addClassName("exporter-view");
        add(new H2(getTranslation("core.exporter.boundary.ExporterView.title")));

        exportFieldsContainer = new VerticalLayout();
        exportFieldsContainer.setId("export-fields-container");
        exportFieldsContainer.add(exportButton);
        add(exportFieldsContainer);

        exportLogContainer = new VerticalLayout();
        exportLogContainer.setId("export-log-container");
        exportLogContainer.add(exportLogTitle);
        exportLogContainer.add(exportLog);
        exportLogContainer.setVisible(false);
        add(exportLogContainer);
    }

    private void processExport() {
        exportFieldsContainer.setEnabled(false);
        exportLogContainer.setVisible(true);
        exportLog.add(new ListItem(getTranslation("core.exporter.boundary.ExporterView.exportStarted")));
        ui.access(() -> {
            try {
                final var jsonExporter = new JSONExporter();
                final String json = jsonExporter.exportAll(
                        configurationService,
                        imageService,
                        userService,
                        communityService,
                        memberService,
                        eventService,
                        participantService,
                        globalPageService,
                        mailService,
                        translationProvider
                );

                final String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FORMAT);
                final String fileName = "komunumo-export-" + timestamp + ".json";

                final var downloadLink = new Anchor(
                        getDownloadHandler(json, fileName),
                        getTranslation("core.exporter.boundary.ExporterView.downloadLink"));

                downloadLink.addClassName("export-download-link");
                exportFieldsContainer.add(downloadLink);

                exportLog.add(new ListItem(getTranslation("core.exporter.boundary.ExporterView.exportSuccess")));
            } catch (final KomunumoException e) {
                exportLog.add(new ListItem(getTranslation("core.exporter.boundary.ExporterView.exportFailed")
                        + ": " + e.getMessage()));
                NotificationUtil.showNotification(
                        getTranslation("core.exporter.boundary.ExporterView.exportFailed"),
                        NotificationVariant.LUMO_ERROR);
            } finally {
                exportFieldsContainer.setEnabled(true);
            }
        });
    }

    private static @NonNull InputStreamDownloadHandler getDownloadHandler(
            final @NotNull String json, final @NotNull String fileName) {
        return DownloadHandler.fromInputStream(_ -> new DownloadResponse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                fileName,
                "application/json",
                -1));
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("core.exporter.boundary.ExporterView.title");
    }

}
