import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { timeOut } from '@polymer/polymer/lib/utils/async.js';
import { DirMixin } from '@vaadin/vaadin-element-mixin/vaadin-dir-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { registerStyles, css } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import { OverlayElement } from '@vaadin/vaadin-overlay/vaadin-overlay.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import './vaadin-user-tag.js';

class UserTagsOverlayElement extends OverlayElement {
  static get is() {
    return 'vaadin-user-tags-overlay';
  }

  ready() {
    super.ready();
    this.$.overlay.setAttribute('tabindex', '-1');
  }
}

customElements.define(UserTagsOverlayElement.is, UserTagsOverlayElement);

registerStyles(
  'vaadin-user-tags-overlay',
  css`
    :host {
      align-items: stretch;
      justify-content: flex-start;
      background: transparent;
      box-shadow: none;
      bottom: auto;
    }

    [part='overlay'] {
      box-shadow: none;
      background: transparent;
      position: relative;
      left: -4px;
      padding: 4px;
      outline: none;
    }

    :host([dir='rtl']) [part='overlay'] {
      left: auto;
      right: -4px;
    }

    [part='content'] {
      padding: 0;
    }

    :host([dir='rtl']) {
      left: auto;
    }

    :host(:not([dir='rtl'])) {
      right: auto;
    }
  `
);

export class UserTags extends ThemableMixin(DirMixin(PolymerElement)) {
  static get is() {
    return 'vaadin-user-tags';
  }

  static get template() {
    return html`
      <style>
        :host {
          position: absolute;
        }

        [part='tags'] {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
        }
      </style>
      <vaadin-user-tags-overlay modeless id="overlay" opened="[[opened]]">
        <template>
          <div part="tags">
            <template id="tags" is="dom-repeat" items="[[users]]">
              <vaadin-user-tag name="[[item.name]]" color-index="[[item.colorIndex]]"></vaadin-user-tag>
            </template>
          </div>
        </template>
      </vaadin-user-tags-overlay>
    `;
  }

  static get properties() {
    return {
      opened: {
        type: Boolean,
        value: false,
        observer: '_openedChanged'
      },
      target: {
        type: Object
      },
      users: {
        type: Array,
        value: () => []
      }
    };
  }

  constructor() {
    super();
    this._boundSetPosition = this._debounceSetPosition.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    window.addEventListener('resize', this._boundSetPosition);
    window.addEventListener('scroll', this._boundSetPosition);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('resize', this._boundSetPosition);
    window.removeEventListener('scroll', this._boundSetPosition);
  }

  _debounceSetPosition() {
    this._debouncer = Debouncer.debounce(this._debouncer, timeOut.after(16), () => this._setPosition());
  }

  _openedChanged(opened) {
    if (opened) {
      this._setPosition();
    }
  }

  _setPosition() {
    if (!this.opened) {
      return;
    }

    const targetRect = this.target.getBoundingClientRect();

    const overlayRect = this.$.overlay.getBoundingClientRect();

    this._translateX =
      this.getAttribute('dir') === 'rtl'
        ? targetRect.right - overlayRect.right + (this._translateX || 0)
        : targetRect.left - overlayRect.left + (this._translateX || 0);
    this._translateY = targetRect.top - overlayRect.top + (this._translateY || 0) + targetRect.height;

    const devicePixelRatio = window.devicePixelRatio || 1;
    this._translateX = Math.round(this._translateX * devicePixelRatio) / devicePixelRatio;
    this._translateY = Math.round(this._translateY * devicePixelRatio) / devicePixelRatio;

    this.$.overlay.style.transform = `translate3d(${this._translateX}px, ${this._translateY}px, 0)`;
  }

  setUsers(users) {
    this.set('users', users);
  }
}

customElements.define(UserTags.is, UserTags);
