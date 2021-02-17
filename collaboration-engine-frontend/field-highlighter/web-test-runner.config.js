const { chromeLauncher } = require('@web/test-runner');

module.exports = {
  nodeResolve: true,
  browsers: [chromeLauncher({ launchOptions: { args: ['--no-sandbox'] } })],
  coverage: true,
  coverageConfig: {
    include: ['**/src/**/*'],
    threshold: {
      statements: 99,
      branches: 87,
      functions: 98,
      lines: 99
    }
  },
  testFramework: {
    config: {
      timeout: '3000'
    }
  }
};
