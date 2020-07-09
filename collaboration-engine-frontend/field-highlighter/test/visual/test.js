describe('field highlight', () => {
  const locator = '#test[data-ready]';

  ['lumo', 'material'].forEach(theme => {
    it(`${theme}-checkbox`, function() {
      return this.browser
        .url(`checkbox.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-checkbox`, locator);
    });

    it(`${theme}-checkbox-group`, function() {
      return this.browser
        .url(`checkbox-group.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-checkbox-group`, locator);
    });

    it(`${theme}-radio-group`, function() {
      return this.browser
        .url(`radio-group.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-radio-group`, locator);
    });

    it(`${theme}-text-field`, function() {
      return this.browser
        .url(`text-field.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-text-field`, locator);
    });

    it(`${theme}-overflow`, function() {
      return this.browser
        .url(`overflow.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-overflow`, locator);
    });

    it(`${theme}-user-tags`, function() {
      return this.browser
        .url(`user-tags.html?theme=${theme}`)
        .waitForVisible(locator, 10000)
        .assertView(`${theme}-user-tags`, locator);
    });
  });
});
