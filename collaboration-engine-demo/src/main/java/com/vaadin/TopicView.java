package com.vaadin;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Push
// Use "" instead in the tutorial text to make it open for /
@Route("topic")
public class TopicView extends VerticalLayout {

    private final Checkbox checkbox;

    public TopicView() {
        checkbox = new Checkbox("Is it Friday?");
        add(checkbox);

        // TODO: replace hard-coded ID and name with data from the actual logged
        // in user
        UserInfo localUser = new UserInfo("johndoe", "John Doe");

        CollaborationEngine.getInstance().openTopicConnection(this, "tutorial",
                localUser, topic -> {
                    CollaborationMap fieldValues = topic
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
    }
}
