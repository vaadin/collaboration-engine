/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
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

    List<Command> getListeners() {
        return new ArrayList<>(listeners);
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

        ui.getElement().executeJs(getUnloadScript(), relativeBeaconPath);

        VaadinSession session = ui.getSession();
        session.addRequestHandler(newBeaconHandler);
        ComponentUtil.setData(ui, BeaconHandler.class, newBeaconHandler);

        ui.addDetachListener(
                detachEvent -> session.removeRequestHandler(newBeaconHandler));
        return newBeaconHandler;
    }

    private static String getUnloadScript() {
        //@formatter:off
        return "window.addEventListener('unload', function() {"
                + "  if (navigator.sendBeacon) {"
                + "    navigator.sendBeacon($0);"
                + "  } else {"
                + "    var xhr = new XMLHttpRequest();"
                + "    xhr.open(\"POST\", $0, false);"
                + "    xhr.send(\"\");"
                + "  }"
                + "})";
        //@formatter:on
    }
}
