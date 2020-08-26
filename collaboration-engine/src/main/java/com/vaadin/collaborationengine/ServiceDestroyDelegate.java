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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

/**
 * Collects all service destroy listeners for a specific session to avoid
 * constantly adding and removing listeners in the global list in the service.
 *
 * @author Vaadin Ltd
 */
class ServiceDestroyDelegate {

    private final ArrayList<ServiceDestroyListener> listeners = new ArrayList<>();

    private final Registration serviceRegistration;
    private final Registration sessionRegistration;

    private final VaadinSession session;

    public ServiceDestroyDelegate(VaadinSession session) {
        this.session = session;
        VaadinService service = session.getService();

        serviceRegistration = service
                .addServiceDestroyListener(this::notifyListeners);

        sessionRegistration = service
                .addSessionDestroyListener(event -> removeRegistrations());
    }

    private void removeRegistrations() {
        serviceRegistration.remove();
        sessionRegistration.remove();
    }

    private void notifyListeners(ServiceDestroyEvent event) {
        session.access(() -> new ArrayList<>(listeners)
                .forEach(listener -> listener.serviceDestroy(event)));
    }

    public Registration addListener(ServiceDestroyListener listener) {
        return Registration.addAndRemove(listeners, listener);
    }

    public static ServiceDestroyDelegate ensureInstalled(UI ui) {
        VaadinSession session = ui.getSession();

        ServiceDestroyDelegate delegate = session
                .getAttribute(ServiceDestroyDelegate.class);

        if (delegate == null) {
            delegate = new ServiceDestroyDelegate(session);
            session.setAttribute(ServiceDestroyDelegate.class, delegate);
        }

        return delegate;
    }
}
