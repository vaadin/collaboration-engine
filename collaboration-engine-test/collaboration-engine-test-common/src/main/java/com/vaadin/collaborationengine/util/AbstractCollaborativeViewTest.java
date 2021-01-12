package com.vaadin.collaborationengine.util;

import com.vaadin.testbench.TestBenchTestCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.openqa.selenium.WebDriver;

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
        new ArrayList<>(addedClients).forEach(this::close);
        openBlankPage(this);
    }

    protected void close(TestBenchTestCase client) {
        openBlankPage(client);
        client.getDriver().close();
        addedClients.remove(client);
    }

    private void openBlankPage(TestBenchTestCase client) {
        /*
         * When closing the chromedriver, it does not correctly fire the
         * window's `unload` event, that would send a beacon request and e.g.
         * remove the user from CollaborationAvatarGroup. Opening a blank page
         * before closing the browser is a workaround for this.
         * https://bugs.chromium.org/p/chromedriver/issues/detail?id=3706
         */
        client.getDriver().get("about:blank");
    }

}
