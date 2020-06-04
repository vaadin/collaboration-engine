package com.vaadin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static class Broadcaster {
        public static final Broadcaster INSTANCE = new Broadcaster();

        private final Map<String, Consumer<CollaborationState>> subscribers = new HashMap<>();

        private CollaborationState state = new CollaborationState(
                Collections.emptyMap(), Collections.emptyList(), "");

        public synchronized Registration addSubscriber(String name,
                Consumer<CollaborationState> subscriber) {
            subscribers.put(name, subscriber);

            updateState(state -> new CollaborationState(state.fieldStates,
                    Stream.concat(state.editors.stream(), Stream.of(name)),
                    name + " joined\n" + state.activityLog));

            return () -> removeSubscriber(name);
        }

        private synchronized void removeSubscriber(String name) {
            subscribers.remove(name);

            updateState(state -> new CollaborationState(state.fieldStates,
                    state.editors.stream().filter(value -> !name.equals(value)),
                    name + " left\n" + state.activityLog));
        }

        public synchronized void updateState(
                Function<CollaborationState, CollaborationState> updater) {
            // Run update logic while holding the lock
            CollaborationState state = updater.apply(this.state);

            this.state = state;

            subscribers.values().forEach(subscriber -> {
                try {
                    subscriber.accept(state);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        public void updateFieldState(String fieldName, String logMessage,
                Function<FieldState, FieldState> updater) {
            updateState(state -> {
                HashMap<String, FieldState> newStates = new HashMap<>(
                        state.fieldStates);

                FieldState oldFieldState = newStates.getOrDefault(fieldName,
                        EMPTY_FIELD_STATE);

                newStates.put(fieldName, updater.apply(oldFieldState));

                return new CollaborationState(newStates, state.editors,
                        logMessage + "\n" + state.activityLog);
            });
        }
    }

    private final Binder<Person> binder = new Binder<>(Person.class);

    private AvatarGroup collaboratorsAvatars = new AvatarGroup();
    private AvatarGroup ownAvatar = new AvatarGroup();

    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");

    private final Div log = new Div();

    public MainView() {
        addClassName("centered-content");
        collaboratorsAvatars.addClassName("collaborators-avatars");
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
        Person person = new Person();

        binder.setBean(person);

        binder.bindInstanceFields(this);

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

        firstName.addFocusListener(event -> setEditor("firstName", username));
        lastName.addFocusListener(event -> setEditor("lastName", username));

        firstName.addBlurListener(event -> clearEditor("firstName", username));
        lastName.addBlurListener(event -> clearEditor("lastName", username));

        firstName.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                submitValue("firstName", username, event.getValue());
            }
        });
        lastName.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                submitValue("lastName", username, event.getValue());
            }
        });

        /*
         * Tie subscription to submit button so that it's removed when detaching
         * the form
         */
        submitButton.getElement().getNode().runWhenAttached(ui -> {
            Registration addRegistration = Broadcaster.INSTANCE.addSubscriber(
                    username,
                    state -> ui.access(() -> showState(username, state)));

            submitButton.addDetachListener(event -> {
                addRegistration.remove();
                event.unregisterListener();
            });
        });
    }

    private static void setEditor(String fieldName, String username) {
        String message = username + " started editing " + fieldName;
        Broadcaster.INSTANCE.updateFieldState(fieldName, message, state -> {
            return new FieldState(state.value,
                    Stream.concat(state.editors.stream(), Stream.of(username)));
        });
    }

    private static void clearEditor(String fieldName, String username) {
        String message = username + " stopped editing " + fieldName;
        Broadcaster.INSTANCE.updateFieldState(fieldName, message, state -> {
            return new FieldState(state.value, state.editors.stream()
                    .filter(editor -> !username.equals(editor)));
        });
    }

    private static void submitValue(String fieldName, String username,
            Object value) {
        String message = username + " changed " + fieldName + " to " + value;
        Broadcaster.INSTANCE.updateFieldState(fieldName, message, state -> {
            return new FieldState(value, state.editors);
        });
    }

    @SuppressWarnings("unchecked")
    private void showState(String username, CollaborationState state) {
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
