package com.vaadin.collaborationengine;

import com.vaadin.collaborationengine.util.Person;
import com.vaadin.flow.router.PreserveOnRefresh;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Push
@Route("form")
@PreserveOnRefresh
public class CollaborativeFormView extends VerticalLayout {

    public static final String TOPIC_ID = "topic";

    private CollaborativeAvatarGroup avatars;

    private TextField textField = new TextField("Name");
    private Checkbox checkbox = new Checkbox("Married");
    private TextField email = new TextField("Email (not collaborative)");

    private CollaborativeBinder<Person> binder;

    private static AtomicInteger userCounter = new AtomicInteger(0);
    private NativeButton resetUserCounter = new NativeButton(
            "Reset user counter", e -> userCounter.set(0));

    public CollaborativeFormView() {
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        avatars = new CollaborativeAvatarGroup(localUser, TOPIC_ID);

        resetUserCounter.setId("reset-user-counter");
        email.setId("emailField");
        add(avatars, textField, checkbox, resetUserCounter, email);

        binder = new CollaborativeBinder<>(Person.class, localUser, TOPIC_ID);
        binder.bind(textField, "name");
        binder.bind(checkbox, "married");
    }

}
