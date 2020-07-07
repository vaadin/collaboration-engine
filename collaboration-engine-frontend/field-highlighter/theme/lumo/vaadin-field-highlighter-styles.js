import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';

registerStyles(
  'vaadin-field-highlighter',
  css`
    :host {
      display: block;
      --lumo-field-highlight-offset: var(--lumo-space-s);
      top: calc(var(--lumo-field-highlight-offset) * -1);
      left: calc(var(--lumo-field-highlight-offset) * -1);
      width: calc(100% + var(--lumo-field-highlight-offset) * 2);
      height: calc(100% + var(--lumo-field-highlight-offset) * 2);
      opacity: 0;
      transition: opacity 0.3s;
      --_active-user-color: transparent;
    }

    :host::before {
      border-radius: var(--lumo-border-radius-l);
      transition: border-color 0.3s;
    }

    :host([has-active-user]) {
      opacity: 1;
    }
  `,
  { moduleId: 'lumo-field-highlighter' }
);
