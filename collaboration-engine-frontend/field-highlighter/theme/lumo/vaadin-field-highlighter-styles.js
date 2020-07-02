import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';

registerStyles(
  'vaadin-field-highlighter',
  css`
    :host {
      --lumo-field-highlight-offset: var(--lumo-space-s);
      top: calc(var(--lumo-field-highlight-offset) * -1);
      left: calc(var(--lumo-field-highlight-offset) * -1);
      width: calc(100% + var(--lumo-field-highlight-offset) * 2);
      height: calc(100% + var(--lumo-field-highlight-offset) * 2);
    }

    :host::before {
      border-radius: var(--lumo-border-radius-l);
    }
  `,
  { moduleId: 'lumo-field-highlighter' }
);
