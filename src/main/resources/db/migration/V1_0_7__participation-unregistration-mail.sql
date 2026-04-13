-- [jooq ignore start]
INSERT INTO mail_template (id, language, subject, markdown)
VALUES ('EVENT_UNREGISTRATION_NOTIFY_MANAGERS', 'DE', 'Abmeldung für "${eventTitle}"',
        'Hallo,\n\n${participantName} hat sich soeben von "${eventTitle}" abgemeldet.\n\nAktuelle Anzahl Anmeldungen: ${participantCount}\n\nViele Grüße\n${instanceName}'),
       ('EVENT_UNREGISTRATION_NOTIFY_MANAGERS', 'EN', 'Unregistration for "${eventTitle}"',
        'Hello,\n\n${participantName} just unregistered from "${eventTitle}".\n\nCurrent number of registrations: ${participantCount}\n\nBest regards\n${instanceName}');
-- [jooq ignore stop]
