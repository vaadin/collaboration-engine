import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import '@vaadin/vaadin-lumo-styles/color.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/typography.js';

registerStyles(
  'vaadin-user-tags',
  css`
    :host {
      --lumo-user-tags-offset: calc((var(--lumo-space-s) - 2px) * -1);
      top: var(--lumo-user-tags-offset);
      right: var(--lumo-user-tags-offset);
      border: solid var(--lumo-space-xs) transparent;
    }

    :host([dir="rtl"]) {
      right: auto;
      left: var(--lumo-user-tags-offset);
    }

    :host(:not(:hover)) [part='tag'] {
      max-width: 0.5em;
      max-height: 0.5em;
      box-shadow: none;
    }
  `,
  { moduleId: 'lumo-user-tags' }
);

// TODO: Lumo colors
registerStyles(
  'vaadin-user-tag',
  css`
    :host {
      font-family: var(--lumo-font-family);
      font-size: var(--lumo-font-size-xxs);
      border-radius: var(--lumo-border-radius-s);
      box-shadow: var(--lumo-box-shadow-s);
      max-height: calc(1rem + 0.6em);
      transition: max-width 0.3s, max-height 0.3s;
    }

    [part='name'] {
      color: var(--lumo-primary-contrast-color);
      padding: 0.3em calc(0.3em + var(--lumo-border-radius-s) / 4);
      line-height: 1;
      font-weight: 500;
      min-width: calc(var(--lumo-line-height-xs) * 1em + 0.45em);
    }
  `,
  { moduleId: 'lumo-user-tag' }
);
