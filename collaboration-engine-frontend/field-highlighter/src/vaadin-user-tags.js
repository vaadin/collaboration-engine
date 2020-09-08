import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { timeOut } from '@polymer/polymer/lib/utils/async.js';
import { calculateSplices } from '@polymer/polymer/lib/utils/array-splice.js';
import { DirMixin } from '@vaadin/vaadin-element-mixin/vaadin-dir-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { registerStyles, css } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import { OverlayElement } from '@vaadin/vaadin-overlay/vaadin-overlay.js';
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
      overflow: visible;
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
          <div part="tags"></div>
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
    this._debouncePosition = Debouncer.debounce(this._debouncePosition, timeOut.after(16), () => this._setPosition());
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
    // Apply pending change if needed
    this.render();

    const splices = calculateSplices(users, this.users);
    if (splices.length === 0) {
      return;
    }

    const wrapper = this.$.overlay.content.querySelector('[part="tags"]');

    const removed = [];
    const added = [];

    splices.forEach((splice) => {
      for (let i = 0; i < splice.removed.length; i++) {
        const user = splice.removed[i];
        const tag = wrapper.querySelector(`#tag-${user.id}`);
        removed.push(tag);
      }

      for (let i = splice.addedCount - 1; i >= 0; i--) {
        const user = users[splice.index + i];
        // Check if the tag is moved and can be reused
        let tag = wrapper.querySelector(`#tag-${user.id}`);
        if (!tag) {
          tag = document.createElement('vaadin-user-tag');
          tag.setAttribute('id', `tag-${user.id}`);
          tag.name = user.name;
          tag.colorIndex = user.colorIndex;
        }
        added.push(tag);
      }
    });

    // Filter out reused tags instances
    const tags = wrapper.querySelectorAll('vaadin-user-tag');
    tags.forEach((tag) => {
      const a = added.indexOf(tag);
      const r = removed.indexOf(tag);

      if (a !== -1 && r !== -1) {
        removed.splice(r, 1);
        added.splice(a, 1);
      }
    });

    removed.forEach((tag) => tag.classList.remove('show'));

    added.forEach((tag) => wrapper.insertBefore(tag, wrapper.firstChild));

    this._debounceRender = Debouncer.debounce(this._debounceRender, timeOut.after(200), () => {
      this.set('users', users);

      removed.forEach((tag) => wrapper.removeChild(tag));

      added.forEach((tag) => tag.classList.add('show'));

      if (users.length === 0 && this.opened) {
        this.opened = false;
      }
    });
  }

  render() {
    if (this._debounceRender && this._debounceRender.isActive()) {
      this._debounceRender.flush();
    }
  }
}

customElements.define(UserTags.is, UserTags);
