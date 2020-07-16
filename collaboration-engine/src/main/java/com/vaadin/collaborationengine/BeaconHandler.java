/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the license along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 */
package com.vaadin.collaborationengine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

/**
 * A {@link SynchronizedRequestHandler} which notifies its listeners when the
 * browser tab is closed.
 *
 * @author Vaadin Ltd
 */
class BeaconHandler extends SynchronizedRequestHandler {
    private final String beaconPath = "/beacon/" + UUID.randomUUID().toString();
    private final List<Command> listeners;

    public BeaconHandler() {
        listeners = new ArrayList<>();
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return beaconPath.equals(request.getPathInfo());
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) {
        new ArrayList<>(listeners).forEach(Command::execute);
        return true;
    }

    Registration addListener(Command listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    static BeaconHandler ensureInstalled(UI ui) {
        BeaconHandler beaconHandler = ComponentUtil.getData(ui,
                BeaconHandler.class);
        if (beaconHandler != null) {
            // Already installed, return the existing handler
            return beaconHandler;
        }

        BeaconHandler newBeaconHandler = new BeaconHandler();

        // ./beacon/<random uuid>
        String relativeBeaconPath = "." + newBeaconHandler.beaconPath;

        // TODO Use synchronous XHR if Beacon cannot be used (IE11)?
        ui.getElement().executeJs(
                "window.addEventListener('unload', function() {navigator.sendBeacon && navigator.sendBeacon($0)})",
                relativeBeaconPath);

        VaadinSession session = ui.getSession();
        session.addRequestHandler(newBeaconHandler);
        ComponentUtil.setData(ui, BeaconHandler.class, newBeaconHandler);

        ui.addDetachListener(
                detachEvent -> session.removeRequestHandler(newBeaconHandler));
        return newBeaconHandler;
    }
}
