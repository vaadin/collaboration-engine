package com.vaadin.collaborationengine;

import java.util.Objects;
import java.util.UUID;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * The main view contains a button and a collaborative div which shows the
 * number of clicks
 */
@Route("")
@Push
public class MainView extends VerticalLayout {

    private UserInfo user = new UserInfo(UUID.randomUUID().toString());
    private VerticalLayout editor = new VerticalLayout();
    private Button button = new Button("Increase");
    private Span span = new Span();

    private Registration connectionRegistration;

    public MainView() {
        openConnection();

        editor.add(button, span);

        HorizontalLayout activationButtons = new HorizontalLayout();
        Button detachButton = new Button("Detach editor", e -> remove(editor));
        Button reattachButton = new Button("Reattach editor",
                e -> addComponentAsFirst(editor));
        activationButtons.add(detachButton, reattachButton);

        HorizontalLayout connectionButtons = new HorizontalLayout();
        Button closeButton = new Button("Close connection",
                e -> connectionRegistration.remove());
        Button reopenButton = new Button("Reopen connection",
                e -> openConnection());
        connectionButtons.add(closeButton, reopenButton);

        add(editor, new Hr(), activationButtons, new Hr(), connectionButtons);
    }

    private void openConnection() {
        connectionRegistration = CollaborationEngine.getInstance()
                .openTopicConnection(editor, MainView.class.getName(), user,
                        topic -> {
                            CollaborationMap map = topic.getNamedMap("values");
                            if (map.get("value") == null) {
                                map.put("value", 0);
                            }

                            map.subscribe(event -> span.setText(
                                    Objects.toString(event.getValue())));

                            return button.addClickListener(e -> {
                                Thread update = new Thread(() -> {
                                    Integer newState = (Integer) map
                                            .get("value") + 1;
                                    map.put("value", newState);
                                });
                                update.start();
                            });
                        });
    }

}
