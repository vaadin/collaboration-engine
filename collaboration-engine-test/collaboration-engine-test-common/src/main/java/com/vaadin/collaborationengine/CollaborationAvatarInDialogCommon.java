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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("dialog")
public class CollaborationAvatarInDialogCommon extends Div {

    public CollaborationAvatarInDialogCommon() {
        UserInfo userInfo = new UserInfo("a");

        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(true);
        dialog.addDialogCloseActionListener(e -> dialog.close());

        // Dialog contains a close button and an avatar group
        addWithId(dialog, new Button("close", e -> dialog.close()), "close");
        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(
                userInfo, null);
        dialog.add(avatarGroup);

        final String topicId = "1";
        addWithId(this, new Button("open", e -> {
            avatarGroup.setTopic(topicId);
            dialog.open();
        }), "open");

        // List of users in the topic, controlled by the PresenceManager
        Div usersInTopic = addWithId(this, new Div(), "users");

        // Connects to the same topic as the avatar group without marking
        // as present and lists the users in the topic.
        PresenceManager manager = new PresenceManager(this, userInfo, topicId);
        manager.setPresenceHandler(ctx -> {
            Div userInTopic = addWithId(usersInTopic,
                    new Div(new Text("User " + ctx.getUser().getId())),
                    "user_" + ctx.getUser().getId());
            return () -> usersInTopic.remove(userInTopic);
        });
    }

    private static <T extends Component> T addWithId(HasComponents parent,
            T element, String id) {
        element.setId(id);
        parent.add(element);
        return element;
    }
}
