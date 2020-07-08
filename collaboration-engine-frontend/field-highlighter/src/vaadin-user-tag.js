import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { setCustomProperty } from './css-helpers.js';
import './vaadin-user-colors.js';

export class UserTag extends ThemableMixin(PolymerElement) {
  static get is() {
    return 'vaadin-user-tag';
  }

  static get template() {
    return html`
      <style>
        :host {
          display: block;
          position: relative;
          box-sizing: border-box;
          margin-bottom: var(--vaadin-user-tag-offset);
          background-color: var(--vaadin-user-tag-color);
          color: #fff;
          max-width: 6px;
          max-height: 6px;
          overflow: hidden;
          border-radius: 3px;
          cursor: default;
          transition: max-width 0.3s, max-height 0.3s;
          --vaadin-user-tag-offset: 4px;
        }

        :host(:last-of-type) {
          margin-bottom: 0;
        }

        [part='name'] {
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
          box-sizing: border-box;
          padding: 2px 4px;
          font-size: 13px;
        }
      </style>
      <!-- TODO: image / avatar -->
      <div part="name">[[name]]</div>
    `;
  }

  static get properties() {
    return {
      name: {
        type: String
      },
      colorIndex: {
        type: Number,
        observer: '_colorIndexChanged'
      }
    };
  }

  ready() {
    super.ready();

    // Capture mousedown to prevent click on the underlying label,
    // which would result in undesirable focusing field components.
    // TODO: consider handling touchstart event in a similar way
    this.addEventListener('mousedown', this._onClick.bind(this), true);
  }

  _colorIndexChanged(index) {
    if (index != null) {
      setCustomProperty(this, '--vaadin-user-tag-color', `var(--vaadin-user-color-${index})`);
    }
  }

  _onClick(e) {
    e.preventDefault();
    this.dispatchEvent(
      new CustomEvent('user-tag-click', {
        bubbles: true,
        composed: true,
        detail: {
          name: this.name
        }
      })
    );
  }
}

customElements.define(UserTag.is, UserTag);
