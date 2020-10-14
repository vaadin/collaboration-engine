package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormIT;

public class CommonReattachFieldIT extends AbstractCollaborativeFormIT {

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
        Assert.assertEquals(
                "TextField value was not propagated to the other client", "foo",
                client2.textField.getValue());

        client1.blur();
        assertNoUserTags(client2.textField);
    }
}
