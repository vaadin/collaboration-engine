import { ComponentObserver } from './vaadin-component-observer.js';
import { DatePickerObserver } from './vaadin-date-picker-observer.js';
import { FieldObserver } from './vaadin-field-observer.js';

class DateObserver extends DatePickerObserver {
  constructor(datePicker, timePicker, host) {
    super(datePicker);

    // fire events on the host
    this.component = host;
    this.timePicker = timePicker;
  }

  onFocusIn(event) {
    if (event.relatedTarget === this.timePicker) {
      return;
    }
    super.onFocusIn(event);
  }

  onFocusOut(event) {
    if (event.relatedTarget === this.timePicker) {
      return;
    }
    super.onFocusOut(event);
  }

  onOverlayFocusOut(event) {
    const { relatedTarget } = event;
    // Depending on whether the components are slotted or not, focus can go
    // either to the host or to the component, so we check for both cases.
    if (event.relatedTarget === this.timePicker || event.relatedTarget === this.component) {
      return;
    }
    super.onOverlayFocusOut(event);
  }
}

class TimeObserver extends FieldObserver {
  constructor(timePicker, datePicker, host) {
    super(timePicker);

    // fire events on the host
    this.component = host;
    this.datePicker = datePicker;
  }

  onFocusIn(event) {
    const { relatedTarget } = event;
    if (relatedTarget === this.datePicker || relatedTarget === this.datePicker.$.overlay) {
      return;
    }
    super.onFocusIn(event);
  }

  onFocusOut(event) {
    if (event.relatedTarget === this.datePicker) {
      return;
    }
    super.onFocusOut(event);
  }
}

export class DateTimePickerObserver extends ComponentObserver {
  constructor(picker) {
    super(picker);

    const [datePicker, timePicker] = picker.$.customField.inputs;

    this.dateObserver = new DateObserver(datePicker, timePicker, picker);
    this.timeObserver = new TimeObserver(timePicker, datePicker, picker);
  }
}
