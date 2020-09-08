import { FieldObserver } from './vaadin-field-observer.js';

export class CheckboxGroupObserver extends FieldObserver {
  getFields() {
    return this.component._checkboxes;
  }

  getFocusTarget(event) {
    const fields = this.getFields();
    return Array.from(event.composedPath()).filter((node) => fields.indexOf(node) !== -1)[0];
  }
}
