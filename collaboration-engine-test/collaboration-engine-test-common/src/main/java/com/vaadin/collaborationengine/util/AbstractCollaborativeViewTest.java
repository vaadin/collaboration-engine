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
