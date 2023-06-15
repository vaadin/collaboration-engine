package com.vaadin.collaborationengine;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.collaborationengine.util.AbstractCollaborativeFormTestCommon;
import com.vaadin.flow.component.avatar.testbench.AvatarGroupElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBenchTestCase;

import static org.junit.Assert.assertFalse;

public class InvalidLicenseIT extends AbstractCollaborativeFormTestCommon {

    @Test
    public void tamperedLicense_openViews_errorMessageRendered_componentsNot()
            throws Exception {
        assertViewBroken(this);

        Client anotherClient = addClient();
        assertViewBroken(anotherClient);
    }

    private void assertViewBroken(TestBenchTestCase client) {
        String pageText = client.$("body").first()
                .getPropertyString("innerText");
        Assert.assertThat(pageText, CoreMatchers.containsString(
                "The content of the license property or file is not valid"));

        String msg = "Expected the view to be broken and no components to be rendered.";
        assertFalse(msg, client.$(AvatarGroupElement.class).exists());
        assertFalse(msg, client.$(TextFieldElement.class).exists());
    }

    @Override
    public void init() {
        // Overriding to avoid the default querying of elements
    }

    @Override
    public void reset() {
        // Overriding to avoid the default behavior
    }

}
