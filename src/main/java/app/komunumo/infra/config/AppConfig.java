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
package app.komunumo.infra.config;

import app.komunumo.util.ImageUtil;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "komunumo")
public record AppConfig(@NotNull String version,
                        @NotNull DemoConfig demo,
                        @NotNull FilesConfig files,
                        @NotNull InstanceConfig instance,
                        @NotNull MailConfig mail) {

    @ConstructorBinding
    @SuppressWarnings({"java:S1186", "java:S6207"}) // needed to add the `@ConstructorBinding` annotation
    public AppConfig { }

    @PostConstruct
    public void configureUtilityClasses() {
        ImageUtil.initialize(this);
    }

}
