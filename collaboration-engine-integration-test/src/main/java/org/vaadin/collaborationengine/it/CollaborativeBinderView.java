package org.vaadin.collaborationengine.it;

import java.util.concurrent.atomic.AtomicInteger;

import org.vaadin.collaborationengine.it.util.BeaconHandler;
import org.vaadin.collaborationengine.it.util.Person;

import com.vaadin.collaborationengine.CollaborativeBinder;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

@Push
@Route("binder")
public class CollaborativeBinderView extends VerticalLayout {

    private TextField textField = new TextField("Name");
    private Checkbox checkbox = new Checkbox("Married");

    private CollaborativeBinder<Person> binder;

    private static AtomicInteger userCounter = new AtomicInteger(0);
    private NativeButton resetUserCounter = new NativeButton(
            "Reset user counter", e -> userCounter.set(0));

    public CollaborativeBinderView() {
        add(textField, checkbox, resetUserCounter);
        resetUserCounter.setId("reset-user-counter");

        binder = new CollaborativeBinder<>(Person.class, "topic");
        binder.bind(textField, "name");
        binder.bind(checkbox, "married");

        binder.setUserName("User " + userCounter.incrementAndGet());

        BeaconHandler.ensureInstalled(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Workaround for blur not fired when closing tabs
        binder.getBinding("name").ifPresent(Binder.Binding::unbind);
        binder.getBinding("married").ifPresent(Binder.Binding::unbind);
    }
}
