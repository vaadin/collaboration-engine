package com.vaadin.collaborationengine;

import com.vaadin.flow.component.html.Span;
import org.springframework.beans.factory.annotation.Autowired;

public class CollaborativeFormView extends CommonCollaborativeFormView {
    public CollaborativeFormView(@Autowired GreetService greetService) {
        super();
        Span span = new Span(greetService.getText());
        add(span);
    }
}
