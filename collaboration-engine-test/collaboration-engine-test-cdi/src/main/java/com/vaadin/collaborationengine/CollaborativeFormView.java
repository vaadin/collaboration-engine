package com.vaadin.collaborationengine;

import com.vaadin.flow.component.html.Span;
import javax.inject.Inject;

public class CollaborativeFormView extends CommonCollaborativeFormView {

    @Inject
    public CollaborativeFormView(GreetService greetService) {
        super();
        Span span = new Span(greetService.getText());
        add(span);
    }

}
