package com.vaadin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Code snippets used in CollaborationBinder's reference documentation.
 */
public class BinderDocumentation {

    private static class Person extends TutorialView.Person {
        private long id;
        private MaritalStatus maritalStatus;
        private Person supervisor;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public MaritalStatus getMaritalStatus() {
            return maritalStatus;
        }

        public void setMaritalStatus(MaritalStatus maritalStatus) {
            this.maritalStatus = maritalStatus;
        }

        public Person getSupervisor() {
            return supervisor;
        }

        public void setSupervisor(Person supervisor) {
            this.supervisor = supervisor;
        }
    }

    private enum MaritalStatus {
        SINGLE, MARRIED;
    }

    private static class PersonService {
        private Person findById(long id) {
            return new Person();
        }

        private List<Person> findAllSupervisors() {
            return new ArrayList<>();
        }
    }

    private UserService userService;
    private PersonService personService;
    private CollaborationBinder<Person> binder;

    public BinderDocumentation() {
        User userEntity = userService.getCurrentUser();

        UserInfo userInfo = new UserInfo(userEntity.getId(),
                userEntity.getName());

        CollaborationBinder<Person> binder = new CollaborationBinder<>(
                Person.class, userInfo);

        TextField name = new TextField();
        binder.forField(name).bind("name");

        ComboBox<Person> supervisor = new ComboBox<>();
        supervisor.setItems(personService.findAllSupervisors());

        binder.setSerializer(Person.class,
                person -> String.valueOf(person.getId()),
                id -> personService.findById(Long.parseLong(id)));

        binder.bind(supervisor, "supervisor");

        CheckboxGroup<String> pets = new CheckboxGroup<>();
        pets.setItems("Dog", "Cat", "Parrot");

        binder.forField(pets, Set.class, String.class).bind("pets");

        Checkbox married = new Checkbox();
        binder.forField(married, Boolean.class)
                .withConverter(
                        fieldValue -> fieldValue ? MaritalStatus.MARRIED
                                : MaritalStatus.SINGLE,
                        MaritalStatus.MARRIED::equals)
                .bind("maritalStatus");
    }

    public void personSelected(long personId) {
        binder.setTopic("person/" + personId,
                () -> personService.findById(personId));
    }
}
