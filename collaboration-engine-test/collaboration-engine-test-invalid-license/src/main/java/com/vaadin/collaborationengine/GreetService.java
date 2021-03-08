package com.vaadin.collaborationengine;

import java.io.Serializable;

import org.springframework.stereotype.Service;

@Service
public class GreetService implements Serializable {

    public String getText() {
        return "Hello from Spring!";
    }

}
