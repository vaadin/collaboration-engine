import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles.js';

registerStyles(
  'vaadin-field-highlighter',
  css`
    :host {
      --material-field-highlight-offset: 0.5em;
      top: calc(var(--material-field-highlight-offset) * -1);
      left: calc(var(--material-field-highlight-offset) * -1);
      width: calc(100% + var(--material-field-highlight-offset) * 2);
      height: calc(100% + var(--material-field-highlight-offset) * 2);
    }

    :host::before {
      border-radius: 0.5em;
    }
  `,
  { moduleId: 'material-field-highlighter' }
);
