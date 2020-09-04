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

      window.ShadyDOM && window.ShadyDOM.flush();

      // Get root component to apply styles
      const root = this.getHighlightRoot(field);
      instance._highlightRoot = root;

      // Mimic :host-context to apply styles
      instance.setAttribute('context', root.tagName.toLowerCase());

      // Get target to attach instance
      const target = this.getHighlightTarget(root);
      instance._target = target;

      // Some components set this, but not all
      target.style.position = 'relative';

      const style = `
        :host([active]) [part="highlight"],
        :host([focus-ring]) [part="highlight"] {
          display: none;
        }
      `;
      applyShadyStyle(root, style);

      // Store instance
      fields.set(field, instance);

      this.initFieldObserver(field);

      // Attach instance
      target.appendChild(instance);
    }

    return fields.get(field);
  }

  static getHighlightRoot(field) {
    switch (field.tagName.toLowerCase()) {
      /* c8 ignore next 4 */
      case 'vaadin-combo-box':
      case 'vaadin-date-picker':
      case 'vaadin-select':
      case 'vaadin-time-picker':
        return field.focusElement;
      /* c8 ignore next */
      case 'vaadin-checkbox-group':
        // TODO: support multiple checkboxes
        return field._checkboxes[0];
      /* c8 ignore next */
      case 'vaadin-radio-group':
        // TODO: support multiple radios
        return field._radioButtons[0];
      /* c8 ignore next */
      case 'vaadin-date-time-picker':
        return field.$.customField.inputs[0].focusElement;
      default:
        return field;
    }
  }

  static getHighlightTarget(element) {
    switch (element.tagName.toLowerCase()) {
      /* c8 ignore next */
      case 'vaadin-text-area':
      case 'vaadin-text-field':
      case 'vaadin-password-field':
      case 'vaadin-email-field':
      case 'vaadin-number-field':
      case 'vaadin-integer-field':
      case 'vaadin-select-text-field':
      case 'vaadin-date-picker-text-field':
      case 'vaadin-time-picker-text-field':
      case 'vaadin-date-time-picker-date-text-field':
        return element.shadowRoot.querySelector('[part="input-field"]');
      /* c8 ignore next */
      case 'vaadin-checkbox':
        return element.shadowRoot.querySelector('[part="checkbox"]');
      /* c8 ignore next */
      case 'vaadin-radio-button':
        return element.shadowRoot.querySelector('[part="radio"]');
      default:
        return element.shadowRoot;
    }
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
          display: block;
          box-sizing: border-box;
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          width: 100%;
          height: 100%;
          z-index: -1;
          user-select: none;
          opacity: 0;
          --_active-user-color: transparent;
        }

        :host([has-active-user]) {
          opacity: 1;
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

    this.setAttribute('part', 'highlight');
    this._tags = document.createElement('vaadin-user-tags');
    this._target.appendChild(this._tags);
    this._tags.target = this._highlightRoot;
    this._addListeners();
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

  _addListeners() {
    const field = this._field;

    field.addEventListener('mouseenter', (event) => {
      // ignore mouseleave on overlay opening
      if (event.relatedTarget === this._tags.$.overlay) {
        return;
      }
      this._mouse = true;
      this._tags.opened = true;
    });

    field.addEventListener('mouseleave', (event) => {
      // ignore mouseleave on overlay opening
      if (event.relatedTarget === this._tags.$.overlay) {
        return;
      }
      this._mouse = false;
      if (!this._hasFocus) {
        this._tags.opened = false;
      }
    });

    field.addEventListener('vaadin-highlight-show', (event) => {
      this._hasFocus = true;
      this._tags.opened = true;
    });

    field.addEventListener('vaadin-highlight-hide', (event) => {
      this._hasFocus = false;
      if (!this._mouse) {
        this._tags.opened = false;
      }
    });

    this._tags.$.overlay.addEventListener('mouseleave', (event) => {
      // ignore mouseleave when moving back to field
      if (event.relatedTarget === field) {
        return;
      }
      this._mouse = false;
      if (!field.hasAttribute('focused')) {
        this._tags.opened = false;
      }
    });
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
