package com.vaadin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationEngine;
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
    private static final CollaborationState EMPTY_COLLABORATION_STATE = new CollaborationState(
            Collections.emptyMap(), Collections.emptyList(), "");
    private static final FieldState EMPTY_FIELD_STATE = new FieldState(null,
            Collections.emptyList());

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

    public static final class FieldState {
        public final Object value;
        public final List<String> editors;

        public FieldState(Object value, List<String> editors) {
            this.value = value;
            this.editors = Collections
                    .unmodifiableList(new ArrayList<>(editors));
        }

        public FieldState(Object value, Stream<String> editors) {
            this.value = value;
            this.editors = Collections
                    .unmodifiableList(editors.collect(Collectors.toList()));
        }
    }

    public static class CollaborationState {
        public final Map<String, FieldState> fieldStates;
        public final List<String> editors;
        public final String activityLog;

        public CollaborationState(Map<String, FieldState> fieldStates,
                List<String> editors, String activityLog) {
            this.fieldStates = Collections
                    .unmodifiableMap(new HashMap<>(fieldStates));
            this.editors = Collections
                    .unmodifiableList(new ArrayList<>(editors));
            this.activityLog = activityLog;
        }

        public CollaborationState(Map<String, FieldState> fieldStates,
                Stream<String> editors, String activityLog) {
            this.fieldStates = Collections
                    .unmodifiableMap(new HashMap<>(fieldStates));
            this.editors = Collections
                    .unmodifiableList(editors.collect(Collectors.toList()));
            this.activityLog = activityLog;
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
        TopicConnection topic = CollaborationEngine.getInstance()
                .openTopicConnection(this, "form");

        Person person = new Person();

        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");

        binder = new Binder<>(Person.class);
        binder.forField(firstName).bind("firstName");
        binder.forField(lastName).bind("lastName");
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

        firstName.addFocusListener(
                event -> setEditor(topic, "firstName", username));
        lastName.addFocusListener(
                event -> setEditor(topic, "lastName", username));

        firstName.addBlurListener(
                event -> clearEditor(topic, "firstName", username));
        lastName.addBlurListener(
                event -> clearEditor(topic, "lastName", username));

        firstName.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                submitValue(topic, "firstName", username, event.getValue());
            }
        });
        lastName.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                submitValue(topic, "lastName", username, event.getValue());
            }
        });

        /*
         * Tie subscription to submit button so that it's removed when detaching
         * the form
         */
        submitButton.getElement().getNode().runWhenAttached(ui -> {
            Registration registration = topic.subscribe(
                    state -> showState(username, (CollaborationState) state));

            updateState(topic,
                    state -> new CollaborationState(state.fieldStates,
                            Stream.concat(state.editors.stream(),
                                    Stream.of(username)),
                            username + " joined\n" + state.activityLog));

            submitButton.addDetachListener(event -> {
                registration.remove();
                event.unregisterListener();

                updateState(topic,
                        state -> new CollaborationState(state.fieldStates,
                                state.editors.stream().filter(
                                        value -> !username.equals(value)),
                                username + " left\n" + state.activityLog));
            });
        });
    }

    private static void updateState(TopicConnection topicConnection,
            Function<CollaborationState, CollaborationState> updater) {
        while (true) {
            CollaborationState oldState = (CollaborationState) topicConnection
                    .getValue();
            CollaborationState newState = updater.apply(
                    oldState != null ? oldState : EMPTY_COLLABORATION_STATE);
            if (topicConnection.compareAndSet(oldState, newState)) {
                break;
            }
        }
    }

    private static void updateFieldState(TopicConnection topicConnection,
            String fieldName, String logMessage,
            Function<FieldState, FieldState> updater) {
        updateState(topicConnection, state -> {
            HashMap<String, FieldState> newStates = new HashMap<>(
                    state.fieldStates);

            FieldState oldFieldState = newStates.getOrDefault(fieldName,
                    EMPTY_FIELD_STATE);

            newStates.put(fieldName, updater.apply(oldFieldState));

            return new CollaborationState(newStates, state.editors,
                    logMessage + "\n" + state.activityLog);
        });
    }

    private static void setEditor(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " started editing " + fieldName;
        updateFieldState(topicConnection, fieldName, message, state -> {
            return new FieldState(state.value,
                    Stream.concat(state.editors.stream(), Stream.of(username)));
        });
    }

    private static void clearEditor(TopicConnection topicConnection,
            String fieldName, String username) {
        String message = username + " stopped editing " + fieldName;
        updateFieldState(topicConnection, fieldName, message, state -> {
            return new FieldState(state.value, state.editors.stream()
                    .filter(editor -> !username.equals(editor)));
        });
    }

    private static void submitValue(TopicConnection topicConnection,
            String fieldName, String username, Object value) {
        String message = username + " changed " + fieldName + " to " + value;
        updateFieldState(topicConnection, fieldName, message, state -> {
            return new FieldState(value, state.editors);
        });
    }

    @SuppressWarnings("unchecked")
    private void showState(String username, CollaborationState state) {
        if (state == null) {
            state = EMPTY_COLLABORATION_STATE;
        }

        collaboratorsAvatars.setItems(
                state.editors.stream().filter(name -> !username.equals(name))
                        .map(AvatarGroup.AvatarGroupItem::new)
                        .collect(Collectors.toList()));

        state.fieldStates.forEach((fieldName, fieldState) -> {
            binder.getBinding(fieldName).ifPresent(binding -> {
                @SuppressWarnings("rawtypes")
                HasValue field = binding.getField();
                if (fieldState.value == null) {
                    field.clear();
                } else {
                    field.setValue(fieldState.value);
                }

                if (field instanceof HasElement) {
                    HasElement component = (HasElement) field;

                    String effectiveEditor = fieldState.editors.stream()
                            .filter(editor -> !username.equals(editor))
                            .findFirst().orElse(null);

                    component.getElement().executeJs(
                            "window.setFieldState(this, $0)", effectiveEditor);
                }
            });
        });

        log.setText(state.activityLog);
    }
}
