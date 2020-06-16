package com.vaadin;

import java.io.IOException;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class BeaconHandler extends SynchronizedRequestHandler {
    private final UI ui;
    private final String beaconPath = "/beacon/" + UUID.randomUUID().toString();

    public BeaconHandler(UI ui) {
        this.ui = ui;
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return beaconPath.equals(request.getPathInfo());
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        ui.close();
        return true;
    }

    public static void ensureInstalled(Component component) {
        component.getElement().getNode()
                .runWhenAttached(BeaconHandler::ensureInstalledForUi);
    }

    private static void ensureInstalledForUi(UI ui) {
        if (ComponentUtil.getData(ui, BeaconHandler.class) != null) {
            // Already installed, nothing to do
            return;
        }

        BeaconHandler beaconHandler = new BeaconHandler(ui);

        // ./beacon/<random uuid>
        String relativeBeaconPath = "." + beaconHandler.beaconPath;

        // TODO Use synchronous XHR if Beacon cannot be used (IE11)?
        ui.getElement().executeJs(
                "window.addEventListener('unload', function() {navigator.sendBeacon && navigator.sendBeacon($0)})",
                relativeBeaconPath);

        VaadinSession session = ui.getSession();
        session.addRequestHandler(beaconHandler);
        ui.addDetachListener(
                detachEvent -> session.removeRequestHandler(beaconHandler));

        ComponentUtil.setData(ui, BeaconHandler.class, beaconHandler);
    }
}
