/**
 * Copyright (C) 2000-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.Registration;

/**
 * A {@link SynchronizedRequestHandler} which notifies its listeners when the
 * browser tab is closed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class BeaconHandler extends SynchronizedRequestHandler {
    private static final String ID_PARAMETER = "id";
    private static final String REQUEST_TYPE = "beacon";

    private final String id = UUID.randomUUID().toString();
    private final List<Command> listeners;
    private final String beaconPath;

    public BeaconHandler(String beaconPath) {
        listeners = new ArrayList<>();
        this.beaconPath = beaconPath;
    }

    static BeaconHandler ensureInstalled(UI ui, String beaconPath) {
        BeaconHandler beaconHandler = ComponentUtil.getData(ui,
                BeaconHandler.class);
        if (beaconHandler != null) {
            // Already installed, return the existing handler
            return beaconHandler;
        }

        BeaconHandler newBeaconHandler = new BeaconHandler(beaconPath);

        ui.getElement().executeJs(getUnloadScript(),
                newBeaconHandler.createBeaconUrl());

        VaadinSession session = ui.getSession();
        session.addRequestHandler(newBeaconHandler);
        ComponentUtil.setData(ui, BeaconHandler.class, newBeaconHandler);

        ui.addDetachListener(
                detachEvent -> session.removeRequestHandler(newBeaconHandler));
        return newBeaconHandler;
    }

    String createBeaconUrl() {
        String requestTypeParameter = formatParameter(
                ApplicationConstants.REQUEST_TYPE_PARAMETER, REQUEST_TYPE);
        String beaconIdParameter = formatParameter(ID_PARAMETER, id);
        return "." + beaconPath + "?" + requestTypeParameter + "&"
                + beaconIdParameter;

    }

    private static String formatParameter(String name, String value) {
        return name + "=" + value;
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

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        if (!beaconPath.equals(request.getPathInfo())) {
            return false;
        }
        String requestType = request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
        String beaconId = request.getParameter(ID_PARAMETER);
        return REQUEST_TYPE.equals(requestType) && id.equals(beaconId);
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
}
