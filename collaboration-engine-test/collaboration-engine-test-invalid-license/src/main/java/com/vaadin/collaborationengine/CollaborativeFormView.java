package com.vaadin.collaborationengine;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.html.Span;

public class CollaborativeFormView extends CollaborativeFormViewCommon {
    public CollaborativeFormView(@Autowired GreetService greetService) {
        super();
        Span span = new Span(greetService.getText());
        add(span);
    }
}
