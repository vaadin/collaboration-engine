import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/custom-style.js';

const userColorsStyles = html`
  <custom-style>
    <style>
      html {
        --vaadin-user-color-0: #b300d0;
        --vaadin-user-color-1: #5c62f1;
        --vaadin-user-color-2: #00ae8f;
        --vaadin-user-color-3: #b99000;
        --vaadin-user-color-4: #0043f0;
        --vaadin-user-color-5: #ff15be;
        --vaadin-user-color-6: #ff5f04;
        --vaadin-user-color-7: #74bb2c;
        --vaadin-user-color-8: #00a2d5;
        --vaadin-user-color-9: #434348;
      }
    </style>
  </custom-style>
`;

document.head.appendChild(userColorsStyles.content);
