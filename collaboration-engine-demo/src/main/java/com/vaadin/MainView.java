package com.vaadin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeMap;
import com.vaadin.collaborationengine.MapChangeEvent;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("")
@Push
@CssImport("./styles/shared-styles.css")
@JsModule("./field-collaboration.js")
public class MainView extends VerticalLayout {

    private static final String EDITORS = "editors";
    private static final String EDITORS_POSTFIX = "." + EDITORS;

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

    private Binder<Person> binder;

    private AvatarGroup collaboratorsAvatars = new AvatarGroup();
    private AvatarGroup ownAvatar = new AvatarGroup();

    private final Div log = new Div();

    public MainView() {
        addClassName("centered-content");
        collaboratorsAvatars.addClassName("collaborators-avatars");
        log.setClassName("log");
        showLogin();

        BeaconHandler.ensureInstalled(this);
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
        Person person = new Person();

        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");

        binder = new Binder<>(Person.class);
        binder.forField(firstName).bind(FIRST_NAME);
        binder.forField(lastName).bind(LAST_NAME);
        binder.setBean(person);

        removeAll();
        Button submitButton = new Button("Submit", event -> {
            Notification.show("Submit: " + person);
            showLogin();
        });

        ownAvatar.setItems(
                Arrays.asList(new AvatarGroup.AvatarGroupItem(username)));

        HorizontalLayout avatarGroups = new HorizontalLayout(
                collaboratorsAvatars, ownAvatar);
        avatarGroups.setWidthFull();
        avatarGroups.setSpacing(false);
        avatarGroups.setJustifyContentMode(JustifyContentMode.BETWEEN);

        add(avatarGroups, firstName, lastName, submitButton, log);

        /*
         * Tie connection to submit button so that it's deactivated when
         * detaching the form
         */
        CollaborationEngine.getInstance().openTopicConnection(submitButton,
                "form", topic -> configureTopicConnection(topic, username,
                        firstName, lastName));
    }

    private Registration configureTopicConnection(TopicConnection topic,
            String username, TextField firstName, TextField lastName) {
        Registration firstNameFocusRegistration = firstName.addFocusListener(
                event -> setEditor(topic, FIRST_NAME, username));
        Registration lastNameFocusRegistration = lastName.addFocusListener(
                event -> setEditor(topic, LAST_NAME, username));

        Registration firstNameBlurRegistration = firstName.addBlurListener(
                event -> clearEditor(topic, FIRST_NAME, username));
        Registration lastNameBlurRegistration = lastName.addBlurListener(
                event -> clearEditor(topic, LAST_NAME, username));

        Registration firstNameValueChangeRegistration = firstName
                .addValueChangeListener(event -> {
                    if (event.isFromClient()) {
                        submitValue(topic, FIRST_NAME, username,
                                event.getValue());
                    }
                });
        Registration lastNameValueChangeRegistration = lastName
                .addValueChangeListener(event -> {
                    if (event.isFromClient()) {
                        submitValue(topic, LAST_NAME, username,
                                event.getValue());
                    }
                });

        CollaborativeMap map = topic.getMap();
        map.subscribe(event -> updateState(event, username));
        addEditor(map, EDITORS, username);
        log(map, username + " joined");

        Registration editorRegistration = () -> {
            removeEditor(map, EDITORS, username);
            log(map, username + " left");
        };

        return Registration.combine(firstNameFocusRegistration,
                lastNameFocusRegistration, firstNameBlurRegistration,
                lastNameBlurRegistration, firstNameValueChangeRegistration,
                lastNameValueChangeRegistration, editorRegistration);
    }

    private static void removeEditor(CollaborativeMap map, String key,
            String username) {
        MainView.<List<String>> updateState(map, key, Collections.emptyList(),
                oldEditors -> oldEditors.stream()
                        .filter(value -> !username.equals(value))
                        .collect(Collectors.toList()));
    }

    private static void addEditor(CollaborativeMap map, String key,
            String username) {
        MainView.<List<String>> updateState(map, key, Collections.emptyList(),
                oldEditors -> Stream
                        .concat(oldEditors.stream(), Stream.of(username))
                        .collect(Collectors.toList()));
    }

    private static void log(CollaborativeMap map, String message) {
        MainView.<String> updateState(map, "activityLog", "",
                oldLog -> message + "\n" + oldLog);
    }

    private static <T> void updateState(CollaborativeMap map, String key,
            T nullValue, Function<T, T> updater) {
        while (true) {
            T oldValue = (T) map.get(key);
            T newValue = updater.apply(oldValue != null ? oldValue : nullValue);
            if (map.replace(key, oldValue, newValue)) {
                return;
            }
        }
    }

    private static void setEditor(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " started editing " + fieldName;
        addEditor(topicConnection.getMap(), fieldName + EDITORS_POSTFIX,
                username);
        log(topicConnection.getMap(), message);
    }

    private static void clearEditor(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " stopped editing " + fieldName;

        removeEditor(topicConnection.getMap(), fieldName + EDITORS_POSTFIX,
                username);
        log(topicConnection.getMap(), message);
    }

    private static void submitValue(TopicConnection topicConnection,
            String fieldName, String username, Object value) {
        String message = username + " changed " + fieldName + " to " + value;

        topicConnection.getMap().put(fieldName + ".value", value);

        log(topicConnection.getMap(), message);
    }

    @SuppressWarnings("unchecked")
    private void updateState(MapChangeEvent event, String username) {
        Object value = event.getNewValue();
        String key = event.getKey();

        if (EDITORS.equals(key)) {
            List<String> editors = (List<String>) value;
            collaboratorsAvatars.setItems(
                    editors.stream().filter(name -> !username.equals(name))
                            .map(AvatarGroup.AvatarGroupItem::new)
                            .collect(Collectors.toList()));
        } else if ("activityLog".contentEquals(key)) {
            log.setText(Objects.toString(value, ""));
        } else if (key.endsWith(".value")) {
            String propertyName = key.substring(0, key.indexOf('.'));
            binder.getBinding(propertyName).ifPresent(binding -> {
                @SuppressWarnings("rawtypes")
                HasValue field = binding.getField();

                if (value == null) {
                    field.clear();
                } else {
                    field.setValue(value);
                }
            });
        } else if (key.endsWith(EDITORS_POSTFIX)) {
            String propertyName = key.substring(0, key.indexOf('.'));

            binder.getBinding(propertyName).ifPresent(binding -> {
                HasValue<?, ?> field = binding.getField();
                if (field instanceof HasElement) {
                    HasElement component = (HasElement) field;

                    List<String> fieldEditors = (List<String>) value;

                    String effectiveEditor = fieldEditors.stream()
                            .filter(editor -> !username.equals(editor))
                            .findFirst().orElse(null);

                    component.getElement().executeJs(
                            "window.setFieldState(this, $0)", effectiveEditor);
                }
            });
        } else {
            throw new UnsupportedOperationException("Unknown map key: " + key);
        }
    }
}
