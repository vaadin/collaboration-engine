/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.collaborationengine;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;

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
