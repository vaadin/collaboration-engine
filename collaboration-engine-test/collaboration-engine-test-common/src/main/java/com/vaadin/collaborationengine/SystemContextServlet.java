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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/submit")
public class SystemContextServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CollaborationEngine ce = (CollaborationEngine) getServletContext()
                .getAttribute(CollaborationEngine.class.getName());
        if (ce == null) {
            resp.sendError(500,
                    "Collaboration Engine has not yet been initialized");
            return;
        }

        try (BufferedReader reader = req.getReader()) {
            String message = reader.lines().collect(Collectors.joining("\n"))
                    .trim();
            if (message.isEmpty()) {
                resp.sendError(500, "Request must have a body");
                return;
            }

            MessageManager messageManager = new MessageManager(
                    ce.getSystemContext(), SystemUserInfo.getInstance(),
                    SystemContextViewCommon.class.getName(), () -> ce);

            // Block until we get a confirmation
            messageManager.submit(message).get();

            messageManager.close();

            resp.setStatus(200);
        } catch (InterruptedException e) {
            // Ignore interruptions
            e.printStackTrace();
        } catch (ExecutionException e) {
            resp.sendError(500, e.getMessage());
        }
    }
}
