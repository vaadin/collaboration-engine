package org.vaadin.collaborationengine.it;

import static org.vaadin.collaborationengine.it.util.FieldHighlightUtil.assertNoUserTags;
import static org.vaadin.collaborationengine.it.util.FieldHighlightUtil.assertUserTags;

import org.junit.Test;

public class FieldHighlightIT extends AbstractCollaborativeFormIT {

    @Test
    public void noInitialUserTags() {
        assertNoUserTags(client1.textField, client1.checkbox);
    }

    @Test
    public void focusFields_userTagsForLocalUserNotDisplayed() {
        client1.focusTextField();
        assertNoUserTags(client1.textField, client1.checkbox);

        client1.focusCheckbox();
        assertNoUserTags(client1.textField, client1.checkbox);
    }

    @Test
    public void focusField_addClient_newClientHasUserTag() {
        client1.focusTextField();
        ClientState client2 = new ClientState(addClient());
        assertUserTags(client2.textField, "User 1");
        assertNoUserTags(client1.textField, client1.checkbox, client2.checkbox);
    }

    @Test
    public void addClient_focusAndBlurFields_userTagsUpdated() {
        ClientState client2 = new ClientState(addClient());

        client1.focusTextField();
        client2.focusCheckbox();

        assertUserTags(client2.textField, "User 1");
        assertUserTags(client1.checkbox, "User 2");
        assertNoUserTags(client1.textField, client2.checkbox);

        client1.focusCheckbox();

        assertUserTags(client2.checkbox, "User 1");
        assertUserTags(client1.checkbox, "User 2");
        assertNoUserTags(client1.textField, client2.textField);

        client2.blur();

        assertUserTags(client2.checkbox, "User 1");
        assertNoUserTags(client1.textField, client1.checkbox,
                client2.textField);
    }

    @Test
    public void threeClients_focusSameField_twoUserTags() {
        ClientState client2 = new ClientState(addClient());
        ClientState client3 = new ClientState(addClient());

        client1.focusTextField();
        client2.focusTextField();
        client3.focusTextField();

        assertUserTags(client1.textField, "User 3", "User 2");
        assertUserTags(client2.textField, "User 3", "User 1");
        assertUserTags(client3.textField, "User 2", "User 1");

        client3.blur();

        assertUserTags(client1.textField, "User 2");
        assertUserTags(client2.textField, "User 1");
        assertUserTags(client3.textField, "User 2", "User 1");

    }

    @Test
    public void closeBrowser_tagRemoved() {
        ClientState client2 = new ClientState(addClient());
        client2.focusTextField();
        close(client2.client);

        assertNoUserTags(client1.textField);
    }
}
