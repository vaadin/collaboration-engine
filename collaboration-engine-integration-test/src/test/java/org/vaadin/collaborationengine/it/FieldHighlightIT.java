package org.vaadin.collaborationengine.it;

import static org.vaadin.collaborationengine.it.util.FieldHighlightUtil.assertNoUserTags;
import static org.vaadin.collaborationengine.it.util.FieldHighlightUtil.assertUserTags;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.collaborationengine.it.util.AbstractCollaborativeViewTest;

import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.TestBenchTestCase;

public class FieldHighlightIT extends AbstractCollaborativeViewTest {

    @Override
    public String getRoute() {
        return "binder";
    }

    private static class ClientState {
        TestBenchTestCase client;
        TextFieldElement textField;
        CheckboxElement checkbox;

        TestBenchElement focusedElement;

        ClientState(TestBenchTestCase client) {
            this.client = client;
            textField = client.$(TextFieldElement.class).first();
            checkbox = client.$(CheckboxElement.class).first();
        }

        /**
         * Note: Focus and blur is simulated with events, because otherwise
         * interacting with another browser would blur the focused field.
         */
        void focusTextField() {
            blur();
            textField.dispatchEvent("focus");
            focusedElement = textField;
        }

        void focusCheckbox() {
            blur();
            checkbox.dispatchEvent("focus");
            focusedElement = checkbox;
        }

        void blur() {
            if (focusedElement != null) {
                focusedElement.dispatchEvent("blur");
                focusedElement = null;
            }
        }

    }

    private ClientState client1;

    @Before
    public void init() {
        client1 = new ClientState(this);
    }

    @After
    public void reset() {
        $("button").id("reset-user-counter").click();
    }

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
