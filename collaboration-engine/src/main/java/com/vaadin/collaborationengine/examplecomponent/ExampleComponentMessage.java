/*
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Runtime License 1.0
 * (CVRLv1).
 *
 * For the full License, see http://vaadin.com/license/cvrl-1
 */
package com.vaadin.collaborationengine.examplecomponent;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;

public class ExampleComponentMessage extends Div {
    public ExampleComponentMessage(Message message) {
        Avatar avatar = new Avatar(message.getUserName(), message.getImage());
        Div name = new Div(new Text(message.getUserName()));
        Div date = new Div(new Text(message.getTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        Div nameAndDate = new Div(name, date);
        Div content = new Div(new Text(message.getContent()));
        Div nameDateContent = new Div(nameAndDate, content);
        add(avatar, nameDateContent);

        addClassName("comments-comment");
        avatar.addClassName("comments-comment-avatar");
        nameAndDate.addClassName("comments-comment-namedate");
        name.addClassName("comments-comment-name");
        date.addClassName("comments-comment-date");
        content.addClassName("comments-comment-content");
        nameDateContent.addClassName("comments-comment-content");
    }
}
