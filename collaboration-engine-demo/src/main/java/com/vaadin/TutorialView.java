package com.vaadin;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Push
// Use "" instead in the tutorial text to make it open for /
@Route("tutorial")
public class TutorialView extends VerticalLayout {
    private final TopicConnection topic = CollaborationEngine.getInstance()
            .openTopicConnection("tutorial");

    private final Checkbox checkbox;

    public TutorialView() {
        checkbox = new Checkbox("Is it Friday?");

        checkbox.addValueChangeListener(valueChangeEvent -> {
            topic.setValue(valueChangeEvent.getValue());
        });

        add(checkbox);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Registration registration = topic.subscribe(value -> {
            boolean friday = Boolean.TRUE.equals(value);
            attachEvent.getUI().access(() -> checkbox.setValue(friday));
        });

        addDetachListener(detachEvent -> {
            detachEvent.unregisterListener();
            registration.remove();
        });
    }
}
