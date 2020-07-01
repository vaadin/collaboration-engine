import { FieldHighlighter } from '@vaadin/flow-frontend/field-highlighter/src/vaadin-field-highlighter.js';

// TODO: get color index from the server?
const users = [];

window.setFieldState = (field, activeUser) => {
    let index = users.indexOf(activeUser);
    if (activeUser && index === -1) {
        users.push(activeUser);
        index = users.length - 1;
    }

    const user = { name: activeUser, index };
    if (activeUser) {
        FieldHighlighter.addUser(field, user);
    } else {
        FieldHighlighter.removeUser(field);
    }
}
