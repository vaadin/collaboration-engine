package com.vaadin.collaborationengine.util;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;

@Tag("test")
public class TestComponent extends Component {
    public boolean hasAttachListener() {
        return hasListener(AttachEvent.class);
    }

    public boolean hasDetachListener() {
        return hasListener(DetachEvent.class);
    }
}
