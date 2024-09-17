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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * A {@link VaadinServiceInitListener} which applies callbacks to the service
 * instance to reinitialize the state of topic connection context after
 * de-serialization.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class CollaborationEngineServiceInitListener
        implements VaadinServiceInitListener {

    private static final List<Consumer<VaadinService>> reinitializers = new ArrayList<>();

    public static void addReinitializer(Consumer<VaadinService> reinitializer) {
        reinitializers.add(reinitializer);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler((session, request, response) -> {
            VaadinService requestService = request.getService();
            if (requestService != null && !reinitializers.isEmpty()) {
                synchronized (reinitializers) {
                    for (Consumer<VaadinService> reinitializer : reinitializers) {
                        reinitializer.accept(requestService);
                    }
                    reinitializers.clear();
                }
            }
            return false;
        });
    }
}
