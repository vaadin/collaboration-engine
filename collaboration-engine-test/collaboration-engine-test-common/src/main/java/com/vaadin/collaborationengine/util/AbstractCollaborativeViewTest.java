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
package com.vaadin.collaborationengine.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.openqa.selenium.WebDriver;

import com.vaadin.testbench.TestBenchTestCase;

/**
 * Helpers for adding multiple "collaborating" browsers to a test.
 */
public abstract class AbstractCollaborativeViewTest extends AbstractViewTest {

    /**
     * Web driver wrapper with the TestBench query methods.
     */
    public static class Client extends TestBenchTestCase {
    }

    protected List<Client> addedClients = new ArrayList<>();

    /**
     * Opens a new TestBench client. It has the {@link #$(Class)} method for
     * querying elements.
     *
     * @throws Exception
     */
    public Client addClient() throws Exception {
        WebDriver driver = createDriver();
        driver.get(getURL());

        Client client = new Client();
        client.setDriver(driver);
        addedClients.add(client);

        return client;
    }

    @After
    public void closeBrowsers() {
        new ArrayList<>(addedClients).forEach(this::close);
    }

    protected void close(TestBenchTestCase client) {
        if (isHeadless) {
            // This assures beacon request is sent before closing browser when
            // in headless mode
            client.getDriver().get("about:blank");
        }

        client.getDriver().close();
        addedClients.remove(client);

        if (isHub) {
            // In Selenium Hub we need to wait for a while.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }
}
