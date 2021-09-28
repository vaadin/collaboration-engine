package com.vaadin.collaborationengine;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
                    SystemContextViewCommon.class.getName(), ce);

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
