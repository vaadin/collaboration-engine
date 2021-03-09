package com.vaadin.collaborationengine;

import javax.inject.Inject;

import com.vaadin.flow.component.html.Span;

public class CollaborativeFormView extends CollaborativeFormViewCommon {

    @Inject
    public CollaborativeFormView(GreetService greetService) {
        super();
        Span span = new Span(greetService.getText());
        add(span);
    }

}
