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
     * Opens a new TestBench Chrome client. It has the {@link #$(Class)} method
     * for querying elements.
     */
    public Client addClient() {
        WebDriver driver = createHeadlessChromeDriver();
        driver.get(getURL());

        Client client = new Client();
        client.setDriver(driver);
        addedClients.add(client);

        return client;
    }

    @After
    public void closeBrowsers() {
        addedClients.stream().map(TestBenchTestCase::getDriver)
                .forEach(WebDriver::close);
    }

    protected void close(TestBenchTestCase client) {
        client.getDriver().close();
        addedClients.remove(client);
    }
}
