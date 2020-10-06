package com.vaadin;

import java.time.LocalDate;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Push
// Use "" instead in the tutorial text to make it open for /
@Route("tutorial")
public class TutorialView extends VerticalLayout {

    public static class Person {
        private String name;
        private LocalDate dateOfBirth;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
    }

    public TutorialView() {
        TextField nameField = new TextField("Name");
        DatePicker dateOfBirthField = new DatePicker("Date of birth");
        add(nameField, dateOfBirthField);

        UserInfo localUser = new UserInfo("johndoe", "Jon Doe");

        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                localUser, "tutorial");
        addComponentAsFirst(avatarGroup);

        CollaborationBinder<Person> binder = new CollaborationBinder<>(
                Person.class, localUser);
        binder.forField(nameField).bind("name");
        binder.forField(dateOfBirthField).bind("dateOfBirth");
        binder.setTopic("tutorial", () -> new Person());
    }
}
