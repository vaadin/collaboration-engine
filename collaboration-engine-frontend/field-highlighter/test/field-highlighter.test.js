import { expect } from '@bundled-es-modules/chai';
import sinon from 'sinon';
import { fixture, html } from '@open-wc/testing-helpers';
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

    it('should create stacking context on the field', () => {
      expect(getComputedStyle(field).transform).to.equal('matrix(1, 0, 0, 1, 0, 0)');
    });

    it('should position the highlighter based on the field', () => {
      const { position, top, left } = getComputedStyle(highlighter);
      expect(position).to.equal('absolute');
      expect(top).to.equal('-8px');
      expect(left).to.equal('-8px');
    });

    it('should not show highlighter by default', () => {
      expect(getComputedStyle(highlighter).display).to.equal('none');
    });

    it('should set z-index on the highlighter to -1', () => {
      expect(getComputedStyle(highlighter).zIndex).to.equal('-1');
    });
  });

  describe('users', () => {
    const user1 = { name: 'foo', index: 0 };
    const user2 = { name: 'var', index: 1 };

    const addUser = (user) => {
      FieldHighlighter.addUser(field, user);
    };

    const removeUser = (user) => {
      FieldHighlighter.removeUser(field, user);
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

      it('should not add the same user twice', () => {
        addUser(user1);
        addUser(user1);
        expect(highlighter.users).to.deep.equal([user1]);
      });

      it('should not add user if empty value is passed', () => {
        addUser(user1);
        addUser(null);
        expect(highlighter.users).to.deep.equal([user1]);
      });

      it('should remove user if no value is passed', () => {
        addUser(user1);
        removeUser();
        expect(highlighter.users).to.deep.equal([]);
      });
    });

    describe('active user', () => {
      it('should set active user on the highlighter', () => {
        addUser(user1);
        expect(highlighter.user).to.deep.equal(user1);
      });

      it('should set attribute when user is added', () => {
        addUser(user1);
        expect(highlighter.hasAttribute('has-active-user')).to.be.true;
      });

      it('should show highlighter when user is added', () => {
        addUser(user1);
        expect(getComputedStyle(highlighter).display).to.equal('block');
      });

      it('should set width and height on the highlighter', () => {
        addUser(user1);
        const { width, height } = getComputedStyle(highlighter);
        const rect = field.getBoundingClientRect(field);
        expect(parseInt(width)).to.equal(parseInt(rect.width) + 16);
        expect(parseInt(height)).to.equal(parseInt(rect.height) + 16);
      });

      it('should remove attribute when user is removed', () => {
        addUser(user1);
        removeUser(user1);
        expect(highlighter.hasAttribute('has-active-user')).to.be.false;
      });

      it('should remove active user if no user is passed', () => {
        addUser(user1);
        removeUser();
        expect(highlighter.user).to.equal(null);
      });

      it('should set border color based on user index', () => {
        addUser(user1);
        field.style.setProperty('--user-color-0', 'red');
        expect(getComputedStyle(highlighter, '::before').borderColor).to.equal('rgb(255, 0, 0)');
      });

      it('should change border color when user changes', () => {
        addUser(user1);
        addUser(user2);
        field.style.setProperty('--user-color-1', 'blue');
        expect(getComputedStyle(highlighter, '::before').borderColor).to.equal('rgb(0, 0, 255)');
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
    });

    describe('user tags', () => {
      let wrapper;
      let tags;

      beforeEach(async () => {
        wrapper = field.shadowRoot.querySelector('vaadin-user-tags');
      });

      it('should create user tags for each added user', () => {
        addUser(user1);
        addUser(user2);
        tags = wrapper.shadowRoot.querySelectorAll('[part="tag"]');
        expect(tags.length).to.equal(2);
      });

      it('should remove user tag when user is removed', () => {
        addUser(user1);
        addUser(user2);
        removeUser(user2);
        tags = wrapper.shadowRoot.querySelectorAll('[part="tag"]');
        expect(tags.length).to.equal(1);
      });

      it('should set tag background color based on user index', () => {
        addUser(user1);
        addUser(user2);
        tags = wrapper.shadowRoot.querySelectorAll('[part="tag"]');
        field.style.setProperty('--user-color-0', 'red');
        field.style.setProperty('--user-color-1', 'blue');
        expect(getComputedStyle(tags[0]).backgroundColor).to.equal('rgb(255, 0, 0)');
        expect(getComputedStyle(tags[1]).backgroundColor).to.equal('rgb(0, 0, 255)');
      });

      it('should not set custom property if index is NaN', () => {
        addUser({ name: 'xyz', index: null });
        expect(getComputedStyle(highlighter).getPropertyValue('--vaadin-user-tag-color')).to.equal('');
      });

      it('should dispatch event on tag mousedown', () => {
        addUser(user1);
        const tag = wrapper.shadowRoot.querySelector('[part="tag"]');
        const spy = sinon.spy();
        tag.addEventListener('user-tag-click', spy);
        tag.dispatchEvent(new Event('mousedown'));
        expect(spy.callCount).to.equal(1);
      });
    });
  });
});
