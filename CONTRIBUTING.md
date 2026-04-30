# Contributing

**In this document you will find a lot of information on how you can contribute to *Komunumo*.**

- [How to contribute](#how-to-contribute)
    - [Project Board](#project-board)
    - [Good first issues](#good-first-issues)
    - [Commit Messages](#commit-messages)
    - [Sign-off your commits](#sign-off-your-commits)
    - [AI generated code](#ai-generated-code)
    - [Testing](#testing)
    - [Pull Request Scope](#pull-request-scope)
- [Quickstart for Local Development](#quickstart-for-local-development)
    - [Prerequisites](#prerequisites)
    - [Clone and Verify](#clone-and-verify)
    - [Start Required Services](#start-required-services)
    - [Configure Environment Variables](#configure-environment-variables)
    - [Run Komunumo](#run-komunumo)
    - [Open in Browser](#open-in-browser)
    - [Stop Komunumo](#stop-komunumo)
- [Communication](#communication)
    - [Matrix Chat](#matrix-chat)
    - [GitHub Discussions](#github-discussions)
- [Architecture](#architecture)
    - [Structure](#structure)
    - [Boundary Control Entity Architecture](#boundary-control-entity-architecture)
    - [Useful Vaadin Links](#useful-vaadin-links)
- [Internationalization](#internationalization)
    - [Translation Files](#translation-files)
    - [Message Format](#message-format)
- [Database](#database)
    - [General Principles](#general-principles)
    - [Index Names](#index-names)
    - [Starting a Local MariaDB Instance](#starting-a-local-mariadb-instance)
- [Build](#build)
    - [Maven](#maven)
- [Running and debugging](#running-and-debugging)
    - [Running the server from the command line.](#running-the-server-from-the-command-line)
    - [Running and debugging the server in Intellij IDEA](#running-and-debugging-the-server-in-intellij-idea)
    - [Running and debugging the server in Eclipse](#running-and-debugging-the-server-in-eclipse)
- [Running using Docker](#running-using-docker)

## How to contribute

### Project Board

To get an overview of ongoing and upcoming work, you can check our [project board](https://github.com/orgs/komunumo/projects/1), which organizes issues in a Kanban-style workflow:

| Status      | Description                                                   |
|-------------|---------------------------------------------------------------|
| Backlog     | Ideas and requests that are not yet analyzed.                 |
| In Analysis | Issues currently under discussion or refinement.              |
| Ready       | Issues that are fully specified and ready to be implemented.  |
| In Progress | Issues actively being worked on.                              |
| Done        | Recently completed issues; older ones are archived over time. |

> [!TIP]
> If you’re looking for something to work on, focus on issues in the **Ready** column. These contain all necessary information to start contributing right away.

### Good first issues

To find possible tasks for your first contribution to *Komunumo*, we tagged some of the hopefully easier to solve issues as [good first issue](https://github.com/komunumo/komunumo/labels/good%20first%20issue).

If you prefer to meet people in real life to contribute to *Komunumo* together, we recommend to visit a [Hackergarten](https://www.hackergarten.net/) event. *Komunumo* is often selected as a contribution target in [Lucerne](https://www.meetup.com/hackergarten-luzern/), [Zurich](https://www.meetup.com/hackergarten-zurich/), and at the [CyberLand Open Source Camp](https://cyberland.ijug.eu/).

Please join our developer community using our [Matrix chat](#matrix-chat) to get support and help for contributing to *Komunumo*.

### Commit Messages

Please follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for all commit messages and pull request titles. This structured format helps automate changelogs, release processes, and code reviews.

For *Komunumo*, we love to add the issue number to the end of the commit message. This helps to track the changes in the code and the issue tracker. When the commit closes an issue, the `closes`, `fixes`, or `resolves` keyword can be used. This will automatically close the issue when the commit is merged into the main branch.

A commit message consists of the following parts:

```
<type>[optional scope]: <short description> [optional keyword] #<issue number>

[optional body]

[optional footer]
```

#### Examples

- `feat: add support for passkey authentication closes #123`
- `fix(event): correct date formatting in export fixes #456`
- `chore: add missing license headers to source files resolves #789`

#### Common `type` values:

| Type       | Purpose                                                               |
|------------|-----------------------------------------------------------------------|
| `feat`     | Introduce a new feature                                               |
| `fix`      | Fix a bug                                                             |
| `docs`     | Documentation-only changes                                            |
| `style`    | Code style changes (formatting, whitespace, missing semicolons, etc.) |
| `refactor` | Code refactoring without functional change                            |
| `perf`     | Improve performance without changing features or behavior             |
| `test`     | Add or update tests                                                   |
| `build`    | Changes to the build system, packaging or dependencies (e.g. Maven)   |
| `ci`       | Changes to CI/CD configuration (e.g. GitHub Actions, Woodpecker)      |
| `chore`    | Maintenance tasks (configs, license, release meta, etc.)              |
| `revert`   | Revert a previous commit                                              |
| `deps`     | Add, update, or remove dependencies                                   |
| `security` | Address security issues or vulnerabilities                            |

> [!TIP]
> For more information, see [conventionalcommits.org](https://www.conventionalcommits.org/)

### Sign-off your commits

It is important to sign-off *every* commit. That is a de facto standard way to ensure that *you* have the right to submit your content and that you agree to the [DCO](DCO.md) (Developer Certificate of Origin).

You can find more information about why this is important and how to do it easily in a very good [blog post](https://dev.to/janderssonse/git-signoff-and-signing-like-a-champ-41f3)  by Josef Andersson.

### AI generated code

AI generated source code is based on real existing source code, which is copied in whole or in part into the generated code. The license of the original source code with which the AI was trained is not taken into account. It is not clear which license conditions apply and how these can be complied with. For legal reasons, we therefore do not allow AI-generated source code at all.

### Testing

#### Assertions

*Komunumo* uses [AssertJ](https://assertj.github.io/doc/) for writing assertions in unit tests. To ensure consistency, readability, and rich failure messages, only AssertJ should be used for all assertions. The use of JUnit's built-in `assert*` methods and Hamcrest matchers is explicitly disallowed and enforced through architecture tests. This includes, but is not limited to:

- `org.junit.Assert`
- `org.junit.jupiter.api.Assertions`
- `org.hamcrest.MatcherAssert`

Instead, use AssertJ's fluent assertion style, for example:

```java
assertThat(actual).isEqualTo(expected);

assertThat(collection).containsExactly("one", "two");

assertThatThrownBy(() -> service.doSomethingBad())
    .isInstanceOf(IllegalArgumentException.class);
```

#### Integration Tests

All **integration** tests must extend the abstract `KaribuTest` or `BrowserTest` base class. This ensures consistent configuration of the Spring Boot context, dependency injection, and database setup across all integration tests. Extend from the `KaribuTest` class for tests that do not require a browser using [Karibu Testing](https://github.com/mvysny/karibu-testing), or from the `BrowserTest` class for tests that need to interact with the UI in a browser environment using [Playwright](https://playwright.dev/).

Do not annotate integration tests manually with `@SpringBootTest`, `@Transactional`, or other Spring annotations. This is already handled by the base class. The base classes also provide shared utilities and guarantees a controlled test environment. To distinguish integration tests from unit tests, all Karibu test classes should be named with the `KT` suffix and all Playwright browser tests should be named with the `BT` suffix.

Example:

```java
class UserServiceIT extends KaribuTest {

    @Autowired
    private UserService userService;

    @Test
    void shouldReturnUserByEmail() {
        final var user = userService.findByEmail("demo@example.eu");
        assertThat(user).isPresent();
    }
}

```

> [!WARNING]
> Integration tests that do not extend `KaribuTest` or `BrowserTest` may not run correctly or may produce unforeseen errors.

### Pull Request Scope

Please make sure that each pull request is focused on a single issue or change. Avoid bundling unrelated modifications together in the same PR, as this makes the review process harder and slows down merging. If you want to contribute multiple independent changes, submit them as separate pull requests so they can be reviewed and merged individually.

> [!NOTE]  
> GitHub only allows one pull request per branch. To open multiple pull requests, you need to create a separate branch for each change.

### Before Submitting a Pull Request

Please make sure your contribution meets all the following requirements:

- Checkstyle and PMD report no violations.
- The Modernizer Maven Plugin passes without issues.
- All automated tests are green.
- Test coverage is 100% (we practice Test-Driven Development).
- License headers are present in all source files (missing headers can be added using: `./mvnw license:format`).
- The branch is up to date with the main branch (please rebase if necessary before opening a Pull Request).
- The Pull Request title follows the [Conventional Commit](#commit-messages) style and references the related issue number (e.g. `feat: add event export closes #123`).

> [!TIP]
> All checks above are executed by:
> ```bash
> ./mvnw verify
> ```
> Run this locally and ensure it completes successfully before opening a Pull Request.  
> The CI pipeline will run the same checks and block PRs that fail.

**Commit & signature policy**

- Every commit must include a `Signed-off-by` line ([Developer Certificate of Origin](DCO.md)).
- Every commit must be cryptographically signed with GPG, SSH, or S/MIME.
- GitHub verifies these signatures during Pull Requests; unverified commits may be rejected.
- For detailed setup and usage instructions, see [Signing commits – GitHub Docs](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits).

## Quickstart for Local Development

This guide helps you get *Komunumo* up and running on your local machine for development purposes.

### Prerequisites

Make sure the following tools are installed:

- Git
- Java LTS (currently version 25) - check with `java -version`
- Podman or Docker

#### Important Podman Information

##### Linux Users

If you are using Podman on Linux, you need to enable the `podman.socket` systemd service.
This allows non-root users to run Podman commands without `sudo`.

You can enable it with the following command:

```bash
systemctl --user enable --now podman.socket
```

This command starts the Podman socket for your user and ensures it automatically starts on login.
Once enabled, you can use Podman just like Docker, without elevated privileges.

To make sure that Testcontainers can connect to your Podman socket, set the following environment variables in your shell:

```bash
export DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true
```

##### macOS Users

If you use Podman Desktop for macOS, activate the full Docker compatibility layer instead:

1. Start the "Podman Desktop" app.
2. Navigate to "Settings", "Preferences", "Docker Compatibility" and *enable* it.
3. Now a new entry "Docker Compatibility" appears directly under "Settings".
4. Click on the new entry "Docker Compatibility".
5. Make sure, "Third-Party Docker Tool Compatibility" is *activated*.

#### Testcontainers Troubleshooting

Testcontainers can read global settings from `~/.testcontainers.properties`. These settings apply to all projects and Docker-compatible runtimes.

If tests fail because Testcontainers cannot connect to Docker, Podman, or another Docker-compatible runtime, check whether that file contains stale `docker.host` or `tc.host` values. Remove outdated entries unless you intentionally need a global override. Prefer project-specific environment variables or Maven profiles where possible.

#### Using Colima on macOS

[Colima](https://github.com/abiosoft/colima) can be used instead of Docker Desktop/Podman.
Install Colima and the Docker CLI, then start the default Colima instance:

```bash
brew install colima docker
colima start
docker context use colima
```

Verify that Docker commands are routed to Colima:

```bash
docker version
docker context inspect colima
```

When running Maven goals that use Testcontainers, enable the `colima` Maven profile:

```bash
./mvnw -Pcolima verify
```

The profile configures Testcontainers for the default Colima socket at `${user.home}/.colima/default/docker.sock` and sets the container-internal Docker socket override to `/var/run/docker.sock`.
If you use a non-default Colima instance, override the socket path explicitly:

```bash
./mvnw -Pcolima \
  -Dtestcontainers.docker.host=unix://${HOME}/.colima/YOUR_INSTANCE/docker.sock \
  verify
```

### Clone and Verify

1. Fork the [Komunumo repository](https://github.com/komunumo/komunumo) on GitHub.
2. Clone your fork locally:

   ```bash
   git clone https://github.com/YOUR_USERNAME/komunumo.git
   cd komunumo
   ```

3. Run all tests to verify that everything works:

   ```bash
   ./mvnw verify
   ```

   If you use Colima on macOS, run:

   ```bash
   ./mvnw -Pcolima verify
   ```

### Start Required Services

*Komunumo* needs a database and a mail server to run. For local development, you can start both using the provided `docker-compose.yml` file in the `dev-tools` directory:

**Docker**

```bash
cd ./dev-tools
docker compose up -d && docker compose logs -f || docker compose down
```

**Podman**

```bash
cd ./dev-tools
podman compose up -d && podman compose logs -f || podman compose down
```

The commands shown above will start the services in detached mode and then show the logs. If you stop the log view (e.g. using `CTRL`+`C`), all services will be stopped as well.

The following services will be available:

- MariaDB on Port 3306 with persistent data in `mariadb-data`.  
  The database user will be `komunumo` with the password `komunumo`. The database name is also `komunumo`.
- phpMyAdmin (database web UI) on port [4000](http://localhost:4000/)
- Mailpit (SMTP server) on port 1025, web UI on port [8025](http://localhost:8025), persistent data in `mailpit-data`

> [!IMPORTANT]  
> This setup is for development only and not suitable for production use.

**MariaDB Login Information**

| Field    | Value           |
|----------|-----------------|
| System   | MySQL / MariaDB |
| Server   | mariadb         |
| User     | komunumo        |
| Password | komunumo        |
| Database | komunumo        |

After starting the script, logs will be shown. To stop the services, press `Ctrl+C`. This stops all containers as well.

### Configure Environment Variables

*Komunumo* is configured using environment variables. At minimum, the following must be set:

```env
KOMUNUMO_INSTANCE_ADMIN=admin@example.com
KOMUNUMO_DB_URL='jdbc:mariadb://localhost:3306/komunumo?serverTimezone=Europe/Zurich&allowMultiQueries=true'
KOMUNUMO_DB_USER=komunumo
KOMUNUMO_DB_PASS=komunumo
KOMUNUMO_DEMO_ENABLED=true
KOMUNUMO_MAIL_HOST=localhost
KOMUNUMO_MAIL_PORT=1025
```

> [!WARNING]
> When demo mode is enabled (`KOMUNUMO_DEMO_ENABLED=true`), any data entered will be deleted at each start of the server and at the top of every hour! This ensures a consistent state for repeated testing and demonstrations.


If you're in a different timezone, adjust the `serverTimezone` parameter accordingly.
You can define these variables via your IDE, or directly in your shell.

### Run Komunumo

You can run *Komunumo* either via your IDE or the command line. On the command line, use the Maven Wrapper:

```bash
./mvnw
```

This runs the `app.komunumo.Application` main class.

### Open in Browser

Once the application is running, open your browser and navigate to:

http://localhost:8080/

Make sure JavaScript is enabled.

### Stop Komunumo

- In your IDE: use the stop button
- In the terminal: press `Ctrl+C`

## Communication

### Matrix Chat

There is a channel at Matrix for quick and easy communication. This is publicly accessible for everyone. For developers as well as users. The communication in this chat is to be regarded as short-lived and has no documentary character.

You can find our Matrix channel here: [@komunumo:ijug.eu](https://matrix.to/#/%23komunumo:ijug.eu)

### GitHub Discussions

We use the corresponding GitHub function for discussions. The discussions held here are long-lived and divided into categories for the sake of clarity. One important category, for example, is that for questions and answers.

Discussions on GitHub: https://github.com/komunumo/komunumo/discussions  
Questions and Answers: https://github.com/komunumo/komunumo/discussions/categories/q-a

## Architecture

The server of *Komunumo* is written using the [Java programming language](https://en.wikipedia.org/wiki/Java_(programming_language)). The main framework is [Spring](https://spring.io/). For the user interface, we use [Vaadin Flow](https://vaadin.com/flow). To access the database, we rely on [jOOQ](https://www.jooq.org/). To coordinate the build process, [Maven](https://maven.apache.org/) is used.

### Structure

Vaadin web applications are full-stack and include both client-side and server-side code in the same project.

| Directory                                    | Description                                                |
|:---------------------------------------------|:-----------------------------------------------------------|
| `src/main/frontend/themes/komunumo/`         | Base directory for default *Komunumo* theme                |
| &nbsp;&nbsp;&nbsp;&nbsp;`components/`        | Directory with CSS files for UI components                 |
| &nbsp;&nbsp;&nbsp;&nbsp;`fonts/`             | Directory containing font files                            |
| &nbsp;&nbsp;&nbsp;&nbsp;`views/`             | Directory with CSS files for UI views                      |
| &nbsp;&nbsp;&nbsp;&nbsp;`styles.css`         | Base CSS file with includes only                           |
| &nbsp;&nbsp;&nbsp;&nbsp;`theme.json`         | Theme configuration for Vaadin                             |
| `src/main/java/app/komunumo/`                | Server-side source directory                               |
| &nbsp;&nbsp;&nbsp;&nbsp;`domain/`            | Code for all business domains (BCE architecture)           |
| &nbsp;&nbsp;&nbsp;&nbsp;`jooq/`              | jOOQ DB layer code (converters, generators, etc.)          |
| &nbsp;&nbsp;&nbsp;&nbsp;`util/`              | Universal utility classes (not bound to a business domain) |
| &nbsp;&nbsp;&nbsp;&nbsp;`vaadin/components/` | Universal UI components (not bound to a business domain)   |
| &nbsp;&nbsp;&nbsp;&nbsp;`Application.java`   | Server entrypoint with `main` class                        |

### Boundary Control Entity Architecture

*Komunumo* follows the Boundary Control Entity (BCE) architecture pattern. This pattern divides the application into three main layers:

- **Boundary Layer**: This layer contains all user interface components, including Vaadin views and UI components. It handles user interactions and displays data to the user.
- **Control Layer**: This layer contains the service classes that implement the business logic of the application. It acts as an intermediary between the boundary and entity layers, coordinating data flow and enforcing business rules.
- **Entity Layer**: This layer contains the data model, like DTOs.

Inside the `domain` package, each business domain has its own subpackage containing its boundary, control, and entity classes. This modular structure promotes separation of concerns, making the codebase easier to maintain and extend. Each layer has a well-defined responsibility, ensuring that changes in one layer have minimal impact on others. Example:

| Directory                             | Description                                           |
|:--------------------------------------|:------------------------------------------------------|
| `src/main/java/app/komunumo/domain/`  | Code for all business domains (BCE architecture)      |
| &nbsp;&nbsp;&nbsp;&nbsp;`community/`  | Subpackage for the "Community" domain                 |
| &nbsp;&nbsp;&nbsp;&nbsp;`event/`      | Subpackage for the "Event" domain                     |
| &nbsp;&nbsp;&nbsp;&nbsp;`core/`       | Subpackage for core features not specific to a domain |
| &nbsp;&nbsp;&nbsp;&nbsp;`core/config` | Subpackage for the "Configuration" core feature       |

Inside each domain subpackage, you will find the following structure:

| Directory                                      | Description                                 |
|:-----------------------------------------------|:--------------------------------------------|
| `src/main/java/app/komunumo/domain/community/` | Subpackage for the "Community" domain       |
| &nbsp;&nbsp;&nbsp;&nbsp;`boundary/`            | Everything regarding the **Boundary Layer** |
| &nbsp;&nbsp;&nbsp;&nbsp;`control/`             | Everything regarding the **Control Layer**  |
| &nbsp;&nbsp;&nbsp;&nbsp;`entity`               | Everything regarding the **Entity Layer**   |

> [!TIP]
> More information about the BCE architecture can be found on the [BDE Design](https://bce.design/) website.

### Useful Vaadin Links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Discover Vaadin's set of CSS utility classes that enable building any UI without custom CSS in the [docs](https://vaadin.com/docs/latest/ds/foundation/utility-classes).
- Find a collection of solutions to common use cases in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin/platform).

## Internationalization

> [!IMPORTANT]  
> *Komunumo* uses the ICU message format for all translation files. This allows for advanced formatting options, including pluralization and named parameters. ICU stands for "International Components for Unicode".

### Translation Files

All user-facing text is externalized into properties files located in the `src/main/resources/vaadin-i18n/` directory. The english language is the default and is stored in `translations.properties`. Other languages follow the naming convention `translations_xx.properties`, where `xx` is the ISO 639-1 language code (e.g., `de` for German, `fr` for French).

As the key for the translations, we use a dot-separated hierarchical structure that reflects the package structure without the leading `app.komunumo.domain` and include the class name. This makes it easier to locate and manage translations.

Please try keep the translation files sorted alphabetically by key for better maintainability.

### Message Format

> [!IMPORTANT]  
> The message needs to be added to the properties files on a single line without line breaks. Line breaks in the examples below are for better readability only!

**Example (English):**
```
community.boundary.CommunityDetailView.memberCount = {0, plural,
    =0 {no members}
    one {one member}
    other {# members}
}
```

**Example (German):**
```
community.boundary.CommunityDetailView.memberCount = {0, plural,
    =0 {keine Mitglieder}
    one {ein Mitglied}
    other {# Mitglieder}
}
```

**Example (Russian):**
```
community.boundary.CommunityDetailView.memberCount = {0, plural,
    =0 {нет участников}
    one {один участник}
    few {# участника}
    many {# участников}
    other {# участников}
}
```

#### Named Parameters

The ICU message format also supports named parameters for better readability.

**Example (English):**
```
community.boundary.CommunityDetailView.memberCount = {count, plural,
    =0 {no members}
    one {one member}
    other {{count} members}
}
```

## Database

### General Principles

- **Database independence**: All schema definitions must be compatible with common SQL databases. Avoid vendor-specific features unless absolutely necessary.
- **Schema migrations**: Use [Flyway](https://flywaydb.org/) for schema versioning. Each migration must have a clearly written description and follow our naming convention.
- **Identifier generation**: Always use application-generated IDs (e.g., UUIDs) instead of `AUTO_INCREMENT`, `SERIAL`, or database sequences to ensure cross-database compatibility.
- **Primary key constraints**: Primary key constraints should **not** have explicit names; let the database assign a default name to avoid conflicts across different environments.
- **Timestamps**: Use `TIMESTAMP` instead of `DATETIME` to ensure consistent handling of time zones and better cross-database support.
- **Enum usage**: Use Java `enum` types mapped via jOOQ converters to `VARCHAR` columns in the database. Do **not** reference enum values from lookup tables.
- **DTO mapping**: All database access must go through the service layer. Records returned by jOOQ must be converted to DTOs in a dedicated mapping layer.
- **Referential Integrity**: Use foreign key constraints to ensure referential integrity where appropriate.
- **Optional Fields**: If a field is optional, make sure the database column is nullable and check for `null` explicitly in the application logic.

### Index Names

List of prefixes for index names:

| Prefix | Used For     | Example            |
|--------|--------------|--------------------|
| `uk_`  | Unique Key   | `uk_group_profile` |
| `fk_`  | Foreign Key  | `fk_event_group`   |
| `idx_` | Normal Index | `idx_event_begin`  |

Primary keys don't have a name and therefore no prefix!

### Starting a Local MariaDB Instance

To start a local MariaDB instance using [Docker](https://www.docker.com/) or [Podman](https://podman.io/), you can use the provided `docker-compose.yml` file in the `dev-tools` directory. Please see section [Start Required Services](#start-required-services) for detailed instructions.

## Build

### Maven

*Komunumo* uses [Maven](https://maven.apache.org/) to build the project. Please use standard Maven commands to build what you need:

| Command          | What it does                                                      |
|------------------|-------------------------------------------------------------------|
| `./mvnw`         | compile and run the app                                           |
| `./mvnw clean`   | cleanup generated files and build artefacts                       |
| `./mvnw compile` | compile the code without running the tests                        |
| `./mvnw test`    | compile and run all tests                                         |
| `./mvnw package` | compile, test, and create a JAR file to run it with Java directly |
| `./mvnw verify`  | compile, test, package, and run analysis tools                    |

There is *no need* to run the `install` or `deploy` tasks. They will just run longer, produce unnecessary output, burn energy, and occupy your disk space. [Don't just blindly run mvn clean install...](https://www.andreaseisele.com/posts/mvn-clean-install/)

## Running and debugging

There are two ways to run the application: From the command line or directly from your IDE.

### Running the server from the command line.

To run from the command line, use `./mvnw` and open http://localhost:8080 in your browser.

### Running and debugging the server in Intellij IDEA

#### Using Maven

- On the right side of the window, select "Maven" --> "Plugins" --> `spring-boot` --> `spring-boot:run`
- Optionally, you can disable tests by clicking on a `Skip Tests mode` blue button.

After the server has started, you can view it at http://localhost:8080/ in your browser.
You can now also attach breakpoints in code for debugging purposes, by clicking next to a line number in any source file.

#### Using the main method

- Locate the `Application.java` class in the Project view. It is in the `src` folder, under the main package's root.
- Right-click on the Application class
- Select "Debug 'Application.main()'" from the list

After the server has started, you can view it at http://localhost:8080/ in your browser.
You can now also attach breakpoints in code for debugging purposes, by clicking next to a line number in any source file.

### Running and debugging the server in Eclipse

#### Using Maven

- Right click on a project folder and select `Run As` --> `Maven build...`. After that a configuration window is opened.
- In the window set the value of the **Goals** field to `spring-boot:run`
- You can optionally select the `Skip tests` checkbox
- All the other settings can be left to default

Once configurations are set clicking `Run` will start the application.

Do not worry if the debugger breaks at a `SilentExitException`. This is a Spring Boot feature and happens on every startup.

After the server has started, you can view it at http://localhost:8080/ in your browser.
You can now also attach breakpoints in code for debugging purposes, by clicking next to a line number in any source file.

#### Using the main method

- Locate the `Application.java` class in the Package Explorer. It is in `src/main/java`, under the main package.
- Right-click on the file and select `Debug As` --> `Java Application`.

Do not worry if the debugger breaks at a `SilentExitException`. This is a Spring Boot feature and happens on every startup.

After the server has started, you can view it at http://localhost:8080/ in your browser.
You can now also attach breakpoints in code for debugging purposes, by clicking next to a line number in any source file.

## Running using Docker

> [!TIP]
> There is an [official Docker image](https://hub.docker.com/repository/docker/mcpringle/komunumo/general) you can use to run *Komunumo*.

To build the dockerized version of *Komunumo* yourself, run:

```
docker build . -t komunumo:latest
```

Once the Docker image is correctly built, you can test it locally using:

```
docker run -p 8080:8080 komunumo:latest
```
