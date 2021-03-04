package com.vaadin.collaborationengine;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.collaborationengine.util.Person;
import com.vaadin.collaborationengine.util.Person.Diet;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;

@Route("form")
@PreserveOnRefresh
public class CommonCollaborativeFormView extends VerticalLayout {

    public static final String TOPIC_ID = "topic";

    CollaborationAvatarGroup avatars;

    TextField textField = new TextField("Name");
    Checkbox checkbox = new Checkbox("Married");
    TextField email = new TextField("Email (not collaborative)");
    RadioButtonGroup<Diet> radioButtonGroup = new RadioButtonGroup<>();

    CollaborationBinder<Person> binder;

    static AtomicInteger userCounter = new AtomicInteger(0);
    NativeButton resetUserCounter = new NativeButton("Reset user counter",
            e -> userCounter.set(0));

    public CommonCollaborativeFormView() {
        addAttachListener(event -> event.getUI().getPushConfiguration()
                .setPushMode(PushMode.AUTOMATIC));
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        avatars = new CollaborationAvatarGroup(localUser, TOPIC_ID);

        radioButtonGroup.setItems(Diet.values());
        radioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioButtonGroup.setLabel("Diet");

        resetUserCounter.setId("reset-user-counter");
        email.setId("emailField");

        add(avatars, textField, checkbox, radioButtonGroup, resetUserCounter,
                email);

        binder = new CollaborationBinder<>(Person.class, localUser);
        binder.setTopic(TOPIC_ID, () -> null);
        binder.bind(textField, "name");
        binder.bind(checkbox, "married");
        binder.bind(radioButtonGroup, "diet");

        NativeButton detachTextField = new NativeButton("Detach text field",
                e -> remove(textField));
        detachTextField.setId("detach-text-field");
        NativeButton attachTextField = new NativeButton("Attach text field",
                e -> addComponentAtIndex(1, textField));
        attachTextField.setId("attach-text-field");

        NativeButton setBinderNull = new NativeButton(
                "Set binder topic to null",
                e -> binder.setTopic(null, Person::new));
        setBinderNull.setId("set-binder-null");
        NativeButton setBinder = new NativeButton("Bind again",
                e -> binder.setTopic(TOPIC_ID, Person::new));
        setBinder.setId("set-binder");

        add(detachTextField, attachTextField, setBinderNull, setBinder);
    }

}
