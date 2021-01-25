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
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Component for displaying a list of messages with controls for adding new
 * messages to the stream.
 */
@CssImport("./styles/examplecomponent.css")
public class ExampleComponent extends Div {
    private Div commentLayout;
    private TextField commentTextField;
    private Button submitMessageButton;
    private Consumer<String> listener;

    /**
     * Creates a new example component for displaying a list of messages with
     * controls for adding new messages to the stream. The list of messages to
     * be shown in the component can be defined with
     * {@link #setMessages(List<Message>)}. New messages, added by the end-user,
     * can be listened to with {@link #setSubmitListener(Consumer)}. New
     * messages added do not appear automatically, so the developer must
     * contstruct a new {@link Message} object from the provided string and
     * update the list of messages.
     */
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

    /**
     * Removes all messages displayed and repopulates it with the provided list
     * of messages.
     * 
     * @param messages
     *            the list of messages to be shown in the component. From each
     *            {@link Message}, a {@link ExampleComponentMessage} is created
     */
    public void setMessages(List<Message> messages) {
        commentLayout.removeAll();
        messages.forEach(message -> commentLayout
                .add(new ExampleComponentMessage(message)));
        commentLayout.getElement().executeJs("$0.scrollTop = $0.scrollHeight;");
    }

    /**
     * Gets the components displaying the current messages.
     * 
     * @return a stream of the components displaying messages
     */
    public Stream<Component> getMessages() {
        return commentLayout.getChildren();
    }

    /**
     * Gets a reference for the input field for adding new messages.
     * 
     * @return the {@link TextField} reference for typing in new messages
     */
    public TextField getCommentField() {
        return commentTextField;
    }

    /**
     * Gets a reference for the button that the user can press to send a new
     * message.
     * 
     * @return the {@link Button} reference for sending new messages
     */
    public Button getSubmitMessageButton() {
        return submitMessageButton;
    }

    /**
     * Provides a listener for when the user sends a new message with the
     * component. The string that the user has entered is provided.
     * 
     * To add the string that the user filled in back into the message of the
     * list, you can do the following:
     * 
     * <pre>
     * exampleComponent.setSubmitListener(content -> {
     *     Message message = new Message(content, userName, userAvatar,
     *             LocalDateTime.now());
     *     messages.add(message);
     *     exampleComponent.setMessages(messages);
     * });
     * </pre>
     * 
     * @param listener
     *            the listener for a new message
     */
    public void setSubmitListener(Consumer<String> listener) {
        this.listener = listener;
    }

    private Div createControls() {
        commentTextField = new TextField();
        commentTextField.setPlaceholder("Message...");
        commentTextField.addClassName("comments-textfield");
        commentTextField.addKeyDownListener(Key.ENTER,
                event -> submitMessage());

        submitMessageButton = new Button("Send", event -> submitMessage());
        submitMessageButton.addClassName("comments-button");
        submitMessageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new Div(commentTextField, submitMessageButton);
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
