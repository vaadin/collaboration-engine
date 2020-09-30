import { expect } from '@bundled-es-modules/chai';
import sinon from 'sinon';
import { afterNextRender } from '@polymer/polymer/lib/utils/render-status.js';
import { fixture, html } from '@open-wc/testing-helpers';
import '@vaadin/vaadin-checkbox/vaadin-checkbox-group.js';
import '@vaadin/vaadin-date-picker/vaadin-date-picker.js';
import '@vaadin/vaadin-date-time-picker/vaadin-date-time-picker.js';
import '@vaadin/vaadin-select/vaadin-select.js';
import '@vaadin/vaadin-list-box/vaadin-list-box.js';
import '@vaadin/vaadin-item/vaadin-item.js';
import '@vaadin/vaadin-radio-button/vaadin-radio-button.js';
import '@vaadin/vaadin-radio-button/vaadin-radio-group.js';
import { FieldHighlighter } from '../src/vaadin-field-highlighter.js';

describe('field components', () => {
  let field;
  let overlay;
  let highlighter;
  let showSpy;
  let hideSpy;

  function getOutline(elem) {
    return elem.shadowRoot.querySelector('[part="outline"]');
  }

  function listenForEvent(elem, type, callback) {
    const listener = function () {
      elem.removeEventListener(type, listener);
      callback();
    };
    elem.addEventListener(type, listener);
  }

  function open(elem, callback) {
    const overlay = elem.$.overlay || elem._overlayElement;
    listenForEvent(overlay, 'vaadin-overlay-open', callback);
    const event = new CustomEvent('keydown', {bubbles: true, composed: true});
    event.key = 'ArrowDown';
    elem.focusElement.dispatchEvent(event);
  }

  function focusin(target, relatedTarget) {
    const event = new Event('focusin');
    if (relatedTarget) {
      event.relatedTarget = relatedTarget;
    }
    target.dispatchEvent(event);
  }

  function focusout(target, relatedTarget) {
    const event = new Event('focusout');
    if (relatedTarget) {
      event.relatedTarget = relatedTarget;
    }
    target.dispatchEvent(event);
  }

  describe('text field', () => {
    beforeEach(async () => {
      field = await fixture(html`<vaadin-text-field></vaadin-text-field>`);
      highlighter = FieldHighlighter.init(field);
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
    });

    it('should dispatch vaadin-highlight-show event on focus', () => {
      field.focus();
      expect(showSpy.callCount).to.equal(1);
    });

    it('should dispatch vaadin-highlight-hide event on blur', () => {
      field.focus();
      field.blur();
      expect(hideSpy.callCount).to.equal(1);
    });
  });

  describe('date picker', () => {
    beforeEach(async () => {
      field = await fixture(html`<vaadin-date-picker></vaadin-date-picker>`);
      highlighter = FieldHighlighter.init(field);
      overlay = field.$.overlay;
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
    });

    afterEach(() => {
      field.opened && field.close();
    });

    describe('default', () => {
      it('should dispatch vaadin-highlight-show event on focus', () => {
        field.focus();
        expect(showSpy.callCount).to.equal(1);
      });

      it('should dispatch vaadin-highlight-hide event on blur', () => {
        field.focus();
        field.blur();
        expect(hideSpy.callCount).to.equal(1);
      });

      it('should not dispatch vaadin-highlight-hide event on open', (done) => {
        field.focus();
        open(field, () => {
          expect(hideSpy.callCount).to.equal(0);
          done();
        });
      });

      it('should not dispatch vaadin-highlight-hide event on close without blur', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(0);
            done();
          });
          field.close();
        });
      });

      it('should not dispatch vaadin-highlight-hide event on re-focusing field', (done) => {
        field.focus();
        open(field, () => {
          focusin(field, overlay);
          afterNextRender(field, () => {
            expect(hideSpy.callCount).to.equal(0);
            done();
          });
        });
      });

      it('should not dispatch second vaadin-highlight-show event on re-focusing field', (done) => {
        field.focus();
        open(field, () => {
          focusout(field);
          focusin(field);
          afterNextRender(field, () => {
            expect(showSpy.callCount).to.equal(1);
            done();
          });
        });
      });

      it('should not dispatch vaadin-highlight-hide event on field blur if opened', (done) => {
        field.focus();
        open(field, () => {
          focusout(field);
          field.focus();
          afterNextRender(field, () => {
            expect(hideSpy.callCount).to.equal(0);
            done();
          });
        });
      });

      it('should dispatch vaadin-highlight-hide event on close after blur', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(1);
            done();
          });
          overlay.focus();
          focusout(overlay);
          field.close();
        });
      });
    });

    describe('fullscreen', () => {
      beforeEach(async () => {
        field._fullscreen = true;
      });

      it('should not dispatch vaadin-highlight-show event on focus', () => {
        field.focus();
        expect(showSpy.callCount).to.equal(0);
      });

      it('should dispatch vaadin-highlight-show event on open', (done) => {
        listenForEvent(field, 'opened-changed', () => {
          afterNextRender(field, () => {
            expect(showSpy.callCount).to.equal(1);
            done();
          });
        });
        field.dispatchEvent(new Event('focus'));
        field.focus();
        field.click();
      });
    });
  });

  describe('select', () => {
    beforeEach(async () => {
      field = await fixture(html`
        <vaadin-select>
          <template>
            <vaadin-list-box>
              <vaadin-item>Foo</vaadin-item>
              <vaadin-item>Bar</vaadin-item>
              <vaadin-item>Baz</vaadin-item>
            </vaadin-list-box>
          </template>
        </vaadin-select>
      `);
      highlighter = FieldHighlighter.init(field);
      overlay = field._overlayElement;
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
    });

    describe('default', () => {
      it('should dispatch vaadin-highlight-show event on focus', () => {
        field.focus();
        expect(showSpy.callCount).to.equal(1);
      });

      it('should dispatch vaadin-highlight-hide event on blur', () => {
        field.focus();
        field.blur();
        expect(hideSpy.callCount).to.equal(1);
      });

      it('should not dispatch vaadin-highlight-hide event on open', (done) => {
        field.focus();
        open(field, () => {
          expect(hideSpy.callCount).to.equal(0);
          done();
        });
      });

      it('should not dispatch vaadin-highlight-hide event on select', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(0);
            done();
          });
          overlay.querySelector('vaadin-item').click();
        });
      });

      it('should not dispatch vaadin-highlight-hide event on outside click', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(0);
            done();
          });
          overlay.querySelector('vaadin-item').blur();
          document.body.click();
        });
      });

      it('should not dispatch second vaadin-highlight-show event on outside click', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(showSpy.callCount).to.equal(1);
            done();
          });
          overlay.querySelector('vaadin-item').blur();
          document.body.click();
        });
      });

    });

    describe('phone', () => {
      beforeEach(async () => {
        field._phone = true;
      });

      it('should dispatch vaadin-highlight-hide event on outside click', (done) => {
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(1);
            done();
          });
          focusout(overlay);
          document.body.click();
        });
      });

      it('should dispatch vaadin-highlight-hide event on select', (done) => {
        field.focus();
        open(field, () => {
          listenForEvent(field, 'opened-changed', () => {
            expect(hideSpy.callCount).to.equal(1);
            done();
          });
          overlay.querySelector('vaadin-item').click();
        });
      });
    });
  });

  describe('checkbox group', () => {
    let checkboxes;

    beforeEach(async () => {
      field = await fixture(html`
        <vaadin-checkbox-group>
          <vaadin-checkbox value="1">Checkbox <b>1</b></vaadin-checkbox>
          <vaadin-checkbox value="2">Checkbox <b>2</b></vaadin-checkbox>
          <vaadin-checkbox value="3">Checkbox <b>3</b></vaadin-checkbox>
        </vaadin-checkbox-group>
      `);
      highlighter = FieldHighlighter.init(field);
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
      checkboxes = Array.from(field.children);
    });

    it('should dispatch vaadin-highlight-show event on checkbox focus', () => {
      checkboxes[0].focus();
      expect(showSpy.callCount).to.equal(1);
      expect(showSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on checkbox blur', () => {
      checkboxes[0].focus();
      checkboxes[0].blur();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on other checkbox focus', () => {
      checkboxes[0].focus();
      focusout(checkboxes[0], checkboxes[1]);
      checkboxes[1].focus();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch second vaadin-highlight-show event on other checkbox focus', () => {
      checkboxes[0].focus();
      focusout(checkboxes[0], checkboxes[1]);
      checkboxes[1].focus();
      expect(showSpy.callCount).to.equal(2);
      expect(showSpy.getCalls()[1].args[0].detail.fieldIndex).to.equal(1);
    });

    it('should set outline on multiple checkboxes based on the fieldIndex', () => {
      const user1 = { id: 'a', name: 'foo', fieldIndex: 0 };
      const user2 = { id: 'b', name: 'var', fieldIndex: 1 };
      FieldHighlighter.setUsers(field, [user1, user2]);
      expect(getComputedStyle(getOutline(checkboxes[0])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(checkboxes[1])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(checkboxes[2])).opacity).to.equal('0');
    });
  });

  describe('radio group', () => {
    let radios;

    beforeEach(async () => {
      field = await fixture(html`
        <vaadin-radio-group>
          <vaadin-radio-button value="1">Radio <b>1</b></vaadin-radio-button>
          <vaadin-radio-button value="2">Radio <b>2</b></vaadin-radio-button>
          <vaadin-radio-button value="3">Radio <b>3</b></vaadin-radio-button>
        </vaadin-radio-group>
      `);
      highlighter = FieldHighlighter.init(field);
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
      radios = Array.from(field.children);
    });

    it('should dispatch vaadin-highlight-show event on checkbox focus', () => {
      radios[0].focus();
      expect(showSpy.callCount).to.equal(1);
      expect(showSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on checkbox blur', () => {
      radios[0].focus();
      radios[0].blur();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on other radio focus', () => {
      radios[0].focus();
      focusout(radios[0], radios[1]);
      radios[1].focus();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch second vaadin-highlight-show event on other radio focus', () => {
      radios[0].focus();
      focusout(radios[0], radios[1]);
      radios[1].focus();
      expect(showSpy.callCount).to.equal(2);
      expect(showSpy.getCalls()[1].args[0].detail.fieldIndex).to.equal(1);
    });

    it('should set outline on multiple radios based on the fieldIndex', () => {
      const user1 = { id: 'a', name: 'foo', fieldIndex: 0 };
      const user2 = { id: 'b', name: 'var', fieldIndex: 1 };
      FieldHighlighter.setUsers(field, [user1, user2]);
      expect(getComputedStyle(getOutline(radios[0])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(radios[1])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(radios[2])).opacity).to.equal('0');
    });
  });

  describe('date time picker', () => {
    let date;
    let time;

    beforeEach(async () => {
      field = await fixture(html`<vaadin-date-time-picker></vaadin-date-time-picker>`);
      highlighter = FieldHighlighter.init(field);
      date = field.$.customField.inputs[0];
      time = field.$.customField.inputs[1];
      overlay = field.$.overlay;
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
    });

    afterEach(() => {
      date.opened && date.close();
    });

    it('should dispatch vaadin-highlight-show event on date picker focus', () => {
      date.focus();
      expect(showSpy.callCount).to.equal(1);
    });

    it('should dispatch vaadin-highlight-hide event on date picker blur', () => {
      date.focus();
      date.blur();
      expect(hideSpy.callCount).to.equal(1);
    });

    it('should dispatch vaadin-highlight-show event on time picker focus', () => {
      time.focus();
      expect(showSpy.callCount).to.equal(1);
    });

    it('should dispatch vaadin-highlight-hide event on time picker blur', () => {
      time.focus();
      time.blur();
      expect(hideSpy.callCount).to.equal(1);
    });

    it('should dispatch vaadin-highlight-hide event on moving focus to time picker', () => {
      focusout(date, time);
      focusin(time, date);
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch second vaadin-highlight-show event on moving focus to time picker', () => {
      focusout(date, time);
      focusin(time, date);
      expect(showSpy.callCount).to.equal(2);
      expect(showSpy.getCalls()[1].args[0].detail.fieldIndex).to.equal(1);
    });

    it('should dispatch vaadin-highlight-hide event on moving focus to date picker', () => {
      focusout(time, date);
      focusin(date, time);
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(1);
    });

    it('should dispatch second vaadin-highlight-show event on moving focus to date picker', () => {
      focusout(time, date);
      focusin(date, time);
      expect(showSpy.callCount).to.equal(2);
      expect(showSpy.getCalls()[1].args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on overlay focusout to time picker', (done) => {
      date.focus();
      open(date, () => {
        listenForEvent(date, 'opened-changed', () => {
          expect(hideSpy.callCount).to.equal(1);
          expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
          done();
        });
        date.$.overlay.focus();
        focusout(date.$.overlay, time);
        date.close();
      });
    });

    it('should set outline on both date and time pickers based on the fieldIndex', () => {
      const user1 = { id: 'a', name: 'foo', fieldIndex: 0 };
      const user2 = { id: 'b', name: 'var', fieldIndex: 1 };
      FieldHighlighter.setUsers(field, [user1, user2]);
      expect(getComputedStyle(getOutline(date.focusElement)).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(time.focusElement)).opacity).to.equal('1');
    });
  });

  describe('list-box', () => {
    let items;

    beforeEach(async () => {
      field = await fixture(html`
        <vaadin-list-box>
          <vaadin-item>Option 1</vaadin-item>
          <vaadin-item>Option 2</vaadin-item>
          <vaadin-item>Option 3</vaadin-item>
        </vaadin-list-box>
      `);
      highlighter = FieldHighlighter.init(field);
      items = field.items;
      showSpy = sinon.spy();
      hideSpy = sinon.spy();
      field.addEventListener('vaadin-highlight-show', showSpy);
      field.addEventListener('vaadin-highlight-hide', hideSpy);
    });

    it('should dispatch vaadin-highlight-show event on item focus', () => {
      items[0].focus();
      expect(showSpy.callCount).to.equal(1);
      expect(showSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on item blur', () => {
      items[0].focus();
      items[0].blur();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch vaadin-highlight-hide event on other item focus', () => {
      items[0].focus();
      focusout(items[0], items[1]);
      items[1].focus();
      expect(hideSpy.callCount).to.equal(1);
      expect(hideSpy.firstCall.args[0].detail.fieldIndex).to.equal(0);
    });

    it('should dispatch second vaadin-highlight-show event on other item focus', () => {
      items[0].focus();
      focusout(items[0], items[1]);
      items[1].focus();
      expect(showSpy.callCount).to.equal(2);
      expect(showSpy.getCalls()[1].args[0].detail.fieldIndex).to.equal(1);
    });

    it('should set outline on multiple items based on the fieldIndex', () => {
      const user1 = { id: 'a', name: 'foo', fieldIndex: 0 };
      const user2 = { id: 'b', name: 'var', fieldIndex: 1 };
      FieldHighlighter.setUsers(field, [user1, user2]);
      expect(getComputedStyle(getOutline(items[0])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(items[1])).opacity).to.equal('1');
      expect(getComputedStyle(getOutline(items[2])).opacity).to.equal('0');
    });
  });
});
