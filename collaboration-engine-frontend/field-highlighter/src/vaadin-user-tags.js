import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { DirMixin } from '@vaadin/vaadin-element-mixin/vaadin-dir-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import './vaadin-user-tag.js';

export class UserTags extends ThemableMixin(DirMixin(PolymerElement)) {
  static get is() {
    return 'vaadin-user-tags';
  }

  static get template() {
    return html`
      <style>
        :host {
          position: absolute;
          z-index: 1;
          top: -6px;
          right: -6px;
          max-width: 100%;
          border: solid 4px transparent;
          display: flex;
          flex-direction: column;
          align-items: flex-end;
        }

        :host([dir="rtl"]) {
          right: auto;
          left: -6px;
        }

        [part='tag'] {
          flex-shrink: 0;
        }

        :host(:not(:hover)) {
          max-height: calc(100% + 4px);
          overflow: hidden;
        }

        :host([hidden]) {
          display: none !important;
        }

        :host(:hover) [part='tag'] {
          max-width: 100%;
          max-height: calc(1rem + 4px);
        }
      </style>
      <template id="tags" is="dom-repeat" items="[[users]]">
        <vaadin-user-tag name="[[item.name]]" color-index="[[item.colorIndex]]" part="tag"></vaadin-user-tag>
      </template>
    `;
  }

  static get properties() {
    return {
      users: {
        type: Array
      }
    };
  }

  setUsers(users) {
    this.set('users', users);
    this.$.tags.render();
  }
}

customElements.define(UserTags.is, UserTags);
