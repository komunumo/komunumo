# Komunumo

[![All Tests](https://github.com/komunumo/komunumo/actions/workflows/all-tests.yml/badge.svg)](https://github.com/komunumo/komunumo/actions/workflows/all-tests.yml)
[![codecov](https://codecov.io/github/komunumo/komunumo/graph/badge.svg?token=w7dqTcK0Yy)](https://codecov.io/github/komunumo/komunumo)

**Open Source Community Manager**

*Komunumo* is an esperanto noun with a meaning of *community*.

## Table of Contents

- [About](#about)
- [Versioning](#versioning)
- [Contributing](#contributing)
- [Communication](#communication)
    - [Matrix Chat](#matrix-chat)
    - [GitHub Discussions](#github-discussions)
- [Configuration](#configuration)
    - [Server Configuration](#server-configuration)
    - [Instance Configuration](#instance-configuration)
        - [Admin](#admin)
    - [File Configuration](#file-configuration)
    - [Demo Mode Configuration](#demo-mode-configuration)
        - [Providing Custom Demo Data](#providing-custom-demo-data)
    - [Mail Configuration](#mail-configuration)
        - [Available Environment Variables](#available-environment-variables)
        - [Example Configuration](#example-configuration)
    - [Database Configuration](#database-configuration)
        - [Important MySQL and MariaDB configuration](#important-mysql-and-mariadb-configuration)
    - [Custom Styling](#custom-styling)
- [Copyright and License](#copyright-and-license)

## About

You are a community leader? *Komunumo* makes your life a lot easier! Everything you need to run your community in one place, for free! *Komunumo* is an open source community manager software which assists you in all tasks managing a community like a user group or a meetup.

With *Komunumo* you can manage your community members (including self-management), organize events (including registration), send out newsletters, manage your sponsors, and a lot more.

We have set ourselves the goal of implementing all important [Meetup](https://www.meetup.com/) functions for communities in *Komunumo*. We strive for full compatibility with the [Fediverse](https://en.wikipedia.org/wiki/Fediverse) to ensure smooth data exchange with [Mastodon](https://en.wikipedia.org/wiki/Mastodon_(social_network)), [Mobilizon](https://en.wikipedia.org/wiki/Mobilizon), and other services using the [ActivityPub](https://en.wikipedia.org/wiki/ActivityPub) protocol.

**Please read more about the vision of *Komunumo* in the separate file [VISION.md](VISION.md).**

## Versioning

*Komunumo* follows [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html). Version numbers are structured as `MAJOR.MINOR.PATCH`:

- **MAJOR** versions introduce incompatible API or data changes,
- **MINOR** versions add functionality in a backwards-compatible manner,
- **PATCH** versions include backwards-compatible bug fixes.

This versioning scheme applies to the public API, database schema, and plugin interfaces. We aim to keep upgrades predictable and manageable. Breaking changes, new features, and fixes are documented in the [CHANGELOG.md](CHANGELOG.md) and in the [release notes](https://github.com/komunumo/komunumo/releases).

## Contributing

You can find a lot of information on how you can contribute to *Komunumo* in the separate file [CONTRIBUTING.md](CONTRIBUTING.md). A curated list of contributors is available in the file [CONTRIBUTORS.md](CONTRIBUTORS.md).

## Communication

### Matrix Chat

There is a channel at Matrix for quick and easy communication. This is publicly accessible for everyone. For developers as well as users. The communication in this chat is to be regarded as short-lived and has no documentary character.

You can find our Matrix channel here: [@komunumo:ijug.eu](https://matrix.to/#/%23komunumo:ijug.eu)

### GitHub Discussions

We use the corresponding GitHub function for discussions. The discussions held here are long-lived and divided into categories for the sake of clarity. One important category, for example, is that for questions and answers.

Discussions on GitHub: https://github.com/komunumo/komunumo/discussions  
Questions and Answers: https://github.com/komunumo/komunumo/discussions/categories/q-a

## Configuration

The file `application.properties` contains only some default values. To override the default values and to specify other configuration options, just set them as environment variables. The following sections describe all available configuration options. You only need to specify these options if your configuration settings differ from the defaults.

### Server Configuration

The server runs on port 8080 by default. If you don't like it, change it using an environment variable:

```
KOMUNUMO_PORT=8080
```

### Instance Configuration

#### Admin

When starting *Komunumo* for the first time, the system can automatically create an initial instance admin. To enable this, set the following environment variable to the email address of the admin:

```
KOMUNUMO_INSTANCE_ADMIN=admin@example.eu
```

If no user with the admin role is found in the database, a new instance admin will be created with the email address given.

> [!WARNING]
> This mechanism only runs once! It will **not** overwrite or recreate users if an admin already exists.

### File Configuration

| Variable                 | Default                       | Description                            |
|--------------------------|-------------------------------|----------------------------------------|
| `KOMUNUMO_FILES_BASEDIR` | `${user.home}/.komunumo/data` | Base directory for local file storage. |

*Komunumo* stores files on the local file system, for example uploaded images. The base directory must be writable by the application and should have sufficient disk space. To ensure data integrity, administrators are strongly advised to include this directory in regular backup routines.

The placeholder `${user.home}` refers to the home directory of the system user running the application.

### Demo Mode Configuration

*Komunumo* can be started in demo mode, which will automatically delete any data entered at each start of the server and at the top of every hour! This ensures a consistent state for repeated testing and demonstrations. To enable demo mode, set the following environment variable:

```
KOMUNUMO_DEMO_ENABLED=true
```

#### Providing Custom Demo Data

If you want to provide custom demo data, you can specify a JSON file. The file must be publicly accessible over HTTPS and contain valid JSON in the expected format. To use your own demo data, set the following environment variable:

```
KOMUNUMO_DEMO_JSON=https://example.com/events.json
```

The structure of the JSON file must follow the expected format used by *Komunumo* for demo data. The file should contain an array of setting, user, community, event, image, and global page objects. Here is an example of the expected JSON structure:

```json
{
    "settings": [
        {
            "setting": "instance.name",
            "language": "en",
            "value": "Komunumo Demo"
        },
        {
            "setting": "instance.slogan",
            "language": "en",
            "value": "This is a demo instance of Komunumo."
        }
    ],
    "users": [
        {
            "userId": "1b7cd51b-4f1b-43d1-a51c-e58043b5d7a4",
            "handle": "admin",
            "email": "admin@example.com",
            "name": "Admin User",
            "bio": "The bio of the admin.",
            "imageId": "",
            "role": "ADMIN",
            "type": "LOCAL"
        },
        {
            "userId": "025d62be-b564-4bd4-9325-f2f1d8bf2093",
            "handle": "user",
            "email": "user@example.com",
            "name": "Test User",
            "bio": "The bio of the user.",
            "imageId": "24a27732-09c3-45e0-9b4c-3d6a3f2eb81b",
            "role": "USER",
            "type": "LOCAL"
        },
        {
            "userId": "f309ee0d-519d-4eaa-8781-256f3200f243",
            "email": "anonymous@example.com",
            "name": "",
            "bio": "",
            "imageId": "",
            "role": "USER",
            "type": "ANONYMOUS"
        }
    ],
    "communities": [
        {
            "communityId": "9a73690b-6dbd-456a-88e9-dc3f77b69aa0",
            "handle": "demo",
            "name": "Demo Community",
            "description": "A description of the demo community.",
            "imageId": "0278ec5a-9fe1-4882-85f9-845ca72c2795"
        }
    ],
    "members": [
        {
            "userId": "1b7cd51b-4f1b-43d1-a51c-e58043b5d7a4",
            "communityId": "9a73690b-6dbd-456a-88e9-dc3f77b69aa0",
            "role": "OWNER",
            "since": "2025-01-01T10:00:00+02:00[Europe/Zurich]"
        },
        {
            "userId": "025d62be-b564-4bd4-9325-f2f1d8bf2093",
            "communityId": "9a73690b-6dbd-456a-88e9-dc3f77b69aa0",
            "role": "ORGANIZER",
            "since": "2025-01-01T10:00:00+02:00[Europe/Zurich]"
        },
        {
            "userId": "f309ee0d-519d-4eaa-8781-256f3200f243",
            "communityId": "9a73690b-6dbd-456a-88e9-dc3f77b69aa0",
            "role": "MEMBER",
            "since": "2025-01-01T10:00:00+02:00[Europe/Zurich]"
        }
    ],
    "events": [
        {
            "eventId": "07fe5b04-0ea1-43b7-a63a-f4d8b8c29ed6",
            "communityId": "9a73690b-6dbd-456a-88e9-dc3f77b69aa0",
            "title": "Demo Event",
            "description": "A description of the demo event.",
            "location": "Somewhere",
            "begin": "2025-08-06T18:00:00+02:00[Europe/Zurich]",
            "end": "2025-08-06T20:00:00+02:00[Europe/Zurich]",
            "imageId": "4ca05a55-de1e-4571-a833-c9e5e4f4bfba",
            "visibility": "PUBLIC",
            "status": "PUBLISHED"
        }
    ],
    "images": [
        {
            "imageId": "24a27732-09c3-45e0-9b4c-3d6a3f2eb81b",
            "contentType": "image/jpeg",
            "url": "https://example.com/images/demo-user.jpg"
        },
        {
            "imageId": "0278ec5a-9fe1-4882-85f9-845ca72c2795",
            "contentType": "image/jpeg",
            "url": "https://example.com/images/demo-community.jpg"
        },
        {
            "imageId": "4ca05a55-de1e-4571-a833-c9e5e4f4bfba",
            "contentType": "image/jpeg",
            "url": "https://example.com/images/demo-event.jpg"
        }
    ],
    "globalPages": [
        {
            "slot": "imprint",
            "language": "en",
            "title": "Legal Notice",
            "markdown": "## Legal Notice\n\nThis is a demo for testing purposes **only**."
        }
    ]
}
```

- The `settings` array contains key-value pairs for instance settings, such as the instance name and slogan.
- The `language` fields specify the language of the content.
- The `communityId`, `eventId`, and `imageId` fields must be valid UUIDs.
- The `begin` and `end` fields in the `events` object must be in ISO 8601 format with a timezone offset, such as `2025-08-06T18:00:00+02:00[Europe/Zurich]`. This ensures correct interpretation of event times, including proper handling of daylight saving time. They can be empty or `null` if not applicable.
- The `imageId` fields in the `users`, `communities`, and `events` objects must refer to the IDs of the images in the `images` array. The `url` field in each image object must point to a publicly accessible image file. If the `imageId` is blank, the community or event will not have an image associated with it.
- The `type` fields in the `users` object can be set to `LOCAL`, `REMOTE`, or `ANONYMOUS` to define the user type.
- The `role` fields in the `users` object can be set to `ADMIN` or `USER` to define the role of the user for this instance.
- The `role` fields in the `members` object can be set to `OWNER`, `ORGANIZER`, or `MEMBER` to define the membership role of the user in the community.
- The `visibility` field in the `events` object can be set to `PUBLIC` or `PRIVATE`, to define the visibility of the event.
- The `status` field can be set to `DRAFT`, `PUBLISHED`, or `CANCELED` to reflect the current state of the event.
- The `contentType` field in the `images` object must contain a valid MIME type for the image and be one of: `image/gif`, `image/jpeg`, `image/png`, `image/svg+xml`, or `image/webp`
- The `url` field in the `images` object must point to a publicly accessible image file using `https://`, to an accessible file using `file://`, or contain the image data using `data:`.

### Mail Configuration

*Komunumo* supports sending email notifications. Configuration is done via environment variables using the `KOMUNUMO_MAIL_*` naming scheme.

#### Available Environment Variables

| Variable                          | Default             | Description                                                           |
|-----------------------------------|---------------------|-----------------------------------------------------------------------|
| `KOMUNUMO_MAIL_FROM`              | `noreply@localhost` | Sender address shown in outgoing emails (e.g. noreply@example.com).   |
| `KOMUNUMO_MAIL_REPLY_TO`          | *(empty)*           | Optional reply-to address (e.g. `support@example.com`).               |
| `KOMUNUMO_MAIL_HOST`              | `localhost`         | Mail server address. Use a local MTA or external SMTP provider.       |
| `KOMUNUMO_MAIL_PORT`              | `25`                | Port for the SMTP server (e.g., `587` for STARTTLS or `465` for SSL). |
| `KOMUNUMO_MAIL_PROTOCOL`          | `smtp`              | Protocol used for sending email. Usually `smtp`.                      |
| `KOMUNUMO_MAIL_USERNAME`          | *(empty)*           | Username for SMTP authentication, if required.                        |
| `KOMUNUMO_MAIL_PASSWORD`          | *(empty)*           | Password for SMTP authentication, if required.                        |
| `KOMUNUMO_MAIL_SMTP_AUTH`         | `false`             | Whether SMTP authentication is enabled.                               |
| `KOMUNUMO_MAIL_STARTTLS_ENABLE`   | `false`             | Enable STARTTLS encryption (recommended for port 587).                |
| `KOMUNUMO_MAIL_STARTTLS_REQUIRED` | `false`             | Require STARTTLS (connection will fail if not supported).             |
| `KOMUNUMO_MAIL_SSL_ENABLE`        | `false`             | Enable SSL encryption (typically for port 465).                       |
| `KOMUNUMO_MAIL_ENCODING`          | `UTF-8`             | Default encoding for email subject and content.                       |

#### Example Configuration

In a `.env` file, CI system, or Docker environment:

```bash
KOMUNUMO_MAIL_FROM=noreply@example.com
KOMUNUMO_MAIL_REPLY_TO=support@example.com
KOMUNUMO_MAIL_HOST=smtp.example.com
KOMUNUMO_MAIL_PORT=587
KOMUNUMO_MAIL_USERNAME=myuser
KOMUNUMO_MAIL_PASSWORD=secret
KOMUNUMO_MAIL_SMTP_AUTH=true
KOMUNUMO_MAIL_STARTTLS_ENABLE=true
```

> [!TIP]
> If you are using a local mail relay (e.g., [Nullmailer](https://untroubled.org/nullmailer/) or [Postfix](https://www.postfix.org/)), you can often omit authentication and encryption settings.

### Database Configuration

*Komunumo* needs a database to store the business data. By default, *Komunumo* comes with [MariaDB](https://mariadb.org/) drivers. MariaDB is recommended because we are using it during development, and it is highly tested with *Komunumo*. All free and open source JDBC compatible databases are supported, but you need to configure the JDBC driver dependencies accordingly. Please make sure that your database is using a Unicode character set to avoid problems storing data containing Unicode characters. The database user to access the *Komunumo* database executes automatic schema migrations and needs `ALL PRIVILEGES`.

Please configure the database connection using the following environment variables:

```
KOMUNUMO_DB_URL=jdbc:mariadb://localhost:3306/komunumo?serverTimezone\=Europe/Zurich&allowMultiQueries=true
KOMUNUMO_DB_USER=johndoe
KOMUNUMO_DB_PASS=verysecret
```

The database schema will be migrated automatically by *Komunumo*.

#### Important MySQL and MariaDB configuration

MySQL and MariaDB have a possible silent truncation problem with the `GROUP_CONCAT` command. To avoid this it is necessary, to configure these two databases to allow multi queries. Just add `allowMultiQueries=true` to the JDBC database URL like in this example (you may need to scroll the example code to the right):

```
KOMUNUMO_DB_URL=jdbc:mariadb://localhost:3306/komunumo?serverTimezone\=Europe/Zurich&allowMultiQueries=true
```

### Custom Styling

The visual appearance of *Komunumo* can be customized to your instance's need. As of now, this configuration is globally per instance (e.g. no further styling per community).

To load a custom stylesheet (CSS file), just add it to the following location and restart your instance:

```
{KOMUNUMO_FILES_BASEDIR}/custom/styles/styles.css
```

*Komunumo* detects whether this file is present and if so loads it within the HTML output of all pages.

If you reference other static files like images from your stylesheet, make sure to put those files in the same directory as the `styles.css` file.


## Copyright and License

[AGPL License](https://www.gnu.org/licenses/agpl-3.0.de.html)

*Copyright (C) Marcus Fihlon and the individual contributors to **Komunumo**.*

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
