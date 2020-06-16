package org.vaadin.collaborationengine.it;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainViewIT extends AbstractViewTest {

    @Test
    public void clickingButtonUpdateSpan() {
        ButtonElement button = $(ButtonElement.class).first();
        SpanElement span = $(SpanElement.class).first();

        button.click();
        waitUntil(ExpectedConditions.textToBePresentInElement(span, "1"), 1);
        Assert.assertEquals("1", span.getText());

        button.click();
        button.click();
        waitUntil(ExpectedConditions.textToBePresentInElement(span, "3"), 1);
        Assert.assertEquals("3", span.getText());
    }

}
