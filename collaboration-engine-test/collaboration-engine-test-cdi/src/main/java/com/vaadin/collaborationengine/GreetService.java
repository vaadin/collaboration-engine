package com.vaadin.collaborationengine;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.annotation.VaadinServiceScoped;
import java.io.Serializable;
import javax.enterprise.inject.Default;

@VaadinServiceEnabled
@VaadinServiceScoped
@Default
public class GreetService implements Serializable {

    public String getText() {
        return "Hello from CDI!";
    }

}
