package com.vaadin;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

@Route("")
@CssImport("./styles/shared-styles.css")
public class MainView extends VerticalLayout {
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

    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");

    public MainView() {
        addClassName("centered-content");

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
        
        add(usernameField, startButton);
    }

    private void showPersonEditor(String username) {
        Person person = new Person();

        Binder<Person> binder = new Binder<>(Person.class);
        binder.setBean(person);

        binder.bindInstanceFields(this);

        removeAll();
        add(new Text("Editing as " + username), firstName, lastName, new Button(
                "Submit", event -> Notification.show("Submit: " + person)));
    }
}
