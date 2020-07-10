package org.vaadin.collaborationengine.it;

import java.util.concurrent.atomic.AtomicInteger;

import org.vaadin.collaborationengine.it.util.BeaconHandler;
import org.vaadin.collaborationengine.it.util.Person;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeBinder;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Push
@Route("binder")
public class CollaborativeBinderView extends VerticalLayout {

    private TextField textField = new TextField("Name");
    private Checkbox checkbox = new Checkbox("Married");

    private static AtomicInteger userCounter = new AtomicInteger(0);
    private NativeButton resetUserCounter = new NativeButton(
            "Reset user counter", e -> userCounter.set(0));

    public CollaborativeBinderView() {
        add(textField, checkbox, resetUserCounter);
        resetUserCounter.setId("reset-user-counter");

        CollaborationEngine.getInstance().openTopicConnection(this,
                CollaborativeBinderView.class.getName(),
                this::configureTopicConnection);

        BeaconHandler.ensureInstalled(this);
    }

    private Registration configureTopicConnection(
            TopicConnection topicConnection) {
        CollaborativeBinder<Person> binder = new CollaborativeBinder<>(
                Person.class, topicConnection.getNamedMap("binder"));
        binder.bind(textField, "name");
        binder.bind(checkbox, "married");

        binder.setUserName("User " + userCounter.incrementAndGet());
        return null;
    }

}
