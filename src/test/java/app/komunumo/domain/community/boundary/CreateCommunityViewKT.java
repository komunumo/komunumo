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
package app.komunumo.domain.community.boundary;

import app.komunumo.domain.core.image.control.ImageService;
import app.komunumo.domain.user.entity.UserRole;
import app.komunumo.infra.ui.vaadin.components.HandleField;
import app.komunumo.infra.ui.vaadin.components.ImageUpload;
import app.komunumo.infra.ui.vaadin.components.MarkdownEditor;
import app.komunumo.test.KaribuTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateCommunityViewKT extends KaribuTest {

    @Autowired
    private ImageService imageService;

    @Test
    void testView() {
        login(getTestUser(UserRole.USER));

        UI.getCurrent().navigate(CreateCommunityView.class);
        final var view = _get(VerticalLayout.class, spec -> spec.withClasses("create-community-view"));
        assertThat(view).isNotNull();

        final var handleField = _get(HandleField.class, spec -> spec.withClasses("handle-field"));
        assertThat(handleField).isNotNull();

        final var nameField = _get(TextField.class, spec -> spec.withClasses("name-field"));
        assertThat(nameField).isNotNull();

        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        assertThat(descriptionField).isNotNull();

        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        assertThat(imageField).isNotNull();

        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));
        assertThat(createButton).isNotNull();
    }

    @Test
    void testCreate_invalidSubmission() {
        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateCommunityView.class);

        final var component = _get(CreateCommunityView.class);
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));
        createButton.click();

        assertThat(component.getBinder().isValid()).isFalse();
    }

    @Test
    void testCreate_successfulSubmission_withoutImage() {
        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateCommunityView.class);

        final var view = _get(CreateCommunityView.class);
        final var handleField = _get(HandleField.class, spec -> spec.withClasses("handle-field"));
        final var nameField = _get(TextField.class, spec -> spec.withClasses("name-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        handleField.setValue("testCommunity");
        nameField.setValue("Test Community");
        descriptionField.setValue("Test Community Description");
        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .isEqualTo("communities/@testCommunity");
        assertThat(_get(H2.class, spec -> spec.withClasses("community-name")).getText())
                .isEqualTo("Test Community");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("community-description")).getContent())
                .isEqualTo("Test Community Description");
        assertThat(_find(Image.class, spec -> spec.withClasses("community-image")))
                .isEmpty();
    }

    @Test
    void testCreate_successfulSubmission_withImage() {
        final var imageId = imageService.getAllImageIds().getFirst();
        final var imageDto = imageService.getImage(imageId).orElseThrow();

        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateCommunityView.class);

        final var view = _get(CreateCommunityView.class);
        final var handleField = _get(HandleField.class, spec -> spec.withClasses("handle-field"));
        final var nameField = _get(TextField.class, spec -> spec.withClasses("name-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        handleField.setValue("testCommunity");
        nameField.setValue("Test Community");
        descriptionField.setValue("Test Community Description");
        imageField.setValue(imageDto);
        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .isEqualTo("communities/@testCommunity");
        assertThat(_get(H2.class, spec -> spec.withClasses("community-name")).getText())
                .isEqualTo("Test Community");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("community-description")).getContent())
                .isEqualTo("Test Community Description");
        assertThat(_get(Image.class, spec -> spec.withClasses("community-image")).getSrc())
                .isEqualTo("/images/%s.svg".formatted(imageId));
    }

    @Test
    void testCreate_successfulSubmission_setAndUnsetImage() {
        final var imageId = imageService.getAllImageIds().getFirst();
        final var imageDto = imageService.getImage(imageId).orElseThrow();

        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateCommunityView.class);

        final var view = _get(CreateCommunityView.class);
        final var handleField = _get(HandleField.class, spec -> spec.withClasses("handle-field"));
        final var nameField = _get(TextField.class, spec -> spec.withClasses("name-field"));
        final var descriptionField = _get(MarkdownEditor.class, spec -> spec.withClasses("description-field"));
        final var imageField = _get(ImageUpload.class, spec -> spec.withClasses("image-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        handleField.setValue("testCommunity");
        nameField.setValue("Test Community");
        descriptionField.setValue("Test Community Description");
        imageField.setValue(imageDto);
        imageField.setValue(null);
        assertThat(view.getBinder().isValid()).isTrue();

        createButton.click();

        assertThat(UI.getCurrent().getActiveViewLocation().getPath())
                .isEqualTo("communities/@testCommunity");
        assertThat(_get(H2.class, spec -> spec.withClasses("community-name")).getText())
                .isEqualTo("Test Community");
        assertThat(_get(Markdown.class, spec -> spec.withClasses("community-description")).getContent())
                .isEqualTo("Test Community Description");
        assertThat(_find(Image.class, spec -> spec.withClasses("community-image")))
                .isEmpty();
    }

    @Test
    void sessionTimeout() {
        login(getTestUser(UserRole.USER));
        UI.getCurrent().navigate(CreateCommunityView.class);

        final var view = _get(CreateCommunityView.class);
        final var handleField = _get(HandleField.class, spec -> spec.withClasses("handle-field"));
        final var nameField = _get(TextField.class, spec -> spec.withClasses("name-field"));
        final var createButton = _get(Button.class, spec -> spec.withClasses("create-button"));

        handleField.setValue("Test Profile");
        nameField.setValue("Test Name");
        assertThat(view.getBinder().isValid()).isTrue();

        logout(); // simulate session timeout by logging out

        createButton.click();

        final var notification = _get(Notification.class);
        assertThat(notification.isOpened()).isTrue();

        final var notificationDiv = _get(notification, Div.class);
        assertThat(notificationDiv.getText()).startsWith("You do not have permission to create a new community.");
    }

}
