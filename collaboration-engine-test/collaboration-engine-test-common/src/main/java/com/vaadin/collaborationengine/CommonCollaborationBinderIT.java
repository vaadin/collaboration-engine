package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormIT;

public class CommonCollaborationBinderIT extends AbstractCollaborativeFormIT {

    @Test
    public void fieldValuesSyncedAmongClients() {
        client1.textField.setValue("foo");

        ClientState client2 = new ClientState(addClient());
        Assert.assertEquals("foo", client2.textField.getValue());

        client2.textField.setValue("bar");
        Assert.assertEquals("bar", client1.textField.getValue());
    }
}
