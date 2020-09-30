describe('field highlight', () => {
  const locator = '#test[data-ready]';

  ['lumo', 'material'].forEach(theme => {
    it(`${theme}-checkbox`, function() {
      return this.browser
        .url(`checkbox.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-checkbox`, locator);
    });

    it(`${theme}-checkbox-group`, function() {
      return this.browser
        .url(`checkbox-group.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-checkbox-group`, locator);
    });

    it(`${theme}-date-time-picker`, function() {
      return this.browser
        .url(`date-time-picker.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-date-time-picker`, locator);
    });

    it(`${theme}-list-box`, function() {
      return this.browser
        .url(`list-box.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-list-box`, locator);
    });

    it(`${theme}-radio-button`, function() {
      return this.browser
        .url(`radio-button.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-radio-button`, locator);
    });

    it(`${theme}-radio-group`, function() {
      return this.browser
        .url(`radio-group.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-radio-group`, locator);
    });

    it(`${theme}-text-field`, function() {
      return this.browser
        .url(`text-field.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-text-field`, locator);
    });

    it(`${theme}-rtl`, function() {
      return this.browser
        .url(`rtl.html?theme=${theme}`)
        .waitForVisible(locator, 15000)
        .assertView(`${theme}-rtl`, locator);
    });
  });
});
