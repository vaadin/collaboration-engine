package com.vaadin;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.User.UserService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationEngineConfiguration;
import com.vaadin.collaborationengine.LicenseEventHandler;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * Code snippets used in licensing/production documentation.
 */
public class ProductionDocumentation extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ProductionDocumentation.class);

    private UserService userService;

    public ProductionDocumentation() {
        definitionOfEndUser();
        requestAccess();
        checkUserPermissions();
    }

    @SpringComponent
    public static class MyVaadinInitListener
            implements VaadinServiceInitListener {

        private static final Logger LOGGER = LoggerFactory
                .getLogger(MyVaadinInitListener.class);

        @Override
        public void serviceInit(ServiceInitEvent serviceEvent) {
            VaadinService service = serviceEvent.getSource();

            LicenseEventHandler licenseEventHandler = licenseEvent -> {
                switch (licenseEvent.getType()) {
                case GRACE_PERIOD_STARTED:
                case LICENSE_EXPIRES_SOON:
                    LOGGER.warn(licenseEvent.getMessage());
                    break;
                case GRACE_PERIOD_ENDED:
                case LICENSE_EXPIRED:
                    LOGGER.error(licenseEvent.getMessage());
                    break;
                }
                sendEmail(
                        "Vaadin Collaboration Engine license needs to be updated",
                        licenseEvent.getMessage());
            };

            CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                    licenseEventHandler);
            configuration
                    .setDataDir("/Users/steve/vaadin/collaboration-engine/");
            CollaborationEngine.configure(service, configuration);
        }

        private void sendEmail(String subject, String content) {
            // Implement sending an email to relevant people
        }
    }

    private void sendEmail(String subject, String content) {
        // Replace the following information:
        String from = "sender@gmail.com";
        String password = "*****"; // Read e.g. from encrypted config file
        String to = "receiver@gmail.com";
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void definitionOfEndUser() {
        String userId = "steve@example.com";
        String name = "Steve";
        UserInfo userInfo = new UserInfo(userId, name);
        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                userInfo, "app");
        add(avatarGroup);
    }

    private void requestAccess() {
        Component component = new Div();

        //@formatter:off
        UserInfo userInfo = new UserInfo("steve@example.com", "Steve");
        CollaborationEngine.getInstance().requestAccess(userInfo, response -> {
            component.setVisible(response.hasAccess());
        });
        //@formatter:on
    }

    private void checkUserPermissions() {
        User userEntity = userService.getCurrentUser();
        if (userEntity.getRoles().contains(Role.ADMIN)) {
            UserInfo userInfo = new UserInfo(userEntity.getId(),
                    userEntity.getName(), userEntity.getImageUrl());

            CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                    userInfo, "avatars");

            add(avatarGroup);
        }
    }
}
