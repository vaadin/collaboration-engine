package com.vaadin.collaborationengine;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Push
@Route("examplecomponent")
@PreserveOnRefresh
public class CommonCollaborativeExampleComponentView extends VerticalLayout {

    public static final String TOPIC_ID = "topic";

    CollaborationAvatarGroup avatars;

    CollaborationExampleComponent exampleComponent;

    static AtomicInteger userCounter = new AtomicInteger(0);
    NativeButton resetUserCounter = new NativeButton("Reset user counter",
            e -> userCounter.set(0));

    public CommonCollaborativeExampleComponentView() {
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        avatars = new CollaborationAvatarGroup(localUser, TOPIC_ID);

        resetUserCounter.setId("reset-user-counter");
        exampleComponent = new CollaborationExampleComponent(localUser,
                TOPIC_ID);
        exampleComponent.getContent().addClassName("borders");
        add(avatars, exampleComponent, resetUserCounter);
    }

}
