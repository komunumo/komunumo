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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownloadUtilTest {

    private static final String TEST_BASE_URL =
            requireNonNull(System.getProperty("komunumo.test.base-url"),
                    "Missing system property: komunumo.test.base-url");

    @Test
    void getString() throws Exception {
        final String string = DownloadUtil.getString(TEST_BASE_URL + "/custom-styles/styles.css").trim();
        assertThat(string).startsWith("body::after {").endsWith("}");
    }

    @Test
    void downloadFileSuccess() {
        final var path = DownloadUtil.downloadFile(TEST_BASE_URL + "/custom-styles/styles.css");
        assertThat(path).isNotNull().exists();
    }

    @ParameterizedTest
    @MethodSource("invalidOrUnreachableUrls")
    void shouldThrowExceptionForInvalidOrUnreachableUrls(final String url) {
        assertThatThrownBy(() -> DownloadUtil.downloadFile(url))
                .isInstanceOf(KomunumoException.class);
    }

    @Test
    void shouldThrowExceptionForUnsupportedUrlScheme() {
        final var url = "http://example.com/file.json";
        assertThatThrownBy(() -> DownloadUtil.downloadFile(url))
                .isInstanceOf(KomunumoException.class)
                .hasMessage("Unsupported URL: " + url);
    }

    @Test
    void shouldWrapIOExceptionInCatchBlockForHttpsDownload() {
        final var url = "https://nonexistent.invalid/file.json";
        assertThatThrownBy(() -> DownloadUtil.downloadFile(url))
                .isInstanceOf(KomunumoException.class)
                .hasMessage("Failed to download file from '" + url + "': null");
    }

    private static Stream<String> invalidOrUnreachableUrls() {
        return Stream.of(
                TEST_BASE_URL + "/99",
                TEST_BASE_URL + "/non-existing",
                "http://localhost:8888/",
                "data:invalid");
    }

}
