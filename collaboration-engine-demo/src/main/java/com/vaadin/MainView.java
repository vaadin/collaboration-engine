package com.vaadin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeBinder;
import com.vaadin.collaborationengine.CollaborativeMap;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("")
@Push
@CssImport("./styles/shared-styles.css")
public class MainView extends VerticalLayout {

    private static final String EDITOR_MAP_NAME = "editor";
    private static final String ACTIVITY_LOG_MAP_NAME = "activityLog";

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    public static class Person {
        private String firstName = "";
        private String lastName = "";

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return "Person [firstName=" + firstName + ", lastName=" + lastName
                    + "]";
        }
    }

    private Registration closeConnection;
    private CollaborativeBinder<Person> binder;

    private AvatarGroup collaboratorsAvatars = new AvatarGroup();
    private Avatar ownAvatar = new Avatar();

    private final Div log = new Div();

    public MainView() {
        addClassName("centered-content");
        ownAvatar.addClassName("own-avatar");
        log.setClassName("log");
        showLogin();
    }

    private void showLogin() {
        TextField usernameField = new TextField("Username");
        Button startButton = new Button("Start editing", event -> {
            String value = usernameField.getValue();
            if (!value.isEmpty()) {
                showPersonEditor(value);
            } else {
                Notification.show("Must enter a username to collaborate");
                usernameField.focus();
            }
        });
        startButton.addClickShortcut(Key.ENTER);

        removeAll();
        add(usernameField, startButton);
    }

    private void showPersonEditor(String username) {
        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");

        removeAll();
        Button submitButton = new Button("Submit", event -> {
            Person person = new Person();
            if (binder.writeBeanIfValid(person)) {
                closeConnection.remove();
                Notification.show("Submit: " + person);
                showLogin();
            }
        });

        ownAvatar.setName(username);

        HorizontalLayout avatarLayout = new HorizontalLayout(
                collaboratorsAvatars, ownAvatar);
        avatarLayout.setWidthFull();
        avatarLayout.setSpacing(false);
        avatarLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        add(avatarLayout, firstName, lastName, submitButton, log);

        binder = new CollaborativeBinder<>(Person.class, "binder");
        binder.forField(firstName).bind(FIRST_NAME);
        binder.forField(lastName).bind(LAST_NAME);
        binder.setUserName(username);

        /*
         * Tie connection to submit button so that it's deactivated when
         * detaching the form
         */
        closeConnection = CollaborationEngine.getInstance().openTopicConnection(
                submitButton, "form", topic -> configureTopicConnection(topic,
                        username, firstName, lastName));
    }

    private Registration configureTopicConnection(TopicConnection topic,
            String username, TextField firstName, TextField lastName) {
        Registration firstNameFocusRegistration = firstName.addFocusListener(
                event -> logEditorFocused(topic, FIRST_NAME, username));
        Registration lastNameFocusRegistration = lastName.addFocusListener(
                event -> logEditorFocused(topic, LAST_NAME, username));

        Registration firstNameBlurRegistration = firstName.addBlurListener(
                event -> logEditorBlurred(topic, FIRST_NAME, username));
        Registration lastNameBlurRegistration = lastName.addBlurListener(
                event -> logEditorBlurred(topic, LAST_NAME, username));

        binder.addValueChangeListener(e -> log(topic,
                username + " changed "
                        + ((TextField) e.getHasValue()).getLabel() + " to "
                        + e.getValue()));

        topic.getNamedMap(EDITOR_MAP_NAME)
                .subscribe(event -> updateEditors(event.getValue(), username));

        topic.getNamedMap(ACTIVITY_LOG_MAP_NAME).subscribe(
                event -> log.setText(Objects.toString(event.getValue(), "")));

        addEditor(topic.getNamedMap(EDITOR_MAP_NAME), "", username);

        log(topic, username + " joined");

        Registration editorRegistration = () -> {
            removeEditor(topic.getNamedMap(EDITOR_MAP_NAME), "", username);
            log(topic, username + " left");
        };

        return Registration.combine(firstNameFocusRegistration,
                lastNameFocusRegistration, firstNameBlurRegistration,
                lastNameBlurRegistration, editorRegistration);
    }

    private static void removeEditor(CollaborativeMap map, String key,
            String username) {
        MainView.<List<String>> updateMaps(map, key, Collections.emptyList(),
                oldEditors -> oldEditors.stream()
                        .filter(value -> !username.equals(value))
                        .collect(Collectors.toList()));
    }

    private static void addEditor(CollaborativeMap map, String key,
            String username) {
        MainView.<List<String>> updateMaps(map, key, Collections.emptyList(),
                oldEditors -> Stream
                        .concat(oldEditors.stream(), Stream.of(username))
                        .collect(Collectors.toList()));
    }

    private static void log(TopicConnection topic, String message) {
        MainView.<String> updateMaps(topic.getNamedMap(ACTIVITY_LOG_MAP_NAME),
                "", "", oldLog -> message + "\n" + oldLog);
    }

    private static <T> void updateMaps(CollaborativeMap map, String key,
            T nullValue, Function<T, T> updater) {
        while (true) {
            T oldValue = (T) map.get(key);
            T newValue = updater.apply(oldValue != null ? oldValue : nullValue);
            if (map.replace(key, oldValue, newValue)) {
                return;
            }
        }
    }

    private static void logEditorFocused(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " started editing " + fieldName;
        log(topicConnection, message);
    }

    private static void logEditorBlurred(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " stopped editing " + fieldName;
        log(topicConnection, message);
    }

    @SuppressWarnings("unchecked")
    private void updateEditors(Object value, String username) {
        List<String> editors = (List<String>) value;
        collaboratorsAvatars.setItems(
                editors.stream().filter(name -> !username.equals(name))
                        .map(AvatarGroup.AvatarGroupItem::new)
                        .collect(Collectors.toList()));
    }
}
