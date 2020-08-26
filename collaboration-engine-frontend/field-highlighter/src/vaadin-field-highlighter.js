import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { IronA11yAnnouncer } from '@polymer/iron-a11y-announcer/iron-a11y-announcer.js';
import { DirMixin } from '@vaadin/vaadin-element-mixin/vaadin-dir-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { DatePickerObserver } from './fields/vaadin-date-picker-observer.js';
import { DateTimePickerObserver } from './fields/vaadin-date-time-picker-observer.js';
import { GroupObserver } from './fields/vaadin-group-observer.js';
import { FieldObserver } from './fields/vaadin-field-observer.js';
import { SelectObserver } from './fields/vaadin-select-observer.js';

import { applyShadyStyle, setCustomProperty } from './css-helpers.js';
import './vaadin-user-tags.js';

const fields = new WeakMap();

export class FieldHighlighter extends ThemableMixin(DirMixin(PolymerElement)) {
  static init(field) {
    if (!fields.has(field)) {
      // Create instance
      const instance = document.createElement(this.is);
      instance._field = field;

      // Set attribute for styling
      field.setAttribute('has-highlighter', '');

      // TODO: Not nice, have to create a stacking context
      const style = `
        :host([has-highlighter]) {
          position: relative;
        }
      `;
      applyShadyStyle(field, style);

      // Store instance
      fields.set(field, instance);

      this.initFieldObserver(field);

      // Attach instance
      field.shadowRoot.appendChild(instance);
    }

    return fields.get(field);
  }

  static initFieldObserver(field) {
    let result;
    switch (field.tagName.toLowerCase()) {
      /* c8 ignore next */
      case 'vaadin-date-picker':
        result = new DatePickerObserver(field);
        break;
      /* c8 ignore next */
      case 'vaadin-date-time-picker':
        result = new DateTimePickerObserver(field);
        break;
      /* c8 ignore next */
      case 'vaadin-select':
        result = new SelectObserver(field);
        break;
      /* c8 ignore next 2 */
      case 'vaadin-checkbox-group':
      case 'vaadin-radio-group':
        result = new GroupObserver(field);
        break;
      default:
        result = new FieldObserver(field);
    }
  }

  static addUser(field, user) {
    this.init(field).addUser(user);
  }

  static removeUser(field, user) {
    this.init(field).removeUser(user);
  }

  static setUsers(field, users) {
    this.init(field).setUsers(users);
  }

  static get is() {
    return 'vaadin-field-highlighter';
  }

  static get template() {
    return html`
      <style>
        :host {
          display: none;
          box-sizing: border-box;
          position: absolute;
          z-index: -1;
          top: -8px;
          left: -8px;
          width: calc(100% + 16px);
          height: calc(100% + 16px);
          user-select: none;
        }

        :host::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          opacity: 0.5;
          border: 2px solid var(--_active-user-color);
          border-radius: 8px;
        }

        :host([has-active-user]) {
          display: block;
        }
      </style>
    `;
  }

  static get properties() {
    return {
      user: {
        type: Object,
        value: null,
        observer: '_userChanged'
      },
      users: {
        type: Array,
        value: () => []
      }
    };
  }

  ready() {
    super.ready();

    this._tags = document.createElement('vaadin-user-tags');
    this._field.shadowRoot.appendChild(this._tags);
    this._setUserTags(this.users);
    IronA11yAnnouncer.requestAvailability();
  }

  addUser(user) {
    if (user) {
      this.push('users', user);
      this._setUserTags(this.users);

      // Make user active
      this.user = user;
    }
  }

  setUsers(users) {
    if (Array.isArray(users)) {
      this.set('users', users);
      this._setUserTags(this.users);

      // Make user active
      this.user = users[users.length - 1] || null;
    }
  }

  removeUser(user) {
    if (user && user.id !== undefined) {
      let index;
      for (let i = 0; i < this.users.length; i++) {
        if (this.users[i].id === user.id) {
          index = i;
          break;
        }
      }
      if (index !== undefined) {
        this.splice('users', index, 1);
        this._setUserTags(this.users);

        // Change or remove active user
        if (this.users.length > 0) {
          this.user = this.users[this.users.length - 1];
        } else {
          this.user = null;
        }
      }
    }
  }

  _setUserTags(users) {
    if (this._tags) {
      this._tags.setUsers(Array.from(users).reverse());
    }
  }

  _announce(msg) {
    const label = this._field.label || '';
    this.dispatchEvent(
      new CustomEvent('iron-announce', {
        bubbles: true,
        composed: true,
        detail: {
          text: label ? `${msg} ${label}` : msg
        }
      })
    );
  }

  _userChanged(user) {
    if (user) {
      this.setAttribute('has-active-user', '');
      setCustomProperty(this, '--_active-user-color', `var(--vaadin-user-color-${user.colorIndex})`);
      this._announce(`${user.name} started editing`);
    } else {
      this.removeAttribute('has-active-user');
      setCustomProperty(this, '--_active-user-color', 'transparent');
    }
  }
}

customElements.define(FieldHighlighter.is, FieldHighlighter);
