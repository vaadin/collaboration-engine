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
package com.vaadin.collaborationengine;

import java.io.Serial;
import java.io.Serializable;
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
 * @since 1.0
 */
class ServiceDestroyDelegate implements Serializable {

    private final ArrayList<ServiceDestroyListener> listeners = new ArrayList<>();

    private final transient Registration serviceRegistration;
    private final transient Registration sessionRegistration;

    private final VaadinSession session;

    public ServiceDestroyDelegate(VaadinSession session) {
        this.session = session;
        VaadinService service = session.getService();

        serviceRegistration = service
                .addServiceDestroyListener(this::notifyListeners);

        sessionRegistration = service
                .addSessionDestroyListener(event -> removeRegistrations());
    }

    /*
     * An instance of this class is stored in the Vaadin session, causing
     * session serialization to fail. In order to solve this issue, we replace
     * it with null during serialization and then replace the session value with
     * a new instance during deserialization. See
     * ComponentConnectionContext.attach(UI).
     */
    @Serial
    private Object writeReplace() {
        return null;
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
