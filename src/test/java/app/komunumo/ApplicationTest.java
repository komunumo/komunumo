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
package app.komunumo;

import app.komunumo.infra.config.AppConfig;
import app.komunumo.infra.config.FilesConfig;
import com.vaadin.flow.server.AppShellSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationTest {

    @Test
    void testMainCallsSpring() {
        final String[] args = new String[2];
        args[0] = "foo";
        args[1] = "bar";

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(Application.class, args))
                    .thenReturn(null);
            springApplication.verify(() -> SpringApplication.run(Application.class, args),
                    times(0));
            Application.main(args);
            springApplication.verify(() -> SpringApplication.run(Application.class, args),
                    times(1));
        }
    }

    @Test
    void configurePage_addsDefaultFavicons() {
        final var filesConfig = mock(FilesConfig.class);
        when(filesConfig.basedir()).thenReturn(Path.of(""));
        final var appConfig = mock(AppConfig.class);
        when(appConfig.files()).thenReturn(filesConfig);

        final var app = new Application(appConfig);
        final var settings = mock(AppShellSettings.class);

        app.configurePage(settings);

        verify(settings).addFavIcon("icon", "icons/icon.png", "1024x1024");
        verify(settings).addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        verify(settings).addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        verify(settings).addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        verify(settings).addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        verify(settings).addFavIcon("icon", "icons/favicon-16x16.png", "16x16");

        verify(settings).addLink("shortcut icon", "icons/favicon.ico");

        verifyNoMoreInteractions(settings);
    }

    @Test
    void configurePage_addsCustomStyleSheetWhenPresent(final @TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("custom").resolve("styles"));
        Files.createFile(tempDir.resolve("custom").resolve("styles").resolve("styles.css"));

        final var app = createApplication(tempDir);
        final var settings = mock(AppShellSettings.class);

        app.configurePage(settings);

        verify(settings).addLink("stylesheet", "/custom/styles/styles.css");
        verify(settings).addFavIcon("icon", "icons/icon.png", "1024x1024");
        verify(settings).addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        verify(settings).addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        verify(settings).addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        verify(settings).addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        verify(settings).addFavIcon("icon", "icons/favicon-16x16.png", "16x16");
        verify(settings).addLink("shortcut icon", "icons/favicon.ico");
        verifyNoMoreInteractions(settings);
    }

    @Test
    void configurePage_addsCustomFaviconsInsteadOfDefaults(final @TempDir Path tempDir) throws IOException {
        final var faviconDir = Files.createDirectories(tempDir.resolve("custom").resolve("favicon"));
        Files.createFile(faviconDir.resolve("favicon.ico"));
        Files.createFile(faviconDir.resolve("favicon-32x32.png"));
        Files.createFile(faviconDir.resolve("favicon-180x180.svg"));
        Files.createFile(faviconDir.resolve("ignored.txt"));

        final var app = createApplication(tempDir);
        final var settings = mock(AppShellSettings.class);

        app.configurePage(settings);

        verify(settings).addLink("shortcut icon", "/custom/favicon/favicon.ico");
        verify(settings).addFavIcon("icon", "/custom/favicon/favicon-32x32.png", "32x32");
        verify(settings).addFavIcon("icon", "/custom/favicon/favicon-180x180.svg", "180x180");
        verify(settings, never()).addFavIcon("icon", "icons/icon.png", "1024x1024");
        verify(settings, never()).addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        verify(settings, never()).addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        verify(settings, never()).addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        verify(settings, never()).addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        verify(settings, never()).addFavIcon("icon", "icons/favicon-16x16.png", "16x16");
        verify(settings, never()).addLink("shortcut icon", "icons/favicon.ico");
        verify(settings, never()).addLink("stylesheet", "/custom/styles/styles.css");
        verifyNoMoreInteractions(settings);
    }

    @Test
    void configurePage_fallsBackToDefaultsWhenCustomFaviconDirectoryContainsNoValidIcons(
            final @TempDir Path tempDir) throws IOException {
        final var faviconDir = Files.createDirectories(tempDir.resolve("custom").resolve("favicon"));
        Files.createFile(faviconDir.resolve("invalid-name.png"));

        final var app = createApplication(tempDir);
        final var settings = mock(AppShellSettings.class);

        app.configurePage(settings);

        verify(settings).addFavIcon("icon", "icons/icon.png", "1024x1024");
        verify(settings).addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        verify(settings).addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        verify(settings).addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        verify(settings).addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        verify(settings).addFavIcon("icon", "icons/favicon-16x16.png", "16x16");
        verify(settings).addLink("shortcut icon", "icons/favicon.ico");
        verifyNoMoreInteractions(settings);
    }

    @Test
    void configurePage_fallsBackToDefaultsWhenReadingCustomFaviconDirectoryFails(final @TempDir Path tempDir) {
        final var app = createApplication(tempDir);
        final var settings = mock(AppShellSettings.class);
        final var faviconDir = tempDir.resolve("custom").resolve("favicon");

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.isDirectory(faviconDir)).thenReturn(true);
            files.when(() -> Files.list(faviconDir)).thenThrow(new IOException("boom"));

            app.configurePage(settings);
        }

        verify(settings).addFavIcon("icon", "icons/icon.png", "1024x1024");
        verify(settings).addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        verify(settings).addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        verify(settings).addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        verify(settings).addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        verify(settings).addFavIcon("icon", "icons/favicon-16x16.png", "16x16");
        verify(settings).addLink("shortcut icon", "icons/favicon.ico");
        verifyNoMoreInteractions(settings);
    }

    private Application createApplication(final Path baseDir) {
        final var filesConfig = mock(FilesConfig.class);
        when(filesConfig.basedir()).thenReturn(baseDir);
        final var appConfig = mock(AppConfig.class);
        when(appConfig.files()).thenReturn(filesConfig);
        return new Application(appConfig);
    }
}
