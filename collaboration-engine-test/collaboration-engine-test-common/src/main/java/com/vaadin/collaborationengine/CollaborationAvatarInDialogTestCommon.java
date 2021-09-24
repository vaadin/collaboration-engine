package com.vaadin.collaborationengine;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.collaborationengine.util.AbstractCollaborativeViewTest;
import com.vaadin.flow.component.dialog.testbench.DialogElement;
import com.vaadin.testbench.TestBenchElement;

public class CollaborationAvatarInDialogTestCommon
        extends AbstractCollaborativeViewTest {
    @Override
    public String getRoute() {
        return "dialog";
    }

    @Test
    public void whenAvatarGroupIsDetachedThenUserIsRemovedFromTopic() {
        $("vaadin-button").id("open").click();
        TestBenchElement users = $("div").id("users");
        waitUntil(driver -> !users.findElements(By.tagName("div")).isEmpty(),
                3);

        DialogElement dialogElement = $(DialogElement.class).first();
        dialogElement.setProperty("opened", false);
        waitUntil(driver -> users.findElements(By.tagName("div")).isEmpty(), 3);
    }

}
