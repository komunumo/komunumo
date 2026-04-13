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
package app.komunumo.domain.core.mail.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplateIdTest {

    @Test
    void name() {
        assertThat(MailTemplateId.CONFIRMATION_PROCESS).hasToString("CONFIRMATION_PROCESS");
        assertThat(MailTemplateId.EVENT_REGISTRATION_NOTIFY_MANAGERS).hasToString("EVENT_REGISTRATION_NOTIFY_MANAGERS");
        assertThat(MailTemplateId.EVENT_REGISTRATION_SUCCESS).hasToString("EVENT_REGISTRATION_SUCCESS");
        assertThat(MailTemplateId.EVENT_UNREGISTRATION_NOTIFY_MANAGERS).hasToString("EVENT_UNREGISTRATION_NOTIFY_MANAGERS");
        assertThat(MailTemplateId.TEST).hasToString("TEST");
    }

}
