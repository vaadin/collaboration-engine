package com.vaadin.collaborationengine.util;

import com.vaadin.collaborationengine.CommonCollaborativeFormView;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;

import com.vaadin.flow.component.avatar.testbench.AvatarElement;
import com.vaadin.flow.component.avatar.testbench.AvatarGroupElement;
import com.vaadin.flow.component.checkbox.testbench.CheckboxElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.TestBenchTestCase;

/**
 * Base class for tests that use the {@link CommonCollaborativeFormView}.
 */
public abstract class AbstractCollaborativeFormIT
        extends AbstractCollaborativeViewTest {

    @Override
    public String getRoute() {
        return "form";
    }

    public static class ClientState {
        public TestBenchTestCase client;
        public AvatarGroupElement avatars;
        public TextFieldElement textField;
        public TextFieldElement emailField;
        public CheckboxElement checkbox;

        TestBenchElement focusedElement;

        public ClientState(TestBenchTestCase client) {
            this.client = client;
            avatars = client.$(AvatarGroupElement.class).first();
            textField = client.$(TextFieldElement.class).first();
            emailField = client.$(TextFieldElement.class).id("emailField");
            checkbox = client.$(CheckboxElement.class).first();
        }

        /**
         * Note: Focus and blur is simulated with events, because otherwise
         * interacting with another browser would blur the focused field.
         */
        public void focusTextField() {
            blur();
            textField.dispatchEvent("focusin");
            focusedElement = textField;
        }

        public void focusCheckbox() {
            blur();
            checkbox.dispatchEvent("focusin");
            focusedElement = checkbox;
        }

        public void blur() {
            if (focusedElement != null) {
                focusedElement.dispatchEvent("focusout");
                focusedElement = null;
            }
        }

        public List<String> getAvatarNames() {
            return avatars.$(AvatarElement.class).all().stream()
                    .filter(avatar -> !avatar.hasAttribute("hidden"))
                    .map(avatar -> avatar.getPropertyString("name"))
                    .collect(Collectors.toList());
        }

        public void detachTextField() {
            click("detach-text-field");
        }

        public void attachTextField() {
            click("attach-text-field");
            this.textField = client.$(TextFieldElement.class).first();
        }

        public void click(String id) {
            client.$(TestBenchElement.class).id(id).click();
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
