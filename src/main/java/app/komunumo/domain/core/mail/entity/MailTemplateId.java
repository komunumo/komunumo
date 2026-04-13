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

public enum MailTemplateId {
    ACCOUNT_REGISTRATION_SUCCESS,
    COMMUNITY_JOIN_SUCCESS_MEMBER,
    COMMUNITY_JOIN_SUCCESS_OWNER,
    CONFIRMATION_PROCESS,
    EVENT_REGISTRATION_NOTIFY_MANAGERS,
    EVENT_REGISTRATION_SUCCESS,
    EVENT_UNREGISTRATION_NOTIFY_MANAGERS,
    EVENT_UNREGISTRATION_SUCCESS,
    LOGIN_CONFIRMATION_MAIL,
    TEST
}
