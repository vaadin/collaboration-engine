package com.vaadin.collaborationengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.Assert.fail;

public class MainViewIT extends AbstractCollaborativeViewTest {

    @Test
    public void clickingButtonUpdateSpan() throws Exception {
        ButtonElement button = $(ButtonElement.class).first();
        SpanElement span = $(SpanElement.class).first();

        button.click();
        waitUntil(ExpectedConditions.textToBePresentInElement(span, "1"), 1);
        Assert.assertEquals("1", span.getText());

        Client client2 = addClient();
        SpanElement span2 = client2.$(SpanElement.class).first();
        Assert.assertEquals(
                "Expected new client to get the current value when opened", "1",
                span2.getText());

        client2.$(ButtonElement.class).first().click();
        button.click();

        waitUntil(ExpectedConditions.textToBePresentInElement(span, "3"), 1);
        Assert.assertEquals("3", span.getText());
        Assert.assertEquals("3", span2.getText());

        try {
            ComponentConnectionContext context = new ComponentConnectionContext();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(context);

            ByteArrayInputStream in = new ByteArrayInputStream(
                    out.toByteArray());
            ComponentConnectionContext deserializedCompContext = (ComponentConnectionContext) new ObjectInputStream(
                    in).readObject();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Override
    public String getRoute() {
        return "";
    }
}
