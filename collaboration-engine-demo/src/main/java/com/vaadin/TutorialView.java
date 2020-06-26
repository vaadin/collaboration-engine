package com.vaadin;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeMap;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Push
// Use "" instead in the tutorial text to make it open for /
@Route("tutorial")
public class TutorialView extends VerticalLayout {

    private final Checkbox checkbox;

    public TutorialView() {
        checkbox = new Checkbox("Is it Friday?");
        add(checkbox);

        CollaborationEngine.getInstance().openTopicConnection(this, "tutorial",
                topic -> {
                    Registration registration = checkbox
                            .addValueChangeListener(valueChangeEvent -> {
                                CollaborativeMap map = topic.getMap();
                                map.put("value", valueChangeEvent.getValue());
                            });

                    topic.getMap().subscribe(event -> {
                        if ("value".equals(event.getKey())) {
                            boolean value = Boolean.TRUE
                                    .equals(event.getNewValue());
                            checkbox.setValue(value);
                        }
                    });

                    return registration;
                });
    }
}