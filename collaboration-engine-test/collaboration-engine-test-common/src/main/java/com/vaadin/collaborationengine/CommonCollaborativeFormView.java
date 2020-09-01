package com.vaadin.collaborationengine;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.collaborationengine.util.Person;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

@Push
@Route("form")
@PreserveOnRefresh
public class CommonCollaborativeFormView extends VerticalLayout {

    public static final String TOPIC_ID = "topic";

    CollaborationAvatarGroup avatars;

    TextField textField = new TextField("Name");
    Checkbox checkbox = new Checkbox("Married");
    TextField email = new TextField("Email (not collaborative)");

    CollaborationBinder<Person> binder;

    static AtomicInteger userCounter = new AtomicInteger(0);
    NativeButton resetUserCounter = new NativeButton("Reset user counter",
            e -> userCounter.set(0));

    public CommonCollaborativeFormView() {
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        avatars = new CollaborationAvatarGroup(localUser, TOPIC_ID);

        resetUserCounter.setId("reset-user-counter");
        email.setId("emailField");
        add(avatars, textField, checkbox, resetUserCounter, email);

        binder = new CollaborationBinder<>(Person.class, localUser);
        binder.setTopic(TOPIC_ID, () -> null);
        binder.bind(textField, "name");
        binder.bind(checkbox, "married");

        NativeButton detachTextField = new NativeButton("Detach text field",
                e -> remove(textField));
        detachTextField.setId("detach-text-field");
        NativeButton attachTextField = new NativeButton("Attach text field",
                e -> addComponentAtIndex(1, textField));
        attachTextField.setId("attach-text-field");

        add(detachTextField, attachTextField);
    }

}
