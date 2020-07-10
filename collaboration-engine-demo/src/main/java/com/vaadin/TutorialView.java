package com.vaadin;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeBinder;
import com.vaadin.collaborationengine.CollaborativeMap;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Push
// Use "" instead in the tutorial text to make it open for /
@Route("tutorial")
public class TutorialView extends VerticalLayout {

    private final Checkbox checkbox;

    public static class Person {
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    public TutorialView() {
        checkbox = new Checkbox("Is it Friday?");
        add(checkbox);

        CollaborationEngine.getInstance().openTopicConnection(this, "tutorial",
                topic -> {
                    CollaborativeMap fieldValues = topic
                            .getNamedMap("fieldValues");

                    Registration registration = checkbox
                            .addValueChangeListener(valueChangeEvent -> {
                                fieldValues.put("isFriday",
                                        valueChangeEvent.getValue());
                            });

                    fieldValues.subscribe(event -> {
                        if ("isFriday".equals(event.getKey())) {
                            checkbox.setValue(
                                    Boolean.TRUE.equals(event.getValue()));
                        }
                    });

                    return registration;
                });

        TextField textField = new TextField("First name");
        add(textField);

        CollaborationEngine.getInstance().openTopicConnection(this, "profile",
                topic -> {
                    CollaborativeBinder<Person> binder = new CollaborativeBinder<>(
                            Person.class, topic.getNamedMap("binder"));
                    binder.forField(textField).bind("firstName");
                    binder.setUserName("Jon Doe");
                    return null;
                });
    }
}
