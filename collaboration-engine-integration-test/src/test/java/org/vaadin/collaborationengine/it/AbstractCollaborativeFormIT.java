package org.vaadin.collaborationengine.it;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.vaadin.collaborationengine.it.util.AbstractCollaborativeViewTest;

import com.vaadin.flow.component.avatar.testbench.AvatarElement;
import com.vaadin.flow.component.avatar.testbench.AvatarGroupElement;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.TestBenchTestCase;

/**
 * Base class for tests that use the {@link CollaborativeFormView}.
 */
public abstract class AbstractCollaborativeFormIT
        extends AbstractCollaborativeViewTest {

    @Override
    public String getRoute() {
        return "form";
    }

    public static class ClientState {
        TestBenchTestCase client;
        AvatarGroupElement avatars;
        TextFieldElement textField;
        CheckboxElement checkbox;

        TestBenchElement focusedElement;

        ClientState(TestBenchTestCase client) {
            this.client = client;
            avatars = client.$(AvatarGroupElement.class).first();
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

        List<String> getAvatarNames() {
            return avatars.$(AvatarElement.class).all().stream()
                    .filter(avatar -> !avatar.hasAttribute("hidden"))
                    .map(avatar -> avatar.getPropertyString("name"))
                    .collect(Collectors.toList());
        }

    }

    protected ClientState client1;

    @Before
    public void init() {
        client1 = new ClientState(this);
    }

    @After
    public void reset() {
        $("button").id("reset-user-counter").click();
    }
}
