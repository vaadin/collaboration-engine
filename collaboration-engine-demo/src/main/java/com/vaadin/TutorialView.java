package com.vaadin;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeMap;
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
            .openTopicConnection(this, "tutorial");

    private final Checkbox checkbox;

    public TutorialView() {
        checkbox = new Checkbox("Is it Friday?");

        checkbox.addValueChangeListener(valueChangeEvent -> {
            CollaborativeMap map = topic.getMap();
            map.put("value", valueChangeEvent.getValue());
        });

        add(checkbox);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        Registration registration = topic.getMap().subscribe(event -> {
            if ("value".equals(event.getKey())) {
                boolean value = Boolean.TRUE.equals(event.getNewValue());
                checkbox.setValue(value);
            }
        });

        addDetachListener(detachEvent -> {
            detachEvent.unregisterListener();
            registration.remove();
        });
    }
}
