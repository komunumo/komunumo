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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class HttpTestServer implements LauncherSessionListener {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(HttpTestServer.class);
    public static final @NotNull String TEST_BASE_URL_PROPERTY = "komunumo.test.base-url";
    private static final @NotNull String TEST_BASE_URL_PLACEHOLDER = "{{TEST_BASE_URL}}";
    private static final @NotNull String CERT_ALIAS = "komunumo-test-server";
    private static final @NotNull String STORE_TYPE = "PKCS12";
    private static final @NotNull String STORE_PASSWORD = "komunumo-test-password";

    private static HttpServer server;
    private static @NotNull String testBaseUrl = "http://localhost";
    private static @Nullable Path tempDirectory;
    private static @Nullable Path keyStorePath;
    private static @Nullable Path trustStorePath;
    private static @Nullable Path certificatePath;
    private static final @NotNull Map<@NotNull String, @Nullable String> previousSystemProperties = new HashMap<>();

    @Override
    public void launcherSessionOpened(final @Nullable LauncherSession session) {
        try {
            final var root = Path.of("src/test/resources").toAbsolutePath().normalize();
            setupHttps(root);
            testBaseUrl = "https://localhost:%d".formatted(server.getAddress().getPort());
            System.setProperty(TEST_BASE_URL_PROPERTY, testBaseUrl);
            LOGGER.info("[HTTPS Server] Started on {} serving {}", testBaseUrl, root);
        } catch (final @NotNull  IOException e) {
            throw new RuntimeException("Failed to start HTTPS server", e);
        }
    }

    @Override
    public void launcherSessionClosed(final @Nullable LauncherSession session) {
        if (server != null) {
            server.stop(0);
            LOGGER.info("[HTTPS Server] Stopped");
        }
        System.clearProperty(TEST_BASE_URL_PROPERTY);
        restoreSystemProperty("javax.net.ssl.trustStore");
        restoreSystemProperty("javax.net.ssl.trustStorePassword");
        restoreSystemProperty("javax.net.ssl.trustStoreType");
        deleteTempFile(certificatePath);
        deleteTempFile(keyStorePath);
        deleteTempFile(trustStorePath);
        deleteTempDirectory(tempDirectory);
    }

    private static void handleRequest(final @NotNull HttpExchange exchange,
                                      final @NotNull Path root) throws IOException {
        final var uriPath = exchange.getRequestURI().getPath();
        final var file = root.resolve("." + uriPath).normalize();

        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        final var contentType = guessContentType(file);
        final var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);

        final byte[] body;
        if ("application/json".equals(contentType)) {
            final var json = Files.readString(file).replace(TEST_BASE_URL_PLACEHOLDER, testBaseUrl);
            body = json.getBytes(StandardCharsets.UTF_8);
        } else {
            body = Files.readAllBytes(file);
        }
        exchange.sendResponseHeaders(200, body.length);

        try (final OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void setupHttps(final @NotNull Path root) throws IOException {
        tempDirectory = Files.createTempDirectory("komunumo-http-test-server-");
        keyStorePath = tempDirectory.resolve("keystore.p12");
        trustStorePath = tempDirectory.resolve("truststore.p12");
        certificatePath = tempDirectory.resolve("certificate.pem");

        generateCertificateAndStores();
        final var sslContext = createServerSslContext();

        final var httpsServer = HttpsServer.create(new InetSocketAddress(0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(final HttpsParameters parameters) {
                parameters.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });
        httpsServer.createContext("/", exchange -> handleRequest(exchange, root));
        httpsServer.setExecutor(null);
        httpsServer.start();
        server = httpsServer;

        setSystemPropertyWithBackup("javax.net.ssl.trustStore", Objects.requireNonNull(trustStorePath).toString());
        setSystemPropertyWithBackup("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
        setSystemPropertyWithBackup("javax.net.ssl.trustStoreType", STORE_TYPE);
    }

    private static void generateCertificateAndStores() throws IOException {
        final var keyStore = Objects.requireNonNull(keyStorePath);
        final var trustStore = Objects.requireNonNull(trustStorePath);
        final var certificate = Objects.requireNonNull(certificatePath);

        runKeytool(List.of(
                "-genkeypair",
                "-alias", CERT_ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "3650",
                "-dname", "CN=localhost, OU=Tests, O=Komunumo, L=Test, ST=Test, C=CH",
                "-ext", "SAN=dns:localhost",
                "-storetype", STORE_TYPE,
                "-keystore", keyStore.toString(),
                "-storepass", STORE_PASSWORD,
                "-keypass", STORE_PASSWORD,
                "-noprompt"));

        runKeytool(List.of(
                "-exportcert",
                "-rfc",
                "-alias", CERT_ALIAS,
                "-keystore", keyStore.toString(),
                "-storepass", STORE_PASSWORD,
                "-file", certificate.toString()));

        runKeytool(List.of(
                "-importcert",
                "-alias", CERT_ALIAS,
                "-file", certificate.toString(),
                "-keystore", trustStore.toString(),
                "-storetype", STORE_TYPE,
                "-storepass", STORE_PASSWORD,
                "-noprompt"));
    }

    private static @NotNull SSLContext createServerSslContext() throws IOException {
        try (final var keyStoreStream = Files.newInputStream(Objects.requireNonNull(keyStorePath))) {
            final var keyStore = KeyStore.getInstance(STORE_TYPE);
            keyStore.load(keyStoreStream, STORE_PASSWORD.toCharArray());

            final var keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD.toCharArray());

            final var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
            return sslContext;
        } catch (final Exception e) {
            throw new IOException("Unable to create SSL context for test HTTPS server.", e);
        }
    }

    private static void runKeytool(final @NotNull List<@NotNull String> arguments) throws IOException {
        final var javaHome = System.getProperty("java.home");
        final var keytool = Path.of(javaHome, "bin", "keytool").toString();
        final var command = new java.util.ArrayList<String>();
        command.add(keytool);
        command.addAll(arguments);

        final var process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        final var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while executing keytool.", e);
        }
        if (exitCode != 0) {
            throw new IOException("keytool command failed (%d): %s".formatted(exitCode, output));
        }
    }

    private static void setSystemPropertyWithBackup(final @NotNull String key,
                                                    final @NotNull String value) {
        if (!previousSystemProperties.containsKey(key)) {
            previousSystemProperties.put(key, System.getProperty(key));
        }
        System.setProperty(key, value);
    }

    private static void restoreSystemProperty(final @NotNull String key) {
        final var previousValue = previousSystemProperties.remove(key);
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    private static void deleteTempFile(final @Nullable Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            LOGGER.warn("Failed to delete temporary file '{}': {}", path, e.getMessage());
        }
    }

    private static void deleteTempDirectory(final @Nullable Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory);
        } catch (final IOException e) {
            LOGGER.warn("Failed to delete temporary directory '{}': {}", directory, e.getMessage());
        }
    }

    private static String guessContentType(final @NotNull Path path) {
        try {
            final var probe = Files.probeContentType(path);
            if (probe != null) {
                return probe;
            }
        } catch (final IOException ignored) {
            // if probeContentType fails, we fall back to manual guessing
        }

        final var name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
