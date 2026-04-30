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

import app.komunumo.infra.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Spring Web MVC configuration for serving custom static resources.</p>
 *
 * <p>This configuration registers an additional resource handler that exposes a configurable directory on the file
 * system under a fixed URL path. The primary use case is the delivery of user-provided or instance-specific CSS files
 * that extend or override the default application styling.</p>
 *
 * <p>The base directory for these resources is resolved from the application configuration and mapped to the URL path
 * {@code /custom/styles/**}.</p>
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    /**
     * <p>Provides access to the application-wide configuration.</p>
     *
     * <p>This configuration object is used to resolve file system paths and other runtime settings required for
     * registering custom static resource handlers. In this class, it is primarily used to determine the base directory
     * from which instance-specific style sheets are served.</p>
     */
    private final @NotNull AppConfig appConfig;

    /**
     * <p>Creates a new web configuration instance.</p>
     *
     * <p>The provided application configuration is used to resolve the base directory from which custom static
     * resources, such as style sheets, are served.</p>
     *
     * @param appConfig the application configuration used to resolve the base directory for custom resources
     */
    public WebConfiguration(final @NotNull AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * <p>Registers additional resource handlers for serving static content.</p>
     *
     * <p>This method maps the URL path {@code /custom/styles/**} to a directory on the local file system that is derived
     * from the configured base directory. All files located in this directory and its subdirectories are exposed as
     * static web resources.</p>
     *
     * @param registry the registry used to add resource handler mappings
     */
    @Override
    public void addResourceHandlers(final @NotNull ResourceHandlerRegistry registry) {
        final var stylePath = appConfig.files().basedir().resolve("custom", "styles");
        final var styleLocation = "file:" + stylePath.toAbsolutePath() + "/";
        registry.addResourceHandler("/custom/styles/**")
                .addResourceLocations(styleLocation);

        final var favivonPath = appConfig.files().basedir().resolve("custom", "favicon");
        final var faviconLocation = "file:" + favivonPath.toAbsolutePath() + "/";
        registry.addResourceHandler("/custom/favicon/**")
                .addResourceLocations(faviconLocation);
    }

}
