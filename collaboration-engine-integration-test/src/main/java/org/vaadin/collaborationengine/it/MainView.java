package org.vaadin.collaborationengine.it;

import java.util.Objects;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborativeMap;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;

/**
 * The main view contains a button and a collaborative div which shows the
 * number of clicks
 */
@Route("")
@Push
public class MainView extends VerticalLayout {

    private VerticalLayout editor = new VerticalLayout();
    private Button button = new Button("Increase");
    private Span span = new Span();

    public MainView() {
        CollaborationEngine.getInstance().openTopicConnection(editor,
                MainView.class.getName(), topic -> {
                    CollaborativeMap map = topic.getMap();
                    if (map.get("value") == null) {
                        map.put("value", 0);
                    }

                    map.subscribe(event -> span
                            .setText(Objects.toString(event.getNewValue())));

                    return button.addClickListener(e -> {
                        Thread update = new Thread(() -> {
                            Integer newState = (Integer) map.get("value") + 1;
                            map.put("value", newState);
                        });
                        update.start();
                    });
                });

        editor.add(button, span);

        Button detachButton = new Button("Detach editor", e -> remove(editor));
        add(editor, detachButton);
    }

}
