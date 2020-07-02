import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

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
          max-width: 100%;
          margin-bottom: var(--vaadin-user-tag-offset);
          background-color: var(--vaadin-user-tag-color);
          color: #fff;
          width: var(--vaadin-user-tag-size);
          height: var(--vaadin-user-tag-size);
          overflow: hidden;
          border-radius: 4px;
          cursor: default;
          --vaadin-user-tag-size: 8px;
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
          visibility: var(--vaadin-user-tag-visibility, hidden);
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
      index: {
        type: Number,
        observer: '_indexChanged'
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

  _indexChanged(index) {
    if (index != null) {
      this.style.setProperty('--vaadin-user-tag-color', `var(--user-color-${index})`);
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
