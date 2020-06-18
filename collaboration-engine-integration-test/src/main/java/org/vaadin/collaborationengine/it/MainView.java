package org.vaadin.collaborationengine.it;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.util.Objects;

/**
 * The main view contains a button and a collaborative div which shows the
 * number of clicks
 */
@Route("")
@Push
public class MainView extends VerticalLayout {

    TopicConnection topic = CollaborationEngine.getInstance()
            .openTopicConnection(this, MainView.class.getName());

    private Button button;
    private Span span = new Span();

    public MainView() {
        button = new Button("Increase", e -> {
            Thread update = new Thread(() -> {
                Integer newState = (Integer) topic.getValue() + 1;
                topic.setValue(newState);
            });
            update.start();
        });

        if (topic.getValue() == null) {
            topic.setValue(0);
        }

        add(button, span);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Registration registration = topic.subscribe(
                newValue -> span.setText(Objects.toString(newValue)));

        addDetachListener(detachEvent -> {
            detachEvent.unregisterListener();
            registration.remove();
        });
    }
}
