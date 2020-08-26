import { FieldObserver } from './vaadin-field-observer.js';

export class GroupObserver extends FieldObserver {
  constructor(group) {
    super(group);

    this.internalFocus = false;
  }

  onFocusIn(event) {
    if (this.internalFocus) {
      this.internalFocus = false;
      return;
    }
    super.onFocusIn(event);
  }

  onFocusOut(event) {
    const group = this.component;
    if (group.hasAttribute('focused') || group.contains(event.relatedTarget)) {
      this.internalFocus = true;
      return;
    }
    super.onFocusOut(event);
  }
}
