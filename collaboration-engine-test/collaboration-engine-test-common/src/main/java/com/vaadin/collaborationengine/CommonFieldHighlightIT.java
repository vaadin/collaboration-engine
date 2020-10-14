package com.vaadin.collaborationengine;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormIT;
import com.vaadin.collaborationengine.util.FieldOutlineElement;
import com.vaadin.flow.component.radiobutton.testbench.RadioButtonElement;

public class CommonFieldHighlightIT extends AbstractCollaborativeFormIT {

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

    @Test
    public void focusRadioButtonsInsideGroup_individualButtonsHighlighted() {
        ClientState client2 = new ClientState(addClient());

        // No highlight initially
        assertNoUserTags(client1.radioButtonGroup, client2.radioButtonGroup);
        assertRadioButtonHighlight(client1, null, null, null);
        assertRadioButtonHighlight(client2, null, null, null);

        // Client focuses radio button
        client2.focusRadioButton(1);

        assertUserTags(client1.radioButtonGroup, "User 2");
        assertNoUserTags(client2.radioButtonGroup);

        assertRadioButtonHighlight(client1, null, 1, null);
        assertRadioButtonHighlight(client2, null, null, null);

        // Two clients focus different radio buttons in a group
        ClientState client3 = new ClientState(addClient());
        client3.focusRadioButton(2);

        assertUserTags(client1.radioButtonGroup, "User 3", "User 2");
        assertUserTags(client2.radioButtonGroup, "User 3");
        assertUserTags(client3.radioButtonGroup, "User 2");

        assertRadioButtonHighlight(client1, null, 1, 2);
        assertRadioButtonHighlight(client2, null, null, 2);
        assertRadioButtonHighlight(client3, null, 1, null);

        // Client blurs radio button
        client3.blur();

        assertUserTags(client1.radioButtonGroup, "User 2");
        assertNoUserTags(client2.radioButtonGroup);
        assertUserTags(client3.radioButtonGroup, "User 2");

        assertRadioButtonHighlight(client1, null, 1, null);
        assertRadioButtonHighlight(client2, null, null, null);
        assertRadioButtonHighlight(client3, null, 1, null);

        // Two clients focus the same radio button
        client3.focusRadioButton(1);

        assertUserTags(client1.radioButtonGroup, "User 3", "User 2");
        assertUserTags(client2.radioButtonGroup, "User 3");
        assertUserTags(client3.radioButtonGroup, "User 2");

        assertRadioButtonHighlight(client1, null, 2, null);
        assertRadioButtonHighlight(client2, null, 2, null);
        assertRadioButtonHighlight(client3, null, 1, null);
    }

    @Test
    public void clientClearItsBinder_itsFieldsHaveNoHighlighters() {
        ClientState client2 = new ClientState(addClient());
        client2.focusTextField();
        assertUserTags(client1.textField, "User 2");

        client1.clearBinder();
        assertNoUserTags(client1.textField);

        client1.rebind();
        assertUserTags(client1.textField, "User 2");

        client2.focusCheckbox();
        assertNoUserTags(client1.textField);
        assertUserTags(client1.checkbox, "User 2");
    }

    private void assertRadioButtonHighlight(ClientState client,
            Integer... expectedColorIndices) {
        int index = 0;
        for (RadioButtonElement radioButton : client.radioButtons) {
            FieldOutlineElement outline = radioButton
                    .$(FieldOutlineElement.class).first();
            Integer colorIndex = outline.getColorIndex();
            Assert.assertEquals("Radio button had unexpected color index",
                    expectedColorIndices[index++], colorIndex);
        }
    }
}
