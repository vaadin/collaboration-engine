/**
 * This file has been autogenerated as it didn't exist or was made for an older incompatible version.
 * This file can be used for manual configuration will not be modified if the flowDefaults constant exists.
 */
const path = require('path');
const merge = require('webpack-merge');
const flowDefaults = require('./webpack.generated.js');

module.exports = merge(flowDefaults, {
  resolve: {
    alias: {
      '@polymer/polymer/polymer-element.js': path.resolve(__dirname, './node_modules/@polymer/polymer/polymer-element.js'),
      '@polymer/polymer/lib/elements/dom-repeat.js': path.resolve(__dirname, './node_modules/@polymer/polymer/lib/elements/dom-repeat.js'),
      '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js': path.resolve(__dirname, './node_modules/@vaadin/vaadin-themable-mixin/vaadin-themable-mixin')
    }
  },
});

/**
 * This file can be used to configure the flow plugin defaults.
 * <code>
 *   // Add a custom plugin
 *   flowDefaults.plugins.push(new MyPlugin());
 *
 *   // Update the rules to also transpile `.mjs` files
 *   if (!flowDefaults.module.rules[0].test) {
 *     throw "Unexpected structure in generated webpack config";
 *   }
 *   flowDefaults.module.rules[0].test = /\.m?js$/
 *
 *   // Include a custom JS in the entry point in addition to generated-flow-imports.js
 *   if (typeof flowDefaults.entry.index != "string") {
 *     throw "Unexpected structure in generated webpack config";
 *   }
 *   flowDefaults.entry.index = [flowDefaults.entry.index, "myCustomFile.js"];
 * </code>
 * or add new configuration in the merge block.
 * <code>
 *   module.exports = merge(flowDefaults, {
 *     mode: 'development',
 *     devtool: 'inline-source-map'
 *   });
 * </code>
 */
