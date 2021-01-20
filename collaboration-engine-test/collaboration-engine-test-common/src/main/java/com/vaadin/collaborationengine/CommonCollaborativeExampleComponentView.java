package com.vaadin.collaborationengine;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.collaborationengine.examplecomponent.ExampleComponent;
import com.vaadin.collaborationengine.examplecomponent.Message;
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

    ExampleComponent exampleComponent = new ExampleComponent();

    static AtomicInteger userCounter = new AtomicInteger(0);
    NativeButton resetUserCounter = new NativeButton("Reset user counter",
            e -> userCounter.set(0));

    public CommonCollaborativeExampleComponentView() {
        int userIndex = userCounter.incrementAndGet();

        UserInfo localUser = new UserInfo("userId-" + userIndex);
        localUser.setName("User " + userIndex);
        avatars = new CollaborationAvatarGroup(localUser, TOPIC_ID);

        resetUserCounter.setId("reset-user-counter");

        exampleComponent.addClassName("borders");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(
                "A beginning is the time for taking the most delicate care that the balances are correct.",
                "Frank Herbert", "https://i.pravatar.cc/150?img=3",
                LocalDateTime.now().minus(4149, ChronoUnit.MINUTES)));
        messages.add(new Message(
                "I will not be stopped. Not by you, or the Confederates, or the Protoss or anyone! I will rule this sector or see it burnt to ashes around me.",
                "Arcturus Mengsk", "https://i.pravatar.cc/150?img=13",
                LocalDateTime.now().minus(1535, ChronoUnit.MINUTES)));
        exampleComponent.setMessages(messages);
        exampleComponent.setSubmitListener(content -> {
            messages.add(new Message(content, "Rosie the Riveter",
                    "https://i.pravatar.cc/150?img=45", LocalDateTime.now()));
            exampleComponent.setMessages(messages);
        });

        add(avatars, exampleComponent, resetUserCounter);
    }

}
