import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import './vaadin-user-tag.js';

export class UserTags extends ThemableMixin(PolymerElement) {
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
          padding: 4px;
          display: flex;
          flex-direction: column;
          align-items: flex-end;
        }

        :host([hidden]) {
          display: none !important;
        }

        :host(:hover) [part='tag'] {
          --vaadin-user-tag-size: auto;
          --vaadin-user-tag-visibility: visible;
        }
      </style>
      <template id="tags" is="dom-repeat" items="[[users]]">
        <vaadin-user-tag name="[[item.name]]" index="[[item.index]]" part="tag"></vaadin-user-tag>
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
