module.exports = {
  nodeResolve: true,
  coverage: true,
  coverageConfig: {
    include: ['**/src/**/*'],
    threshold: {
      statements: 100,
      branches: 98,
      functions: 100,
      lines: 100
    }
  },
};
