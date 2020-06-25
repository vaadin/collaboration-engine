module.exports = {
  browsers: {
    chrome: {
      baseUrl: 'http://localhost:8080/test/visual/',
      screenshotsDir: () => 'test/visual/screens/chrome',
      desiredCapabilities: {
        browserName: 'chrome',
        version: 'latest',
        platform: 'Windows 10'
      }
    }
  },
  plugins: {
    'hermione-esm': {
      port: 8080
    },
    'hermione-sauce': {
      verbose: false
    }
  }
};
