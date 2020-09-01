package com.vaadin.collaborationengine;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainViewIT extends AbstractCollaborativeViewTest {

    @Test
    public void clickingButtonUpdateSpan() {
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
    }

    @Override
    public String getRoute() {
        return "";
    }
}
