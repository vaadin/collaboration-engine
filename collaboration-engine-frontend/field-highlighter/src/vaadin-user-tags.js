import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { timeOut } from '@polymer/polymer/lib/utils/async.js';
import { calculateSplices } from '@polymer/polymer/lib/utils/array-splice.js';
import { DirMixin } from '@vaadin/vaadin-element-mixin/vaadin-dir-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { registerStyles, css } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import { OverlayElement } from '@vaadin/vaadin-overlay/vaadin-overlay.js';
import './vaadin-user-tag.js';

const DURATION = 200;
const DELAY = 200;

const listenOnce = (elem, type) => {
  return new Promise((resolve) => {
    const listener = () => {
      elem.removeEventListener(type, listener);
      resolve();
    };
    elem.addEventListener(type, listener);
  });
};

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

    :host([opening]),
    :host([closing]) {
      animation: 0.14s user-tags-overlay-dummy-animation;
    }

    @keyframes user-tags-overlay-dummy-animation {
      0% {
        opacity: 1;
      }
      100% {
        opacity: 1;
      }
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
      flashing: {
        type: Boolean,
        value: false
      },
      target: {
        type: Object
      },
      users: {
        type: Array,
        value: () => []
      },
      _flashQueue: {
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

  get wrapper() {
    return this.$.overlay.content.querySelector('[part="tags"]');
  }

  createUserTag(user) {
    const tag = document.createElement('vaadin-user-tag');
    tag.setAttribute('id', `tag-${user.id}`);
    tag.name = user.name;
    tag.colorIndex = user.colorIndex;
    return tag;
  }

  getTagForUser(user) {
    return this.wrapper.querySelector(`#tag-${user.id}`);
  }

  getChangedTags(addedUsers, removedUsers) {
    const removed = removedUsers.map((user) => this.getTagForUser(user));
    const added = addedUsers.map((user) => this.getTagForUser(user) || this.createUserTag(user));
    return { added, removed };
  }

  getChangedUsers(users, splices) {
    const usersToAdd = [];
    const usersToRemove = [];

    splices.forEach((splice) => {
      for (let i = 0; i < splice.removed.length; i++) {
        usersToRemove.push(splice.removed[i]);
      }

      for (let i = splice.addedCount - 1; i >= 0; i--) {
        usersToAdd.push(users[splice.index + i]);
      }
    });

    // filter out users that are only moved
    const addedUsers = usersToAdd.filter((u) => !usersToRemove.some((u2) => u.id === u2.id));
    const removedUsers = usersToRemove.filter((u) => !usersToAdd.some((u2) => u.id === u2.id));

    return { addedUsers, removedUsers };
  }

  applyTagsStart({ added, removed }) {
    const wrapper = this.wrapper;
    removed.forEach((tag) => tag && tag.classList.remove('show'));
    added.forEach((tag) => wrapper.insertBefore(tag, wrapper.firstChild));
  }

  applyTagsEnd({ added, removed }) {
    const wrapper = this.wrapper;
    removed.forEach((tag) => {
      if (tag && tag.parentNode === wrapper) {
        wrapper.removeChild(tag);
      }
    });
    added.forEach((tag) => tag && tag.classList.add('show'));
  }

  setUsers(users) {
    // Apply pending change if needed
    this.render();

    const splices = calculateSplices(users, this.users);
    if (splices.length === 0) {
      return;
    }

    const { addedUsers, removedUsers } = this.getChangedUsers(users, splices);
    if (addedUsers.length === 0 && removedUsers.length === 0) {
      return;
    }

    const changedTags = this.getChangedTags(addedUsers, removedUsers);

    if (this.opened && !this.flashing) {
      this.updateTags(users, changedTags);
      return;
    } else {
      this.updateTagsSync(users, changedTags);

      if (addedUsers.length) {
        const tags = addedUsers.map((user) => this.createUserTag(user));
        if (this.flashing) {
          // schedule next flash later
          this.push('_flashQueue', tags);
        } else {
          this.flashTags(tags);
        }
      }
    }
  }

  flashTags(added) {
    this.flashing = true;
    const wrapper = this.wrapper;

    // hide existing tags
    wrapper.style.display = 'none';

    // render clones of new tags
    const newWrapper = wrapper.nextElementSibling;
    added.forEach((tag) => newWrapper.insertBefore(tag, newWrapper.firstChild));

    this.flashPromise = new Promise((resolve) => {
      listenOnce(this.$.overlay, 'vaadin-overlay-open').then(() => {
        // animate appearing tags
        added.forEach((tag) => tag.classList.add('show'));

        this._debounceFlashStart = Debouncer.debounce(this._debounceFlashStart, timeOut.after(DURATION + DELAY), () => {
          // animate disappearing
          added.forEach((tag) => tag.classList.remove('show'));

          this._debounceFlashEnd = Debouncer.debounce(this._debounceFlashEnd, timeOut.after(DURATION), () => {
            // wait for overlay closing animation to complete
            listenOnce(this.$.overlay, 'animationend').then(() => {
              added.forEach((tag) => tag.parentNode.removeChild(tag));
              // show all tags
              wrapper.style.display = 'flex';
              this.flashing = false;
              resolve();
            });

            this.opened = false;
          });
        });
      });
    }).then(() => {
      if (this._flashQueue.length > 0) {
        const tags = this._flashQueue[0];
        this.splice('_flashQueue', 0, 1);
        this.flashTags(tags);
      }
    });

    this.opened = true;
  }

  updateTags(users, changed) {
    this.applyTagsStart(changed);

    this._debounceRender = Debouncer.debounce(this._debounceRender, timeOut.after(DURATION), () => {
      this.set('users', users);

      this.applyTagsEnd(changed);

      if (users.length === 0 && this.opened) {
        this.opened = false;
      }
    });
  }

  updateTagsSync(users, changed) {
    this.applyTagsStart(changed);
    this.set('users', users);
    this.applyTagsEnd(changed);
  }

  render() {
    /* c8 ignore next */
    if (this._debounceRender && this._debounceRender.isActive()) {
      this._debounceRender.flush();
    }
  }
}

customElements.define(UserTags.is, UserTags);
