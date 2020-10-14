module.exports = {
  nodeResolve: true,
  coverage: true,
  coverageConfig: {
    include: ['**/src/**/*'],
    threshold: {
      statements: 99,
      branches: 88,
      functions: 98,
      lines: 99
    }
  },
  testFramework: {
    config: {
      timeout: '3000',
    },
  },
};
