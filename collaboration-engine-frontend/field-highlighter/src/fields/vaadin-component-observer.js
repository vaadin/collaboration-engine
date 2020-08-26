export class ComponentObserver {
  constructor(component) {
    this.component = component;
  }

  fireShowHighlight() {
    this.fire(new CustomEvent('vaadin-highlight-show', { bubbles: true, composed: true }));
  }

  fireHideHighlight() {
    this.fire(new CustomEvent('vaadin-highlight-hide', { bubbles: true, composed: true }));
  }

  fire(event) {
    this.component.dispatchEvent(event);
  }
}
