import { ComponentObserver } from './vaadin-component-observer.js';

export class FieldObserver extends ComponentObserver {
  constructor(field) {
    super(field);

    this.addListeners(field);
  }

  addListeners(field) {
    field.addEventListener('focusin', event => this.onFocusIn(event));
    field.addEventListener('focusout', event => this.onFocusOut(event));
  }

  onFocusIn(event) {
    this.fireShowHighlight();
  }

  onFocusOut(event) {
    this.fireHideHighlight();
  }
}
