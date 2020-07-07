import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import './vaadin-user-tags.js';

const fields = new WeakMap();

export class FieldHighlighter extends ThemableMixin(PolymerElement) {
  static init(field) {
    if (!fields.has(field)) {
      // Create instance
      const instance = document.createElement(this.is);
      instance._field = field;

      // Set attribute for styling
      field.setAttribute('has-highlighter', '');

      // TODO: Not nice, have to create a stacking context
      const style = document.createElement('style');
      style.textContent = `
        :host([has-highlighter]) {
          transform: translateZ(0);
        }
      `;
      field.shadowRoot.appendChild(style);

      // Store instance
      fields.set(field, instance);

      // Attach instance
      field.shadowRoot.appendChild(instance);
    }

    return fields.get(field);
  }

  static addUser(field, user) {
    this.init(field).addUser(user);
  }

  static removeUser(field, user) {
    this.init(field).removeUser(user);
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

    this._tags = this._createTags();
    this._field.shadowRoot.appendChild(this._tags);
  }

  _createTags() {
    const tags = document.createElement('vaadin-user-tags');
    tags.users = this.users;
    return tags;
  }

  addUser(user) {
    if (user) {
      this.push('users', user);
      if (this._tags) {
        this._tags.setUsers(this.users);
      }

      // Make user active
      this.user = user;
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
        if (this._tags) {
          this._tags.setUsers(this.users);
        }

        // Change or remove active user
        if (this.users.length > 0) {
          this.user = this.users[this.users.length - 1];
        } else {
          this.user = null;
        }
      }
    }
  }

  _userChanged(user) {
    if (user) {
      this.setAttribute('has-active-user', '');
      this.style.setProperty('--_active-user-color', `var(--user-color-${user.colorIndex})`);
    } else {
      this.style.removeProperty('--_active-user-color');
      this.removeAttribute('has-active-user');
    }
  }
}

customElements.define(FieldHighlighter.is, FieldHighlighter);
