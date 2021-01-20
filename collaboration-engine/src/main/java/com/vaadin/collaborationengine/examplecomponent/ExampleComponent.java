/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine.examplecomponent;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

@CssImport("./styles/examplecomponent.css")
public class ExampleComponent extends Div {
    private Div commentLayout;
    private TextField commentTextField;
    private Consumer<String> listener;

    public ExampleComponent() {
        Div header = new Div(new Text("Conversation"));
        commentLayout = new Div();
        Div addCommentLayout = createControls();
        add(header, commentLayout, addCommentLayout);

        addClassName("comments");
        header.addClassName("comments-header");
        commentLayout.addClassName("comments-feed");
        addCommentLayout.addClassName("comments-add");
    }

    public void setMessages(List<Message> messages) {
        commentLayout.removeAll();
        messages.forEach(message -> commentLayout
                .add(new ExampleComponentMessage(message)));
        commentLayout.getElement().executeJs("$0.scrollTop = $0.scrollHeight;");
    }

    public void setSubmitListener(Consumer<String> listener) {
        this.listener = listener;
    }

    private Div createControls() {
        commentTextField = new TextField();
        commentTextField.setPlaceholder("Message...");
        commentTextField.addClassName("comments-textfield");
        commentTextField.addKeyDownListener(Key.ENTER,
                event -> submitMessage());

        Button button = new Button("Send", event -> submitMessage());
        button.addClassName("comments-button");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new Div(commentTextField, button);
    }

    private void submitMessage() {
        String content = commentTextField.getValue();
        commentTextField.clear();
        commentTextField.focus();
        if (this.listener != null && !"".equals(content)) {
            listener.accept(content);
        }
    }
}
