import { expect } from '@bundled-es-modules/chai';
import sinon from 'sinon';
import { fixture, html, nextFrame } from '@open-wc/testing-helpers';
import '@vaadin/vaadin-text-field/vaadin-text-field.js';
import { FieldHighlighter } from '../src/vaadin-field-highlighter.js';

describe('field highlighter', () => {
  let field;
  let highlighter;

  beforeEach(async () => {
    field = await fixture(html`<vaadin-text-field></vaadin-text-field>`);
    highlighter = FieldHighlighter.init(field);
  });

  describe('initialization', () => {
    it('should create field highlighter instance', () => {
      expect(highlighter).to.be.ok;
    });

    it('should attach field highlighter instance to field', () => {
      expect(highlighter.getRootNode().host).to.equal(field);
    });

    it('should set has-highlighter attribute on the field', () => {
      expect(field.hasAttribute('has-highlighter')).to.be.true;
    });

    it('should position the highlighter based on the field', () => {
      const { position, top, left, right, bottom } = getComputedStyle(highlighter);
      expect(position).to.equal('absolute');
      expect(top).to.equal('0px');
      expect(left).to.equal('0px');
      expect(right).to.equal('0px');
      expect(bottom).to.equal('0px');
    });

    it('should not show highlighter by default', () => {
      expect(getComputedStyle(highlighter).opacity).to.equal('0');
    });

    it('should set z-index on the highlighter to -1', () => {
      expect(getComputedStyle(highlighter).zIndex).to.equal('-1');
    });
  });

  describe('users', () => {
    const user1 = { id: 'a', name: 'foo', colorIndex: 0 };
    const user2 = { id: 'b', name: 'var', colorIndex: 1 };

    const addUser = (user) => {
      FieldHighlighter.addUser(field, user);
    };

    const removeUser = (user) => {
      FieldHighlighter.removeUser(field, user);
    };

    const setUsers = (users) => {
      FieldHighlighter.setUsers(field, users);
    };

    describe('adding and removing', () => {
      it('should add users to the highlighter', () => {
        addUser(user1);
        expect(highlighter.users).to.deep.equal([user1]);

        addUser(user2);
        expect(highlighter.users).to.deep.equal([user1, user2]);
      });

      it('should remove users from the highlighter', () => {
        addUser(user1);
        removeUser(user1);
        expect(highlighter.users).to.deep.equal([]);
      });

      it('should add multiple users at a time', () => {
        setUsers([user1, user2]);
        expect(highlighter.users).to.deep.equal([user1, user2]);
      });

      it('should remove users if empty array is passed', () => {
        setUsers([user1, user2]);
        setUsers([]);
        expect(highlighter.users).to.deep.equal([]);
      });

      it('should not add user if empty value is passed', () => {
        addUser(user1);
        addUser(null);
        expect(highlighter.users).to.deep.equal([user1]);
      });

      it('should not remove user if no value is passed', () => {
        addUser(user1);
        removeUser();
        expect(highlighter.users).to.deep.equal([user1]);
      });
    });

    describe('active user', () => {
      it('should set active user on the highlighter', () => {
        addUser(user1);
        expect(highlighter.user).to.deep.equal(user1);
      });

      it('should set last added user as active', () => {
        setUsers([user1, user2]);
        expect(highlighter.user).to.deep.equal(user2);
      });

      it('should set attribute when user is added', () => {
        addUser(user1);
        expect(highlighter.hasAttribute('has-active-user')).to.be.true;
      });

      it('should show highlighter when user is added', async () => {
        addUser(user1);
        await nextFrame();
        expect(getComputedStyle(highlighter).opacity).to.equal('1');
      });

      it('should remove attribute when user is removed', () => {
        addUser(user1);
        removeUser(user1);
        expect(highlighter.hasAttribute('has-active-user')).to.be.false;
      });

      it('should make previous user active when user is removed', () => {
        addUser(user1);
        addUser(user2);
        removeUser(user2);
        expect(highlighter.user).to.deep.equal(user1);
      });

      it('should reset user when all the users are removed', () => {
        addUser(user1);
        addUser(user2);
        removeUser(user2);
        removeUser(user1);
        expect(highlighter.user).to.equal(null);
      });

      it('should reset user when multiple users are removed', () => {
        setUsers([user1, user2]);
        setUsers([]);
        expect(highlighter.user).to.equal(null);
      });
    });

    describe('overlay', () => {
      let overlay;
      let tags;

      beforeEach(() => {
        tags = field.shadowRoot.querySelector('vaadin-user-tags');
        overlay = tags.$.overlay;
      });

      afterEach(() => {
        if (overlay.opened) {
          overlay.opened = false;
        }
      });

      it('should open overlay on field focusin', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('focusin'));
        expect(overlay.opened).to.equal(true);
      });

      it('should close overlay on field focusout', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('focusin'));
        field.dispatchEvent(new CustomEvent('focusout'));
        expect(overlay.opened).to.equal(false);
      });

      it('should open overlay on field mouseenter', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        expect(overlay.opened).to.equal(true);
      });

      it('should close overlay on field mouseleave', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        field.dispatchEvent(new CustomEvent('mouseleave'));
        expect(overlay.opened).to.equal(false);
      });

      it('should not close overlay on field mouseleave after focusin', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        field.dispatchEvent(new CustomEvent('focusin'));
        field.dispatchEvent(new CustomEvent('mouseleave'));
        expect(overlay.opened).to.equal(true);
      });

      it('should not close overlay on field focusout after mouseenter', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('focusin'));
        field.dispatchEvent(new CustomEvent('mouseenter'));
        field.dispatchEvent(new CustomEvent('focusout'));
        expect(overlay.opened).to.equal(true);
      });

      it('should not close overlay on field mouseleave to overlay', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        const leave = new CustomEvent('mouseleave');
        leave.relatedTarget = overlay;
        field.dispatchEvent(leave);
        expect(overlay.opened).to.equal(true);
      });

      it('should close overlay on overlay mouseleave', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        overlay.dispatchEvent(new CustomEvent('mouseleave'));
        expect(overlay.opened).to.equal(false);
      });

      it('should not close overlay on overlay mouseleave to field', async () => {
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        const leave = new CustomEvent('mouseleave');
        leave.relatedTarget = field;
        overlay.dispatchEvent(leave);
        expect(overlay.opened).to.equal(true);
      });

      it('should set position on tags overlay when opened', async () => {
        const spy = sinon.spy(tags, 'setPosition');
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        expect(spy.callCount).to.equal(1);
      });

      it('should not re-position overlay on mouseenter from overlay', async () => {
        const spy = sinon.spy(tags, 'setPosition');
        addUser(user1);
        await nextFrame();
        field.dispatchEvent(new CustomEvent('mouseenter'));
        // Emulate second mouseenter from overlay
        const enter = new CustomEvent('mouseenter');
        enter.relatedTarget = overlay;
        field.dispatchEvent(enter);
        expect(spy.callCount).to.equal(1);
      });
    });

    describe('user tags', () => {
      let tags;

      const getTags = (field) => {
        const overlay = field.shadowRoot.querySelector('vaadin-user-tags').$.overlay;
        return overlay.content.querySelectorAll('vaadin-user-tag');
      };

      it('should create user tags for each added user', async () => {
        addUser(user1);
        addUser(user2);
        await nextFrame();
        tags = getTags(field);
        expect(tags.length).to.equal(2);
      });

      it('should remove user tag when user is removed', async () => {
        addUser(user1);
        addUser(user2);
        await nextFrame();
        removeUser(user2);
        await nextFrame();
        tags = getTags(field);
        expect(tags.length).to.equal(1);
      });

      it('should set tag background color based on user index', async () => {
        addUser(user1);
        addUser(user2);
        await nextFrame();
        tags = getTags(field);
        document.documentElement.style.setProperty('--vaadin-user-color-0', 'red');
        document.documentElement.style.setProperty('--vaadin-user-color-1', 'blue');
        expect(getComputedStyle(tags[0]).backgroundColor).to.equal('rgb(0, 0, 255)');
        expect(getComputedStyle(tags[1]).backgroundColor).to.equal('rgb(255, 0, 0)');
      });

      it('should not set custom property if index is NaN', () => {
        addUser({ name: 'xyz', colorIndex: null });
        expect(getComputedStyle(highlighter).getPropertyValue('--vaadin-user-tag-color')).to.equal('');
      });

      it('should dispatch event on tag mousedown', async () => {
        addUser(user1);
        await nextFrame();
        const tag = getTags(field)[0];
        const spy = sinon.spy();
        tag.addEventListener('user-tag-click', spy);
        tag.dispatchEvent(new Event('mousedown'));
        expect(spy.callCount).to.equal(1);
      });
    });

    describe('announcements', () => {
      // NOTE: See <iron-a11y-announcer> API

      function waitForAnnounce(callback) {
        var listener = (event) => {
          document.body.removeEventListener('iron-announce', listener);
          callback(event.detail.text);
        };
        document.body.addEventListener('iron-announce', listener);
      }

      it('should announce adding a new user', (done) => {
        waitForAnnounce((text) => {
          expect(text).to.equal(`${user1.name} started editing`);
          done();
        });

        addUser(user1);
      });

      it('should announce field label, if any', (done) => {
        waitForAnnounce((text) => {
          expect(text).to.equal(`${user1.name} started editing ${field.label}`);
          done();
        });

        field.label = 'Username';
        addUser(user1);
      });
    });
  });
});
