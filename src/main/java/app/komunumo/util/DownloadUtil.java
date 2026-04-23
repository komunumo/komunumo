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
package app.komunumo.util;

import app.komunumo.KomunumoException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

public final class DownloadUtil {

    @VisibleForTesting
    static final int CONNECT_TIMEOUT_SECONDS = 5;
    @VisibleForTesting
    static final int REQUEST_TIMEOUT_SECONDS = 30;
    @VisibleForTesting
    static final long MAX_DOWNLOAD_SIZE_IN_BYTES = 100L * 1024L * 1024L;

    public static @NotNull String getString(final @NotNull String location)
            throws IOException, URISyntaxException {
        final var path = downloadFile(location);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @SuppressWarnings({"java:S2095", "java:S2142", "LoggingSimilarMessage"})
    public static @NotNull Path downloadFile(final @NotNull String location) {
        return downloadFile(location,
                Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS),
                Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS),
                MAX_DOWNLOAD_SIZE_IN_BYTES);
    }

    @SuppressWarnings({"java:S2095", "java:S2142", "LoggingSimilarMessage"})
    public static @NotNull Path downloadFile(final @NotNull String location,
                                             final @NotNull Duration connectTimeout,
                                             final @NotNull Duration requestTimeout,
                                             final long maxDownloadSizeInBytes) {
        try {
            final var tempFile = Files.createTempFile("download-", ".tmp");
            tempFile.toFile().deleteOnExit();

            if (location.startsWith("data:")) {
                final var commaIndex = location.indexOf(',');
                if (commaIndex < 0) {
                    Files.deleteIfExists(tempFile);
                    throw new KomunumoException("Invalid data URL: " + location);
                }

                final var metadata = location.substring(5, commaIndex); // skip "data:"
                final var dataPart = location.substring(commaIndex + 1);
                final var isBase64 = metadata.contains(";base64");

                try (var inputStream = isBase64
                        ? Base64.getDecoder().wrap(new ByteArrayInputStream(dataPart.getBytes(StandardCharsets.US_ASCII)))
                        : new ByteArrayInputStream(
                                URLDecoder.decode(dataPart, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8))) {
                    copyToFileWithSizeLimit(inputStream, tempFile, maxDownloadSizeInBytes, location);
                }
                return tempFile;
            }

            if (location.startsWith("https://")) {
                try (var client = HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build()) {
                    final var request = HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .timeout(requestTimeout)
                            .GET()
                            .build();
                    final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    final var statusCode = response.statusCode();
                    if (statusCode == 200) {
                        final var contentLength = response.headers()
                                .firstValueAsLong("Content-Length")
                                .orElse(-1L);
                        if (contentLength > maxDownloadSizeInBytes) {
                            Files.deleteIfExists(tempFile);
                            throw new KomunumoException("Download exceeds maximum allowed size of %s bytes: %s"
                                    .formatted(maxDownloadSizeInBytes, location));
                        }
                        try (var responseBody = response.body()) {
                            copyToFileWithSizeLimit(responseBody, tempFile, maxDownloadSizeInBytes, location);
                        }
                        return tempFile;
                    } else {
                        Files.deleteIfExists(tempFile);
                        throw new KomunumoException("Failed to download file from '%s': HTTP status code %s"
                                .formatted(location, statusCode));
                    }
                }
            }
        } catch (final IOException | InterruptedException e) {
            throw new KomunumoException("Failed to download file from '%s': %s"
                    .formatted(location, e.getMessage()), e);
        }

        throw new KomunumoException("Unsupported URL: " + location);
    }

    @VisibleForTesting
    static void copyToFileWithSizeLimit(final @NotNull InputStream inputStream,
                                        final @NotNull Path targetFile,
                                        final long maxSizeInBytes,
                                        final @NotNull String location) throws IOException {
        long totalBytes = 0L;
        final byte[] buffer = new byte[8192];

        try (var outputStream = Files.newOutputStream(targetFile)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxSizeInBytes) {
                    throw new KomunumoException("Download exceeds maximum allowed size of %s bytes: %s"
                            .formatted(maxSizeInBytes, location));
                }
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (final RuntimeException | IOException e) {
            Files.deleteIfExists(targetFile);
            throw e;
        }
    }

    private DownloadUtil() {
        throw new IllegalStateException("Utility class");
    }

}
