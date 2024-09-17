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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;

@Route("chat")
public class CollaborativeChatViewCommon extends VerticalLayout {

    CollaborationMessageList list;
    CollaborationMessageInput input;

    static AtomicInteger userCounter = new AtomicInteger(0);
    static AtomicInteger topicCounter = new AtomicInteger(0);
    NativeButton resetUserCounter = new NativeButton("Reset user counter",
            e -> userCounter.set(0));
    NativeButton nextTopic = new NativeButton("Next topic", e -> nextTopic());
    String currentTopic;
    Paragraph topicIndicator = new Paragraph("Current topic: ");

    public CollaborativeChatViewCommon() {
        addAttachListener(event -> event.getUI().getPushConfiguration()
                .setPushMode(PushMode.AUTOMATIC));
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        list = new CollaborationMessageList(localUser, null);
        input = new CollaborationMessageInput(list);
        currentTopic = "topic" + topicCounter.get();
        setTopic(currentTopic);

        VerticalLayout chat = new VerticalLayout(list, input);
        chat.setHeight("400px");
        chat.expand(list);

        resetUserCounter.setId("reset-user-counter");
        nextTopic.setId("next-topic");

        add(chat, topicIndicator, resetUserCounter, nextTopic);

        NativeButton setTopicNull = new NativeButton("Set chat topic to null",
                e -> setTopic(null));
        setTopicNull.setId("set-topic-null");
        NativeButton setTopic = new NativeButton("Set old topic again",
                e -> setTopic(currentTopic));
        setTopic.setId("set-topic");

        add(setTopicNull, setTopic);
    }

    private void nextTopic() {
        currentTopic = "topic" + topicCounter.incrementAndGet();
        setTopic(currentTopic);
    }

    private void setTopic(String topic) {
        topicIndicator.setText("Current topic: " + topic);
        list.setTopic(topic);
    }

}
