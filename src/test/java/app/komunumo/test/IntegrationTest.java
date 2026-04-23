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
package app.komunumo.test;

import app.komunumo.Application;
import app.komunumo.domain.core.config.control.ConfigurationService;
import app.komunumo.domain.core.config.entity.ConfigurationSetting;
import app.komunumo.domain.core.demo.control.DemoMode;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * <p>Abstract base class for integration tests that run with a full Spring Boot context.</p>
 *
 * <p>It simplifies writing end-to-end or API-level integration tests by managing the
 * Spring Boot lifecycle and environment configuration.</p>
 *
 * <p>This class configures a random web environment port to avoid conflicts and excludes
 * the task scheduling autoconfiguration to prevent background tasks from interfering
 * with test execution.</p>
 */
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration")
@ActiveProfiles("test")
public abstract class IntegrationTest {

    /**
     * <p>Defines the maximum number of seconds to wait for incoming emails when using GreenMail in integration tests.
     * This timeout is applied to Awaitility-based waits that poll for messages delivered asynchronously by the
     * application under test.</p>
     */
    private static final int GREENMAIL_WAIT_TIMEOUT = 2;

    /**
     * <p>Injected service responsible for managing application configuration settings within the test environment.</p>
     *
     * <p>This service allows reading, updating, and clearing configuration values. It is used in integration tests to
     * dynamically adjust system settings such as the instance URL and to reset cached configuration data between
     * tests.</p>
     *
     * @see ConfigurationService
     */
    @Autowired
    private ConfigurationService configurationService;

    /**
     * <p>Injected Flyway instance used to manage the test database schema during integration tests.</p>
     *
     * <p>The instance is configured through the test Spring context. Ensure that Flyway clean is enabled in the
     * test profile and that the database user has sufficient DDL privileges.</p>
     *
     * @see Flyway
     */
    @Autowired
    private Flyway flyway;

    /**
     * <p>Injected helper component responsible for creating and resetting demo data used by certain integration tests.</p>
     *
     * <p>This component is typically invoked after Flyway has cleaned and migrated the schema to reinsert baseline
     * data required by demo-dependent tests.</p>
     *
     * @see DemoMode
     */
    @Autowired
    private DemoMode demoMode;

    /**
     * <p>Static instance of {@link GreenMailExtension} used to provide an in-memory SMTP server
     * for integration testing.</p>
     *
     * <p>This shared mail server allows tests to send and verify emails without requiring any
     * external SMTP infrastructure. It is configured with a single user account
     * (<code>komunumo</code>/<code>s3cr3t</code>) and persists across test methods to improve
     * performance.</p>
     */
    @RegisterExtension
    private static final @NotNull GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("komunumo", "s3cr3t"))
            .withPerMethodLifecycle(false);

    /**
     * <p>The HTTP port on which the Spring Boot application under test is running.</p>
     *
     * <p>The port is assigned randomly by the Spring Boot test framework to prevent conflicts when multiple test
     * instances run in parallel.</p>
     */
    @LocalServerPort
    private int port;

    /**
     * <p>Holds the base URL of the running test instance, typically pointing to the local test server.</p>
     *
     * <p>This value is initialized before each test with the dynamically assigned port to ensure that
     * components depending on the instance URL can correctly resolve local endpoints.</p>
     */
    private String instanceUrl = "";

    /**
     * <p>Returns the HTTP port on which the Spring Boot application is currently running.</p>
     *
     * <p>This method is useful for constructing URLs in integration tests that perform real HTTP requests against
     * the running application (for example, API endpoint or web interface tests).</p>
     *
     * @return the configured server port, for example {@code 8081}
     */
    protected int getPort() {
        return port;
    }

    /**
     * <p>Returns the base URL of the running test instance.</p>
     *
     * <p>The returned value typically points to the local test server and includes the dynamically assigned port. It
     * can be used by integration tests to construct absolute URLs for HTTP requests against the running instance.</p>
     *
     * @return the base URL of the current test instance
     */
    protected String getInstanceUrl() {
        return instanceUrl;
    }

    /**
     * <p>Returns the shared {@link GreenMailExtension} instance used for email testing.</p>
     *
     * <p>This method gives subclasses access to the in-memory SMTP server so that they can verify
     * sent messages, inspect inboxes, or reset the mail state between tests.</p>
     *
     * @return the global {@link GreenMailExtension} instance used by all integration tests
     */
    protected GreenMailExtension getGreenMail() {
        return greenMail;
    }

    /**
     * <p>Prepares the integration test environment before each test execution.</p>
     *
     * <p>This method ensures a consistent and isolated test setup by purging all emails, resetting the database schema
     * via Flyway, recreating demo data, and configuring the instance URL based on the dynamically assigned test port.</p>
     *
     * <p>The configuration is updated within an administrative context to apply changes immediately and clear any
     * cached configuration values. This guarantees that every test starts from a known, reproducible state.</p>
     *
     * @throws FolderException if an error occurs while purging emails from the mailboxes
     *
     * @see Flyway
     * @see DemoMode
     * @see ConfigurationSetting#INSTANCE_URL
     */
    @BeforeEach
    void prepareIntegrationTest() throws FolderException {
        greenMail.purgeEmailFromAllMailboxes();

        flyway.clean();
        flyway.migrate();
        demoMode.resetDemoData();

        instanceUrl = "http://localhost:%d/".formatted(getPort());
        configurationService.setConfiguration(ConfigurationSetting.INSTANCE_URL, instanceUrl);
        configurationService.clearCache();
    }

    /**
     * <p>Retrieves the first email received by GreenMail whose subject matches the provided value. The method actively
     * waits until such an email arrives or the configured timeout is reached. This is intended for integration tests
     * where emails may be delivered asynchronously and must be awaited before assertions can be performed.</p>
     *
     * <p>The search is repeated during the waiting period, and the first matching message is returned immediately
     * once detected. If no matching email arrives before the timeout expires, the method fails through Awaitility's
     * timeout mechanism.</p>
     *
     * @param subject the subject line to match against incoming emails
     * @return the first {@link Message} instance whose subject matches the provided value
     */
    protected @NotNull Message getEmailBySubject(final @NotNull String subject) {
        final var found = new AtomicReference<Message>();

        await().atMost(GREENMAIL_WAIT_TIMEOUT, SECONDS).until(() -> {
            final var message = Stream.of(greenMail.getReceivedMessages())
                    .filter(m -> {
                        try {
                            return subject.equals(m.getSubject());
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .orElse(null);
            found.set(message);
            return message != null;
        });

        return found.get();
    }

    /**
     * <p>Generates a random email address using the current system time in milliseconds to ensure uniqueness.</p>
     *
     * <p>The generated address follows the format: {@code user-<timestamp>@example.com}.</p>
     *
     * @return a randomly generated email address
     */
    protected String getRandomEmailAddress() {
        return "user_" + System.currentTimeMillis() + "@example.com";
    }

}
