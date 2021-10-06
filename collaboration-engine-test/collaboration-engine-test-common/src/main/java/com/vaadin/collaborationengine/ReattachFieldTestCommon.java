package com.vaadin.collaborationengine;

import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormTestCommon;

public class ReattachFieldTestCommon
        extends AbstractCollaborativeFormTestCommon {

    @Test
    public void detachTextFields_attachTextFields_collaborationWorks() {
        ClientState client2 = new ClientState(addClient());

        client1.detachTextField();
        client2.detachTextField();

        client1.attachTextField();
        client2.attachTextField();

        client1.focusTextField();

        assertUserTags(client2.textField, "User 1");

        client1.textField.setValue("foo");
        // Value should be propagated to the other client
        waitUntil(driver -> "foo".equals(client2.textField.getValue()), 3);

        client1.blur();
        assertNoUserTags(client2.textField);
    }
}
